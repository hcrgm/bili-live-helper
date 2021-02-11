package net.windit.mcpl.livehelper.operation;

import net.windit.mcpl.livehelper.LiveHelper;
import net.windit.mcpl.livehelper.Utils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class OperationSpawn extends Operation {
    public OperationSpawn(String data) {
        super(data);
    }

    @Override
    public void doIt() {
        Player player = Bukkit.getPlayerExact(LiveHelper.getInstance().getConfig().getString("host_playername"));
        if (player == null) {
            return;
        }
        Location location = player.getLocation();
        String[] mobs = data.split("\\|");
        Random random = ThreadLocalRandom.current();
        String mob;
        if (mobs.length > 1) {
            mob = mobs[random.nextInt(mobs.length)];
        } else {
            mob = mobs[0];
        }
        int mobAmount = 1;
        EntityType type = null;
        String[] tmp = mob.split("\\*");
        if (tmp.length > 0) {
            type = EntityType.fromName(tmp[0]);
            if (tmp.length > 1) {
                try {
                    mobAmount = Integer.parseInt(tmp[1]);
                } catch (NumberFormatException e) {
                    // ignored
                }
            }
        }
        if (type != null) {
            for (int i = 0; i < mobAmount; i++) {
                // 在一个圆的区域内随机生成, 相对圆心的x,z坐标偏移量: [-radius,radius]
                int radius = LiveHelper.getInstance().getConfig().getInt("summon.radius", 10);
                int x = location.getBlockX() + random.nextInt(radius - (-radius) + 1) + (-radius);
                int z = location.getBlockZ() + random.nextInt(radius - (-radius) + 1) + (-radius);
                int y = Utils.getHighestSolidBlockY(location.getWorld(), x, z) + 1;
                Location randomLocation = location.clone();
                randomLocation.setX(x);
                randomLocation.setY(y + 0.2);
                randomLocation.setZ(z);
                location.getWorld().spawnEntity(randomLocation, type);
            }
        }
    }
}
