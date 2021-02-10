package net.windit.mcpl.livehelper;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.windit.bililive.API;
import net.windit.bililive.LivePacket;
import net.windit.bililive.LiveRoom;
import net.windit.bililive.PacketListener;

import java.util.function.Consumer;
import java.util.logging.Level;

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
                        API.debug(userName + " " + msg);
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
                    case "SEND_GIFT":
                    case "COMBO_SEND":
                        JsonObject data2 = object.getAsJsonObject("data");
                        String username2 = data2.get("uname").getAsString();
                        String giftName;
                        String action = data2.get("action").getAsString();
                        int num;
                        if ("SEND_GIFT".equalsIgnoreCase(cmd)) {
                            giftName = data2.get("giftName").getAsString();
                            num = data2.get("num").getAsInt();
                            handleGift(false, username2, giftName, action, num);
                        } else {
                            giftName = data2.get("gift_name").getAsString();
                            num = data2.get("combo_num").getAsInt();
                            handleGift(true, username2, giftName, action, num);
                        }
                        API.debug(username2 + " " + action + " " + giftName + " " + num);
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

    private void handleGift(boolean combo, String uname, String giftName, String action, int giftNum) {
        Gift gift = LiveHelper.getGift(giftName);
        if (gift != null) {
            try {
                gift.performOperations(uname, action, giftNum);
            } catch (Exception e) {
                LiveHelper.getInstance().getLogger().log(Level.WARNING, "处理礼物数据时出错", e);
            }
        }
    }

    void setEnterRoomCallback(Consumer<T> consumer, T t) {
        this.consumer = consumer;
        this.t = t;
    }
}
