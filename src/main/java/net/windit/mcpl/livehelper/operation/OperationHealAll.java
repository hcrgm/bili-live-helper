package net.windit.mcpl.livehelper.operation;

import org.bukkit.Bukkit;

public class OperationHealAll extends Operation {
    public OperationHealAll(String data) {
        super(data);
    }

    @Override
    public void doIt() {
        Bukkit.getOnlinePlayers().forEach(p -> {
            if (!p.isDead()) {
                p.setHealth(p.getMaxHealth());
                if (p.getFoodLevel() < 20) {
                    p.setFoodLevel(20);
                }
                p.setFireTicks(0);
            }
        });
    }
}
