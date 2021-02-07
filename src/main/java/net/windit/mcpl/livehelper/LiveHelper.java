package net.windit.mcpl.livehelper;

import net.windit.bililive.API;
import net.windit.bililive.LiveException;
import net.windit.bililive.LiveRoom;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class LiveHelper extends JavaPlugin {

    public final static String messagePrefix = "[" + ChatColor.LIGHT_PURPLE + "直播助手" +
            ChatColor.WHITE + "]" + " ";
    private static LiveHelper instance;
    private static Map<String, DanmuSummonActivity> activities;
    private LiveRoom room;
    private BukkitTask getFollowersTask;
    private long uid;

    public static LiveHelper getInstance() {
        return instance;
    }

    public static DanmuSummonActivity getSummonActivity(String keyword) {
        for (Map.Entry<String, DanmuSummonActivity> entry : activities.entrySet()) {
            if (keyword.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        API.setDebug(getConfig().getBoolean("debug"));
        activities = new ConcurrentHashMap<>();
        parseActivities();
        openRoom(Bukkit.getConsoleSender());
        uid = getConfig().getLong("bili_uid");
        if (uid > 0) {
            getFollowersTask = Bukkit.getScheduler().runTaskTimerAsynchronously(this, new GetFollowersAndSpawnTask(uid), 20, 20 * 10);
        }
    }

    private void parseActivities() {
        Map<String, ?> map = getConfig().getConfigurationSection("summon.danmu").getValues(false);
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (!(entry.getValue() instanceof ConfigurationSection)) {
                continue;
            }
            ConfigurationSection section = (ConfigurationSection) entry.getValue();
            String displayName = section.getString("activity-name");
            if (activities.containsKey(displayName)) {
                if (activities.get(displayName).isRunning()) {
                    continue;
                }
            }
            String[] mobCfg = section.getString("mob").split("\\*");
            if (mobCfg.length == 2) {
                String mob = mobCfg[0];
                EntityType type = EntityType.fromName(mob);
                if (type != null) {
                    int times = section.getInt("times", 2);
                    String id = (String) entry.getKey();
                    int amount = Integer.parseInt(mobCfg[1]);
                    String keyword = section.getString("keyword");
                    DanmuSummonActivity activity = new DanmuSummonActivity(id, displayName, keyword, times, EntityType.fromName(mob), amount);
                    activities.put(keyword, activity);
                }
            }
        }
    }

    @Override
    public void onDisable() {
        instance = null;
        closeRoom();
        if (getFollowersTask != null) {
            getFollowersTask.cancel();
            getFollowersTask = null;
        }
        activities = null;
        API.cleanUp();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.isOp()) {
            return true;
        }
        if (args.length < 1) {
            sendUsage(sender, label);
            return true;
        }
        if ("close".equalsIgnoreCase(args[0])) {
            if (room != null) {
                closeRoom();
                sender.sendMessage(messagePrefix + ChatColor.GREEN + "房间消息监听已关闭.");
                return true;
            }
            sender.sendMessage(messagePrefix + ChatColor.RED + "没有打开房间.");
            return true;
        }
        if ("open".equalsIgnoreCase(args[0])) {
            openRoom(sender);
            return true;
        }
        if ("reload".equalsIgnoreCase(args[0])) {
            reload(sender);
            return true;
        }
        sendUsage(sender, label);
        return true;
    }

    private void reload(CommandSender sender) {
        reloadConfig();
        API.setDebug(getConfig().getBoolean("debug"));
        int oldRoomId = 0;
        if (room != null) {
            oldRoomId = room.getRoomId();
        }
        int newRoomId = getConfig().getInt("room_id");
        if (newRoomId != 0 && oldRoomId != newRoomId) {
            closeRoom();
            openRoom(sender);
        }
        if (getFollowersTask != null) {
            getFollowersTask.cancel();
            getFollowersTask = null;
        }
        uid = getConfig().getLong("bili_uid");
        getFollowersTask = Bukkit.getScheduler().runTaskTimerAsynchronously(this, new GetFollowersAndSpawnTask(uid), 20, 20 * 10);
        parseActivities();
        sender.sendMessage(messagePrefix + ChatColor.GREEN + "已重载配置");
    }

    private void sendUsage(CommandSender sender, String label) {
        if (sender == null || label == null) {
            return;
        }
        sender.sendMessage(messagePrefix + ChatColor.GREEN + "使用方法:");
        sender.sendMessage(messagePrefix + ChatColor.GREEN + "/" + label + " open ----- 打开房间连接 (插件启用时自动打开).");
        sender.sendMessage(messagePrefix + ChatColor.GREEN + "/" + label + " close ----- 关闭房间连接, 将停止接受房间消息.");
        sender.sendMessage(messagePrefix + ChatColor.GREEN + "/" + label + " reload ----- 重载配置文件 (若配置中的房间号无变化, 将不会影响已打开的房间).");
    }

    private void openRoom(CommandSender sender) {
        if (room != null && !room.isClosed()) {
            sender.sendMessage(messagePrefix + ChatColor.RED + "房间已经打开.");
            return;
        }
        int roomId = getConfig().getInt("room_id");
        if (roomId == 0) {
            return;
        }
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            try {
                LiveRoom room = new LiveRoom(roomId);
                PluginPacketListener listener = new PluginPacketListener();
                if (sender != null) {
                    sender.sendMessage(messagePrefix + ChatColor.GREEN + "正尝试建立与主播房间的连接...");
                    listener.setEnterRoomCallback(x -> x.sendMessage(messagePrefix + ChatColor.GREEN + "已建立与主播房间的连接"), sender);
                }
                room.registerListener(listener);
                room.connect();
                this.room = room;
            } catch (LiveException e) {
                getLogger().warning(ChatColor.RED + "打开房间时发送错误");
                e.printStackTrace();
                if (sender != null) {
                    sender.sendMessage(messagePrefix + ChatColor.RED + "打开房间时发送错误:" + e.getMessage());
                    sender.sendMessage(messagePrefix + "错误详情已记录到后台.");
                }
            }
        });
    }

    private void closeRoom() {
        if (room != null) {
            room.close();
        }
        room = null;
    }
}
