package net.windit.mcpl.livehelper;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Zombie;
import org.bukkit.inventory.ItemStack;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class DanmuSummonActivity {
    private final int targetTimes;
    private final EntityType mobToSpawn;
    private final int mobAmount;
    private String id;
    private String name;
    private int times = 0;
    private String keyword;
    private volatile boolean running;

    public DanmuSummonActivity(String id, String name, String keyword, int targetTimes, EntityType mobToSpawn, int mobAmount) {
        this.id = id;
        this.name = name;
        this.keyword = keyword;
        this.targetTimes = targetTimes;
        this.mobToSpawn = mobToSpawn;
        this.mobAmount = mobAmount;
    }

    public boolean isFinished() {
        return times >= targetTimes;
    }

    public boolean isRunning() {
        return running;
    }

    public void reset() {
        this.times = 0;
        running = false;
    }

    public void increaseTimes(String nickname) {
        running = true;
        if (times++ == 0) {
            Bukkit.broadcastMessage(LiveHelper.messagePrefix + ChatColor.translateAlternateColorCodes('&', LiveHelper.getInstance().getConfig().getString("summon.danmu.start-msg-format")
                    .replaceAll("\\{昵称}", nickname)
                    .replaceAll("\\{活动名}", name)));
        } else if (times + 1 <= targetTimes) {
            Bukkit.broadcastMessage(LiveHelper.messagePrefix + ChatColor.translateAlternateColorCodes('&', LiveHelper.getInstance().getConfig().getString("summon.danmu.running-msg-format")
                    .replaceAll("\\{昵称}", nickname)
                    .replaceAll("\\{活动名}", name)
                    .replaceAll("\\{剩余次数}", String.valueOf(targetTimes-times))));
        }
        if (isFinished()) {
            reset();
            String notice = LiveHelper.messagePrefix + ChatColor.translateAlternateColorCodes('&', LiveHelper.getInstance().getConfig().getString("summon.danmu.end-msg-format")
                    .replaceAll("\\{昵称}", nickname)
                    .replaceAll("\\{活动名}", name));
            Bukkit.getScheduler().runTask(LiveHelper.getInstance(), () -> Bukkit.getOnlinePlayers().forEach(player -> {
                Location location = player.getLocation();
                Random random = ThreadLocalRandom.current();
                for (int i = 0; i < mobAmount; i++) {
                    int radius = LiveHelper.getInstance().getConfig().getInt("summon.radius", 10);
                    int x = location.getBlockX() + random.nextInt(radius - (-radius) + 1) + (-radius);
                    int z = location.getBlockZ() + random.nextInt(radius - (-radius) + 1) + (-radius);
                    int y = Utils.getHighestSolidBlockY(location.getWorld(), x, z) + 1;
                    Location randomLocation = location.clone();
                    randomLocation.setX(x);
                    randomLocation.setY(y + 0.2);
                    randomLocation.setZ(z);
                    Entity entity = location.getWorld().spawnEntity(randomLocation, mobToSpawn);
                    if (mobToSpawn == EntityType.ZOMBIE) {
                        Zombie zombie = (Zombie) entity;
                        zombie.getEquipment().setItemInMainHand(new ItemStack(Material.IRON_SWORD));
                    }
                }
            }));
            Bukkit.broadcastMessage(notice);
        }
    }
}
