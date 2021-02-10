package net.windit.mcpl.livehelper;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.List;

public class Gift {

    private final String actionToDo;
    private final String notification;
    private final String name;
    private final boolean multiply;

    public Gift(String name, String notification, String actionToDo, boolean multiply) {
        this.name = name;
        this.notification = notification;
        this.actionToDo = actionToDo;
        this.multiply = multiply;
    }

    public void performOperations(String sender, String senderAction, int giftNum) {
        List<Operation> operations = LiveHelper.getOperations(actionToDo);
        if (operations != null) {
            String msg = LiveHelper.messagePrefix + ChatColor.translateAlternateColorCodes('&', notification.replaceAll("\\{昵称}", sender).replaceAll("\\{观众动作名}", senderAction)
                    .replaceAll("\\{礼物名}", name).replaceAll("\\{数量}", String.valueOf(giftNum))
                    .replaceAll("\\{主播玩家名}", LiveHelper.getInstance().getConfig().getString("host_playername")));
            if (msg.contains("all:")) {
                Bukkit.broadcastMessage(msg.replace("all:", ""));
            } else {
                Player player = Bukkit.getPlayerExact(LiveHelper.getInstance().getConfig().getString("host_playername"));
                if (player != null) {
                    player.sendMessage(msg);
                }
            }

            Bukkit.getScheduler().runTask(LiveHelper.getInstance(), () -> {
                int tmp = 1;
                if (multiply) {
                    tmp = giftNum;
                }
                for (int i = 0; i < tmp; i++) {
                    operations.forEach(Operation::doIt);
                }
            });
        }
    }

    @Override
    public String toString() {
        return "Gift{" +
                "actionToDo='" + actionToDo + '\'' +
                ", notification='" + notification + '\'' +
                ", name='" + name + '\'' +
                ", multiply=" + multiply +
                '}';
    }
}
