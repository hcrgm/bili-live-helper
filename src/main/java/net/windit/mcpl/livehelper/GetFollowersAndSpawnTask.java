package net.windit.mcpl.livehelper;

import net.windit.bililive.API;
import net.windit.mcpl.livehelper.operation.Operation;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.List;

public class GetFollowersAndSpawnTask implements Runnable {

    private final long uid;
    private final String actionToDo;
    private final Plugin plugin;
    private int lastFollowers = -1;

    public GetFollowersAndSpawnTask(long uid, String actionToDo) {
        this.uid = uid;
        this.actionToDo = actionToDo;
        this.plugin = LiveHelper.getInstance();
    }

    @Override
    public void run() {
        API.UserInfo info = API.getUserInfo(uid);
        if (info == null) {
            return;
        }
        int followers = info.fans;
        String nickname = info.name;
        if (lastFollowers != -1) {
            if (followers - lastFollowers > 0) {
                int delta = followers - lastFollowers;
                Player player = Bukkit.getPlayerExact(plugin.getConfig().getString("host_playername"));
                if (player != null) {
                    Bukkit.getScheduler().runTask(LiveHelper.getInstance(), () -> {
                        List<Operation> operations = LiveHelper.getOperations(actionToDo);
                        if (operations != null) {
                            for (int i = 0; i < delta; i++) {
                                operations.forEach(Operation::doIt);
                            }
                        }
                    });
                }
                Bukkit.broadcastMessage(LiveHelper.messagePrefix + ChatColor.translateAlternateColorCodes('&', plugin.getConfig().getString("follow.msg")
                        .replaceAll("\\{昵称}", nickname)
                        .replaceAll("\\{涨粉数}", String.valueOf(delta))
                        .replaceAll("\\{当前粉丝数}", String.valueOf(followers))));
            }
        }
        lastFollowers = followers;
    }
}
