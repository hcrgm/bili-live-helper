package net.windit.mcpl.livehelper;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class Operation {
    private final Type type;
    private final String data;

    public Operation(Type type, String data) {
        this.type = type;
        this.data = data;
    }

    public void doIt() {
        Player player = null;
        if (type != Type.COMMAND) {
            player = Bukkit.getPlayerExact(LiveHelper.getInstance().getConfig().getString("host_playername"));
            if (player == null) {
                return;
            }
        }
        switch (type) {
            case COMMAND:
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), data);
                break;
            case HEAL:
                if (player.isDead()) {
                    return;
                }
                try {
                    int value = Integer.parseInt(data);
                    value = Math.max(value, 0);
                    double health = player.getHealth() + value;
                    if (health > player.getMaxHealth()) {
                        health = player.getMaxHealth();
                    }
                    player.setHealth(health);
                    break;
                } catch (NumberFormatException e) {
                    return;
                }
            case HEALALL:
                Bukkit.getOnlinePlayers().forEach(p -> {
                    if (!p.isDead()) {
                        p.setHealth(p.getMaxHealth());
                        p.setFoodLevel(20);
                        p.setFireTicks(0);

                    }
                });
                break;
            case FEED:
                if (player.isDead()) {
                    return;
                }
                try {
                    int value = Integer.parseInt(data);
                    player.setFoodLevel(player.getFoodLevel() + value);
                    break;
                } catch (NumberFormatException e) {
                    return;
                }
            case EFFECT:
                if (player.isDead()) {
                    return;
                }
                String[] parts = data.split(",");
                if (parts.length != 3) {
                    return;
                }
                try {
                    PotionEffectType type = PotionEffectType.getByName(parts[0]);
                    if (type != null) {
                        int duration = Integer.parseInt(parts[1]);
                        int amplifier = Integer.parseInt(parts[2]);
                        player.addPotionEffect(type.createEffect(duration * 20, amplifier));
                        break;
                    }
                } catch (NumberFormatException e) {
                    return;
                }
            case SPAWN:
                Player host = Bukkit.getPlayerExact(LiveHelper.getInstance().getConfig().getString("host_playername"));
                if (host != null) {
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
                break;
            case SPAWNFORALL:
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
            default:
                break;
        }
    }

    @Override
    public String toString() {
        return "Operation{" +
                "type=" + type +
                ", data='" + data + '\'' +
                '}';
    }

    public enum Type {
        COMMAND,
        HEAL,
        HEALALL,
        FEED,
        EFFECT,
        SPAWN,
        SPAWNFORALL;
        private static final Map<String, Type> TYPES = new HashMap<>();

        static {
            for (Type type : values()) {
                TYPES.put(type.name(), type);
            }
        }

        public static Type getTypeByName(String name) {
            return TYPES.get(name.toUpperCase());
        }
    }
}