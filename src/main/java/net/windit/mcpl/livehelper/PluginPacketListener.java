package net.windit.mcpl.livehelper;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.windit.bililive.LivePacket;
import net.windit.bililive.LiveRoom;
import net.windit.bililive.PacketListener;

import java.util.function.Consumer;

public class PluginPacketListener<T> implements PacketListener {

    private Consumer<T> consumer;
    private T t;

    @Override
    public void onPacket(LivePacket packet, LiveRoom room) {
        if (consumer != null && t != null && packet.getOperation() == LivePacket.OPERATION_ENTER_ROOM_RESPONSE) {
            consumer.accept(t);
        }
        if (packet.getOperation() == LivePacket.OPERATION_MESSAGE) {
            packet.getDecodedJSON().forEach(jsonStr -> {
                JsonObject object = (JsonObject) new JsonParser().parse(jsonStr);
                String cmd = object.get("cmd").getAsString();
                switch (cmd) {
                    // 弹幕消息
                    case "DANMU_MSG":
                        JsonArray info = object.getAsJsonArray("info");
                        String msg = info.get(1).getAsString();
                        JsonArray userInfo = info.get(2).getAsJsonArray();
                        long uid = userInfo.get(0).getAsLong();
                        String userName = userInfo.get(1).getAsString();
                        handleDanmu(userName, msg);
                        break;
                    // 进房
                    case "INTERACT_WORD":
                    case "WELCOME":
                        // 舰长进房
                    case "GUARD_WELCOME":
                        JsonObject data = object.getAsJsonObject("data");
                        String username = data.get("uname").getAsString();
                        handleEnterRoom(username);
                        break;
                    default:
                        break;
                }
            });
        }
    }

    private void handleDanmu(String username, String message) {
        DanmuSummonActivity activity = LiveHelper.getSummonActivity(message);
        if (activity != null) {
            activity.increaseTimes(username);
        }
    }

    private void handleEnterRoom(String username) {
        Utils.broadcastActionBar(LiveHelper.getInstance().getConfig().getString("enter_room_msg").replaceAll("\\{昵称}", username));
    }

    void setEnterRoomCallback(Consumer<T> consumer, T t) {
        this.consumer = consumer;
        this.t = t;
    }
}
