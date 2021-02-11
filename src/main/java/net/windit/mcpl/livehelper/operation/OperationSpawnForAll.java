package net.windit.mcpl.livehelper.operation;

import net.windit.mcpl.livehelper.LiveHelper;
import net.windit.mcpl.livehelper.Utils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class OperationSpawnForAll extends Operation {
    public OperationSpawnForAll(String data) {
        super(data);
    }

    @Override
    public void doIt() {
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
            int finalMobAmount = mobAmount;
            EntityType finalType = type;
            Bukkit.getOnlinePlayers().forEach(p -> {
                Location location = p.getLocation();
                for (int i = 0; i < finalMobAmount; i++) {
                    int radius = LiveHelper.getInstance().getConfig().getInt("summon.radius", 10);
                    int x = location.getBlockX() + random.nextInt(radius - (-radius) + 1) + (-radius);
                    int z = location.getBlockZ() + random.nextInt(radius - (-radius) + 1) + (-radius);
                    int y = Utils.getHighestSolidBlockY(location.getWorld(), x, z) + 1;
                    Location randomLocation = location.clone();
                    randomLocation.setX(x);
                    randomLocation.setY(y + 0.2);
                    randomLocation.setZ(z);
                    location.getWorld().spawnEntity(randomLocation, finalType);
                }
            });
        }
    }
}
