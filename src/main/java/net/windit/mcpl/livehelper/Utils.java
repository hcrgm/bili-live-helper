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
        Bukkit.getOnlinePlayers().forEach(player -> {
            BaseComponent[] components = TextComponent.fromLegacyText(ChatColor.translateAlternateColorCodes('&', msg));
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, components);
        });
    }
}
