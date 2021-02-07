package net.windit.mcpl.livehelper;

import net.windit.bililive.API;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.List;
import java.util.Random;

public class GetFollowersAndSpawnTask implements Runnable {

    private final long uid;
    private final Random random = new Random();
    private final Plugin plugin;
    private int lastFollowers = -1;

    public GetFollowersAndSpawnTask(long uid) {
        this.uid = uid;
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
                    Location location = player.getLocation();
                    List<String> mobs = plugin.getConfig().getConfigurationSection("summon").getStringList("follow.mobs");
                    Bukkit.getScheduler().runTask(LiveHelper.getInstance(), () -> {
                        World world = Bukkit.getWorld(plugin.getConfig().getString("summon.world", "world"));
                        if (world == null) {
                            world = Bukkit.getWorld("world");
                        }
                        if (world != null) {
                            for (int i = 0; i < delta; i++) {
                                EntityType type = EntityType.fromName(mobs.get(random.nextInt(mobs.size())));
                                if (type != null) {
                                    // 在一个圆的区域内随机生成, 相对圆心的x,z坐标偏移量: [-radius,radius]
                                    int radius = plugin.getConfig().getInt("summon.radius",10);
                                    int x = location.getBlockX() + random.nextInt(radius - (-radius) + 1) + (-radius);
                                    int z = location.getBlockZ() + random.nextInt(radius - (-radius) + 1) + (-radius);
                                    int y = LiveHelper.getHighestSolidBlockY(location.getWorld(), x, z) + 1;
                                    Location randomLocation = location.clone();
                                    randomLocation.setX(x);
                                    randomLocation.setY(y + 0.2);
                                    randomLocation.setZ(z);
                                    world.spawnEntity(randomLocation, type);
                                }
                            }
                        }
                    });
                }
                Bukkit.broadcastMessage(LiveHelper.messagePrefix + ChatColor.GOLD + nickname +
                        ChatColor.GREEN + " 新增了 " + ChatColor.GOLD + delta +
                        ChatColor.GREEN + " 名随从! 当前随从数: " + ChatColor.GOLD + followers);
            }
        }
        lastFollowers = followers;
    }
}
