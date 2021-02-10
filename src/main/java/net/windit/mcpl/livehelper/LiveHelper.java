package net.windit.mcpl.livehelper;

import net.windit.bililive.API;
import net.windit.bililive.LiveException;
import net.windit.bililive.LiveRoom;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class LiveHelper extends JavaPlugin {

    public final static String messagePrefix = "[" + ChatColor.LIGHT_PURPLE + "直播助手" +
            ChatColor.WHITE + "]" + " ";
    private static LiveHelper instance;
    private static Map<String, DanmuSummonActivity> activities;
    private static Map<String, Gift> gifts;
    private static Map<String, List<Operation>> actions;
    private LiveRoom room;
    private BukkitTask getFollowersTask;

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

    public static Gift getGift(String name) {
        return gifts.get(name);
    }

    public static List<Operation> getOperations(String actionName) {
        return actions.get(actionName);
    }

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        API.setDebug(getConfig().getBoolean("debug"));
        activities = new ConcurrentHashMap<>();
        gifts = new ConcurrentHashMap<>();
        actions = new ConcurrentHashMap<>();
        parseActions();
        parseActivities();
        parseGifts();
        parseFollow();
        openRoom(Bukkit.getConsoleSender());
    }

    private void parseFollow() {
        long uid = getConfig().getLong("bili_uid");
        String actionToDo = getConfig().getString("follow.do-action");
        if (uid > 0) {
            getFollowersTask = Bukkit.getScheduler().runTaskTimerAsynchronously(this, new GetFollowersAndSpawnTask(uid, actionToDo), 20, 20 * 10);
        }
    }

    private void parseActivities() {
        Map<String, ?> map = getConfig().getConfigurationSection("danmu").getValues(false);
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
            int times = section.getInt("times", 2);
            String id = (String) entry.getKey();
            int cooldown = section.getInt("cooldown", 10);
            String keyword = section.getString("keyword");
            int maxDuration = section.getInt("max-duration");
            String actionToDo = section.getString("do-action");
            DanmuSummonActivity activity = new DanmuSummonActivity(id, displayName, keyword, times, maxDuration, actionToDo, cooldown);
            activities.put(keyword, activity);
        }
    }

    private void parseGifts() {
        Map<String, ?> map = getConfig().getConfigurationSection("gifts").getValues(false);
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (!(entry.getValue() instanceof ConfigurationSection)) {
                continue;
            }
            ConfigurationSection section = (ConfigurationSection) entry.getValue();
            String giftName = (String) entry.getKey();
            String msg = section.getString("msg");
            String actionToDo = section.getString("do-action");
            boolean multiply = section.getBoolean("multiply");
            Gift gift = new Gift(giftName, msg, actionToDo, multiply);
            gifts.put(giftName, gift);
        }
    }

    private void parseActions() {
        Map<String, ?> map = getConfig().getConfigurationSection("actions").getValues(false);
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (!(entry.getValue() instanceof List)) {
                continue;
            }
            String action = (String) entry.getKey();
            List<String> list = (List<String>) entry.getValue();
            List<Operation> operations = new ArrayList<>();
            list.forEach(x -> {
                String[] parts = x.split(":");
                if (parts.length > 0) {
                    Operation.Type type = Operation.Type.getTypeByName(parts[0]);
                    String data = null;
                    if (type != null) {
                        if (parts.length > 1) {
                            data = x.substring(x.indexOf(':') + 1);
                        }
                        Operation operation = new Operation(type, data);
                        operations.add(operation);
                    }
                }
            });
            actions.put(action, operations);
        }
    }

    @Override
    public void onDisable() {
        closeRoom();
        instance = null;
        if (getFollowersTask != null) {
            getFollowersTask.cancel();
            getFollowersTask = null;
        }
        activities = null;
        gifts = null;
        actions = null;
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
        parseActions();
        parseActivities();
        parseGifts();
        parseFollow();
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
                PluginPacketListener<CommandSender> listener = new PluginPacketListener<>();
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
