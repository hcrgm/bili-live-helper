package net.windit.mcpl.livehelper;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.windit.bililive.API;
import net.windit.bililive.LivePacket;
import net.windit.bililive.LiveRoom;
import net.windit.bililive.PacketListener;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;

import java.util.Map;

public class PluginPacketListener implements PacketListener {
    @Override
    public void onPacket(LivePacket packet, LiveRoom room) {
        if (packet.getOperation() == LivePacket.OPERATION_MESSAGE) {
            packet.getDecodedJSON().forEach(jsonStr -> {
                JsonObject object = (JsonObject) new JsonParser().parse(jsonStr);
                String cmd = object.get("cmd").getAsString();
                if ("DANMU_MSG".equalsIgnoreCase(cmd)) {
                    JsonArray info = object.getAsJsonArray("info");
                    String text = info.get(1).getAsString();
                    JsonArray userInfo = info.get(2).getAsJsonArray();
                    long uid = userInfo.get(0).getAsLong();
                    String userName = userInfo.get(1).getAsString();
                    String nameAndId = String.format("%s(%d)", userName, uid);
                    API.debug(Thread.currentThread().getName());
                    API.debug("弹幕:" + nameAndId + ":" + text);
                    LiveHelper plugin = LiveHelper.getInstance();
                    Map<String, ?> map = plugin.getConfig().getConfigurationSection("summon.danmu").getValues(false);
                    System.out.println(map);
                    for (Map.Entry<?, ?> entry : map.entrySet()) {
                        if (!(entry.getValue() instanceof ConfigurationSection)) {
                            continue;
                        }
                        ConfigurationSection section = (ConfigurationSection) entry.getValue();
                        if (text.contains(section.getString("text"))) {
                            int times = section.getInt("times", 1);
                            String[] mobCfg = section.getString("mob").split("\\*");
                            if (mobCfg.length == 2) {
                                String mob = mobCfg[0];
                                int amount = Integer.parseInt(mobCfg[1]);
                                DanmuSummonActivity activity = LiveHelper.getSummonActivity(times, EntityType.fromName(mob), amount);
                                activity.increaseTimes();
                            }
                        }
                    }
                }
            });
        }
    }
}
