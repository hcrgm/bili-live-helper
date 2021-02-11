package net.windit.mcpl.livehelper.operation;

import net.windit.mcpl.livehelper.LiveHelper;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class OperationHeal extends Operation {
    public OperationHeal(String data) {
        super(data);
    }

    @Override
    public void doIt() {
        Player player = Bukkit.getPlayerExact(LiveHelper.getInstance().getConfig().getString("host_playername"));
        if (player == null) {
            return;
        }
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
        } catch (NumberFormatException e) {
            // ignored
        }
    }
}
