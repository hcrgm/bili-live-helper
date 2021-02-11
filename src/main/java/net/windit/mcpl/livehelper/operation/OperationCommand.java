package net.windit.mcpl.livehelper.operation;

import org.bukkit.Bukkit;

public class OperationCommand extends Operation {

    public OperationCommand(String data) {
        super(data);
    }

    @Override
    public void doIt() {
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), data);
    }
}
