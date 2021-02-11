package net.windit.mcpl.livehelper.operation;

import net.windit.mcpl.livehelper.LiveHelper;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class OperationFeed extends Operation {
    public OperationFeed(String data) {
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
            player.setFoodLevel(player.getFoodLevel() + value);
        } catch (NumberFormatException e) {
            // ignored
        }
    }
}
