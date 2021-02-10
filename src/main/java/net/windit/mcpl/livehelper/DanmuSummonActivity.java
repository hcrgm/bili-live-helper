package net.windit.mcpl.livehelper;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

import java.util.List;

public class DanmuSummonActivity {
    private final int targetTimes;
    private final String id;
    private final String name;
    private final int cooldown;
    private final int maxDuration;
    private final String actionToDo;
    private final String keyword;
    private int times = 0;
    private volatile boolean running;
    private volatile long lastFinishTime;

    public DanmuSummonActivity(String id, String name, String keyword, int targetTimes, int maxDuration, String actionToDo, int cooldown) {
        this.id = id;
        this.name = name;
        this.keyword = keyword;
        this.targetTimes = targetTimes;
        this.cooldown = cooldown;
        this.maxDuration = maxDuration;
        this.actionToDo = actionToDo;
        Bukkit.getScheduler().runTaskLaterAsynchronously(LiveHelper.getInstance(), () -> {
            if (running) {
                reset();
                String notice = LiveHelper.messagePrefix + ChatColor.translateAlternateColorCodes('&', LiveHelper.getInstance().getConfig().getString("danmu.timeout-msg-format")
                        .replaceAll("\\{活动名}", name));
                Bukkit.broadcastMessage(notice);
            }
        }, 20L * maxDuration);
    }

    public boolean isFinished() {
        return times >= targetTimes;
    }

    public boolean isRunning() {
        return running;
    }

    public void reset() {
        this.times = 0;
        running = false;
        lastFinishTime = System.currentTimeMillis();
    }

    public long getLastFinishTime() {
        return lastFinishTime;
    }

    public void increaseTimes(String nickname) {
        if ((System.currentTimeMillis() - getLastFinishTime()) / 1000.0 < cooldown) {
            return;
        }
        running = true;
        if (times++ == 0) {
            Bukkit.broadcastMessage(LiveHelper.messagePrefix + ChatColor.translateAlternateColorCodes('&', LiveHelper.getInstance().getConfig().getString("danmu.start-msg-format")
                    .replaceAll("\\{昵称}", nickname)
                    .replaceAll("\\{限时}", Utils.humanTime(maxDuration))
                    .replaceAll("\\{活动名}", name)));
        } else if (times + 1 <= targetTimes) {
            Bukkit.broadcastMessage(LiveHelper.messagePrefix + ChatColor.translateAlternateColorCodes('&', LiveHelper.getInstance().getConfig().getString("danmu.running-msg-format")
                    .replaceAll("\\{昵称}", nickname)
                    .replaceAll("\\{活动名}", name)
                    .replaceAll("\\{剩余次数}", String.valueOf(targetTimes - times))));
        }
        if (isFinished()) {
            reset();
            String notice = LiveHelper.messagePrefix + ChatColor.translateAlternateColorCodes('&', LiveHelper.getInstance().getConfig().getString("danmu.end-msg-format")
                    .replaceAll("\\{昵称}", nickname)
                    .replaceAll("\\{活动名}", name));
            List<Operation> operations = LiveHelper.getOperations(actionToDo);
            Bukkit.getScheduler().runTask(LiveHelper.getInstance(), () -> {
                if (operations != null) {
                    operations.forEach(Operation::doIt);
                    Bukkit.broadcastMessage(notice);
                }
            });
        }
    }

    @Override
    public String toString() {
        return "DanmuSummonActivity{" +
                "targetTimes=" + targetTimes +
                ", id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", cooldown=" + cooldown +
                ", maxDuration=" + maxDuration +
                ", actionToDo='" + actionToDo + '\'' +
                ", keyword='" + keyword + '\'' +
                ", times=" + times +
                ", running=" + running +
                ", lastFinishTime=" + lastFinishTime +
                '}';
    }
}
