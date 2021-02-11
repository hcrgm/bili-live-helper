package net.windit.mcpl.livehelper.operation;

import net.windit.mcpl.livehelper.LiveHelper;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

public class OperationEffect extends Operation {
    public OperationEffect(String data) {
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
            }
        } catch (NumberFormatException e) {
            // ignored
        }
    }
}
