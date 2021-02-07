package net.windit.mcpl.livehelper;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * @deprecated 草稿设计案
 */
@Deprecated
public class DanmuSummonActivity {
    private int times = 0;
    private final int targetTimes;
    private final EntityType mobToSpawn;
    private final int mobAmount;
    private long lastIncreaseTime = -1;
    public DanmuSummonActivity(int targetTimes, EntityType mobToSpawn, int mobAmount) {
        this.targetTimes = targetTimes;
        this.mobToSpawn = mobToSpawn;
        this.mobAmount = mobAmount;
    }
    public boolean isFinished() {
        return times >= targetTimes;
    }
    public void increaseTimes() {
        if (lastIncreaseTime == -1) {
            lastIncreaseTime = System.currentTimeMillis();
        }
        // TODO:超时判断设计不合理, 应设置一个定长的延迟任务判断
        if (System.currentTimeMillis() - lastIncreaseTime > 600000) {
            times = targetTimes+1;
            Bukkit.broadcastMessage(LiveHelper.messagePrefix + "因超时, 召唤" + mobToSpawn.name() + "失败.");
            return;
        }
        times++;
        System.out.println("+1=" + times);
        if (isFinished()) {
            Bukkit.getScheduler().runTask(LiveHelper.getInstance(),()-> Bukkit.getOnlinePlayers().forEach(player -> {
                Location location = player.getLocation();
                Random random = ThreadLocalRandom.current();
                for (int i=0;i<mobAmount;i++) {
                    int radius = LiveHelper.getInstance().getConfig().getInt("summon.radius",10);
                    int x = location.getBlockX() + random.nextInt(radius - (-radius) + 1) + (-radius);
                    int z = location.getBlockZ() + random.nextInt(radius - (-radius) + 1) + (-radius);
                    int y = LiveHelper.getHighestSolidBlockY(location.getWorld(), x, z) + 1;
                    Location randomLocation = location.clone();
                    randomLocation.setX(x);
                    randomLocation.setY(y + 0.2);
                    randomLocation.setZ(z);
                    location.getWorld().spawnEntity(randomLocation,mobToSpawn);
                }
                player.sendMessage(LiveHelper.messagePrefix + "召唤" + mobToSpawn.name() + "成功!");
            }));
        }
    }
}
