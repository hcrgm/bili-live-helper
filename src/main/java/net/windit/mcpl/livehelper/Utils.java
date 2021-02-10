package net.windit.mcpl.livehelper;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;

public class Utils {
    public static int getHighestSolidBlockY(World world, int x, int z) {
        int maxY = world.getMaxHeight();
        for (int i = maxY - 1; i > 0; i--) {
            if (world.getBlockAt(x, i, z).getType().isSolid()) {
                return i;
            }
        }
        return 1;
    }

    public static void broadcastActionBar(String msg) {
        if (msg == null || msg.isEmpty()) {
            return;
        }
        Bukkit.getOnlinePlayers().forEach(player -> {
            BaseComponent[] components = TextComponent.fromLegacyText(ChatColor.translateAlternateColorCodes('&', msg));
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, components);
        });
    }

    public static String humanTime(long seconds) {
        if (seconds < 0) {
            throw new IllegalArgumentException("second must be greater than or equal to 0.");
        }
        if (seconds < 60) {
            return seconds + "秒";
        } else if (seconds < 3600) {
            long minutes = seconds / 60;
            long secs = seconds - minutes * 60;
            return minutes + "分" + secs + "秒";
        } else {
            long hours = seconds / 3600;
            long minutes = (seconds - hours * 3600) / 60;
            long secs = seconds - hours * 3600 - minutes * 60;
            return hours + "小时" + minutes + "分" + secs + "秒";
        }
    }
}
