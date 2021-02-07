package net.windit.bililive;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

public final class API {
    public final static String BILI_ROOM_INIT_URI = "https://api.live.bilibili.com/room/v1/Room/room_init?id=";
    public final static String BILI_DANMU_CONF_URI = "https://api.live.bilibili.com/room/v1/Danmu/getConf?room_id=";
    public final static String BILI_FOLLWERS_URI = "https://api.bilibili.com/x/relation/followers?vmid=";
    public final static String BILI_USER_CARD_URI = "https://api.bilibili.com/x/web-interface/card?mid=";
    public static final Gson gson = new Gson();
    private final static String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/88.0.4324.146 Safari/537.36 Edg/88.0.705.62";
    private final static ReentrantLock LOCK = new ReentrantLock();
    public static OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .writeTimeout(5, TimeUnit.SECONDS)
            .build();
    private static volatile boolean debug = false;

    public static void debug(String log) {
        if (!debug) {
            return;
        }
        LOCK.lock();
        try {
            System.out.println(log);
        } finally {
            LOCK.unlock();
        }
    }

    public static void setDebug(boolean debug) {
        API.debug = debug;
    }

    public static ThreadFactory threadFactory(String name) {
        return new ThreadFactory() {
            private final AtomicLong count = new AtomicLong(0);

            @Override
            public Thread newThread(Runnable runnable) {
                Thread thread = new Thread(runnable, name + "-" + count.getAndIncrement());
                // 房间相关线程必须是守护进程, 防止线程阻止程序退出
                thread.setDaemon(true);
                return thread;
            }
        };
    }

    public static String getURI(String uri) {
        Request request = new Request.Builder()
                .url(uri)
                .get()
                .removeHeader("User-Agent")
                .addHeader("User-Agent", USER_AGENT)
                .build();
        try (Response response = client.newCall(request).execute()) {
            String body = response.body().string();
            API.debug("Request-" + uri);
            return body;
        } catch (Exception e) {
            debug("Request error:" + e.getMessage());
            return null;
        }
    }

    public static int getRealRoomId(int displayId) {
        int realId = -1;
        String body = getURI(BILI_ROOM_INIT_URI + displayId);
        if (body != null) {
            JsonObject object = (JsonObject) new JsonParser().parse(body);
            if (object.get("code").getAsInt() == 0) {
                realId = object.getAsJsonObject("data").get("room_id").getAsInt();
            }
        }
        return realId;
    }


    public static DanmuConf getDanmuConf(int realRoomId) {
        DanmuConf danmuConf = null;
        String body = getURI(BILI_DANMU_CONF_URI + realRoomId);
        if (body != null) {
            JsonObject object = (JsonObject) new JsonParser().parse(body);
            if (object.get("code").getAsInt() == 0) {
                danmuConf = gson.fromJson(object.getAsJsonObject("data"), DanmuConf.class);
            }
        }
        return danmuConf;
    }

    /**
     * 获取用户的粉丝数.
     *
     * @param uid 用户的uid
     * @return 粉丝数
     * @deprecated 请用#getUserInfo(long)
     */
    @Deprecated
    public static int getFollowers(long uid) {
        int followers = -1;
        String body = getURI(BILI_FOLLWERS_URI + uid);
        if (body != null) {
            JsonObject object = (JsonObject) new JsonParser().parse(body);
            if (object.get("code").getAsInt() == 0) {
                followers = object.getAsJsonObject("data").get("total").getAsInt();
            }
        }
        return followers;
    }

    public static UserInfo getUserInfo(long uid) {
        String body = getURI(BILI_USER_CARD_URI + uid);
        UserInfo info = null;
        if (body != null) {
            JsonObject object = (JsonObject) new JsonParser().parse(body);
            if (object.get("code").getAsInt() == 0) {
                info = gson.fromJson(object.getAsJsonObject("data").getAsJsonObject("card"), UserInfo.class);
            }
        }
        return info;
    }

    public static void cleanUp() {
        client.dispatcher().executorService().shutdown();
        client.connectionPool().evictAll();
        client = new OkHttpClient.Builder()
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(5, TimeUnit.SECONDS)
                .writeTimeout(5, TimeUnit.SECONDS)
                .build();
    }

    public static class DanmuConf {
        public List<Map<String, ?>> host_server_list;
        public String token;
    }

    public static class UserInfo {
        public long mid;
        public String name;
        public int fans;
    }
}
