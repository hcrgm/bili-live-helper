package net.windit.bililive;

import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class LiveRoom extends WebSocketListener {

    private final List<String> wsServers;
    private final List<PacketListener> listeners;
    private final AtomicInteger failedTimes = new AtomicInteger();
    private final int displayRoomId;
    private ScheduledExecutorService executor;
    private BlockingQueue<LivePacket> packetQueue;
    private int realRoomId;
    private volatile int popular;
    private ScheduledFuture<?> heartBeatTask;
    private WebSocket ws;
    private Future<?> packetDeliveryTask;
    private volatile boolean reconnect;
    private volatile boolean closed;
    /**
     * B站服务器鉴权用, 未指定key或key无效服务器会拒绝连接
     */
    private String key;

    public LiveRoom(int roomId) {
        this.displayRoomId = roomId;
        listeners = new LinkedList<>();
        wsServers = new ArrayList<>();
    }

    public synchronized void registerListener(PacketListener listener) {
        listeners.add(listener);
    }

    public synchronized void unregisterListener(PacketListener listener) {
        listeners.remove(listener);
    }

    private synchronized void notifyListeners(LivePacket packet) {
        listeners.forEach(listener -> listener.onPacket(packet, this));
    }

    public void setReconnect(boolean reconnect) {
        this.reconnect = reconnect;
    }

    public void connect() throws LiveException {
        int realRoomId = API.getRealRoomId(this.displayRoomId);
        if (realRoomId == -1) {
            throw new LiveException("房间号不存在或获取真实房间号时发生错误.");
        }
        this.realRoomId = realRoomId;
        API.DanmuConf conf = API.getDanmuConf(realRoomId);
        if (conf == null) {
            throw new LiveException("获取弹幕服务器出错");
        }
        for (Map<String, ?> map : conf.serverList) {
            String host = (String) map.get("host");
            int port = ((Double) map.get("wss_port")).intValue();
            wsServers.add("wss://" + host + ":" + port + "/sub");
        }
        this.key = conf.token;
        registerListener(new InternalListener());
        tryServers();
    }

    private void tryServers() {
        Request request = new Request.Builder().url(wsServers.get(failedTimes.get()))
                .removeHeader("User-Agent")
                .addHeader("User-Agent", API.USER_AGENT)
                .build();
        API.client.newWebSocket(request, this);
    }

    public int getPopular() {
        return popular;
    }

    public int getRoomId() {
        return displayRoomId;
    }

    @Override
    public void onOpen(WebSocket webSocket, Response response) {
        API.debug("已开启");
        this.ws = webSocket;
        LivePacket.EnterRoom enterRoomRequest = new LivePacket.EnterRoom();
        enterRoomRequest.roomid = realRoomId;
        enterRoomRequest.key = key;
        LivePacket enterRoomPacket = new LivePacket(LivePacket.PROTOCOL_VERSION_HEARTBEAT, LivePacket.OPERATION_ENTER_ROOM, LivePacket.SEQUENCE_ID,
                API.gson.toJson(enterRoomRequest).getBytes(StandardCharsets.UTF_8));
        if (this.sendPacket(enterRoomPacket)) {
            API.debug("发送成功");
            packetQueue = new LinkedBlockingQueue<>();
            executor = new ScheduledThreadPoolExecutor(2, API.threadFactory(String.format("LiveRoom %d", realRoomId)));
            packetDeliveryTask = executor.submit(new PacketDeliveryTask());
        } else {
            API.debug("发送失败");
            this.close();
        }
    }

    public boolean sendPacket(LivePacket packet) {
        return this.ws.send(packet.encode());
    }

    @Override
    public void onMessage(WebSocket webSocket, ByteString bytes) {
        LivePacket packet = LivePacket.decode(bytes);
        packetQueue.add(packet);
    }

    public void close() {
        setReconnect(false);
        this.ws.close(1000, null);
        closed = true;
    }

    public boolean isClosed() {
        return closed;
    }

    @Override
    public void onClosed(WebSocket webSocket, int code, String reason) {
        API.debug("已关闭");
        clean();
    }

    @Override
    public void onFailure(WebSocket webSocket, Throwable t, Response response) {
        API.debug("连接失败,次数:" + (failedTimes.get() + 1));
        if (!reconnect) {
            clean();
            return;
        }
        if (failedTimes.getAndIncrement() != wsServers.size() - 1 && wsServers.size() > 1) {
            if (heartBeatTask != null) {
                heartBeatTask.cancel(true);
            }
            if (packetDeliveryTask != null) {
                packetDeliveryTask.cancel(true);
            }
            tryServers();
        } else {
            API.debug("连接失败,所有服务器均已尝试");
            clean();
            API.cleanUp();
        }
    }

    private void clean() {
        closed = true;
        if (heartBeatTask != null) {
            heartBeatTask.cancel(true);
        }
        if (packetDeliveryTask != null) {
            packetDeliveryTask.cancel(true);
        }
        if (executor != null) {
            executor.shutdown();
        }
        if (packetQueue != null) {
            packetQueue.clear();
        }
        listeners.clear();
    }

    private final class PacketDeliveryTask implements Runnable {

        @Override
        public void run() {
            String oldName = Thread.currentThread().getName();
            Thread.currentThread().setName(oldName + "-" + "LivePacket Processor");
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    LivePacket packet = packetQueue.take();
                    notifyListeners(packet);
                }
            } catch (InterruptedException e) {
                // ignored
            } finally {
                API.debug("线程" + Thread.currentThread().getName() + "退出");
            }
        }
    }

    /**
     * 内部数据包监听器, 用来处理和发送心跳包, 保持连接存活
     */
    private class InternalListener implements PacketListener {
        @Override
        public void onPacket(LivePacket packet, LiveRoom room) {
            if (packet.getOperation() == LivePacket.OPERATION_ENTER_ROOM_RESPONSE) {
                API.debug(packet.getDecodedJSON().toString());
                // 收到进房回应, 开始心跳任务
                // 没收到回应就是直接被服务器断开了, 可能是鉴权key有误(认证失败)或者数据格式有误
                LiveRoom.this.heartBeatTask = executor.scheduleAtFixedRate(() -> {
                    ws.send(LivePacket.HEARTBEAT_PACKET_RAW);
                    API.debug("心跳包已发送");
                }, 1, 30, TimeUnit.SECONDS);
            } else if (packet.getOperation() == LivePacket.OPERATION_HEARTBEAT_RESPONSE) {
                popular = ByteBuffer.wrap(packet.getBody()).getInt();
                API.debug("收到心跳回复.人气值:" + popular);
            }
        }
    }
}
