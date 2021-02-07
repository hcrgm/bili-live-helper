package net.windit.mcpl.livehelper;

import net.windit.bililive.API;
import net.windit.bililive.LiveException;
import net.windit.bililive.LiveRoom;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;

public final class LiveHelper extends JavaPlugin {

    public final static String messagePrefix = "[" + ChatColor.LIGHT_PURPLE + "直播助手" +
            ChatColor.WHITE + "]" + " ";
    private static LiveHelper instance;
    private List<LiveRoom> rooms;
    private BukkitTask getFollowersTask;
    private static volatile DanmuSummonActivity summonActivity;

    public static LiveHelper getInstance() {
        return instance;
    }

    public static DanmuSummonActivity getSummonActivity(int targetTimes, EntityType mob, int amount) {
        if (summonActivity == null) {
            summonActivity = new DanmuSummonActivity(targetTimes, mob, amount);
        } else {
            if (summonActivity.isFinished()) {
                summonActivity = new DanmuSummonActivity(targetTimes, mob, amount);
            }
        }
        return summonActivity;
    }

    static int getHighestSolidBlockY(World world, int x, int z) {
        int maxY = world.getMaxHeight();
        for (int i = maxY - 1; i > 0; i--) {
            if (world.getBlockAt(x, i, z).getType().isSolid()) {
                return i;
            }
        }
        return 1;
    }

    @Override
    public void onEnable() {
        API.setDebug(true);
        instance = this;
        rooms = new ArrayList<>();
        saveDefaultConfig();
        getFollowersTask = Bukkit.getScheduler().runTaskTimerAsynchronously(this, new GetFollowersAndSpawnTask(getConfig().getLong("bili_uid"))
                , 20, 20 * 10);
    }

    @Override
    public void onDisable() {
        instance = null;
        if (rooms != null) {
            rooms.forEach(LiveRoom::close);
            rooms.clear();
            rooms = null;
        }
        if (getFollowersTask != null) {
            getFollowersTask.cancel();
        }
        API.cleanUp();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if ("close".equalsIgnoreCase(args[0])) {
            if (!rooms.isEmpty()) {
                LiveRoom room = rooms.get(rooms.size() - 1);
                room.close();
                rooms.remove(room);
                sender.sendMessage(ChatColor.GREEN + "已关闭房间" + room.getRoomId());
            }
            return true;
        }
        int roomId;
        try {
            roomId = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "房间号不是数字");
            return true;
        }
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            LiveRoom room = new LiveRoom(roomId);
            rooms.add(room);
            sender.sendMessage(ChatColor.GREEN + "已打开房间");
            try {
                room.registerListener(new PluginPacketListener());
                room.connect();
            } catch (LiveException e) {
                e.printStackTrace();
                rooms.remove(room);
                sender.sendMessage(ChatColor.RED + "连接房间失败:" + e.getMessage());
            }
        });
        return true;
    }
}
