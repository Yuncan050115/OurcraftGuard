package com.ourcraft.guard.utils;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

import java.util.concurrent.TimeUnit;

/**
 * Folia/Paper 统一调度器抽象层（参考 ZMusicGUI 的 SchedulerUtil 设计）。
 *
 * 自动检测服务器是否为 Folia，并提供统一的调度接口：
 *   - Folia: 使用 RegionScheduler / GlobalRegionScheduler / AsyncScheduler / EntityScheduler
 *   - Paper/Bukkit: 使用 BukkitScheduler
 *
 * 一套代码兼容两种平台。
 */
public final class SchedulerUtil {

    private SchedulerUtil() {}

    private static final boolean FOLIA;

    static {
        boolean detected;
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            detected = true;
        } catch (ClassNotFoundException e) {
            detected = false;
        }
        FOLIA = detected;
    }

    public static boolean isFolia() { return FOLIA; }

    // ==================== 同步任务（主线程 / 全局区域线程） ====================

    public static void runSync(Plugin plugin, Runnable runnable) {
        if (FOLIA) {
            try {
                Bukkit.getGlobalRegionScheduler().run(plugin, t -> runnable.run());
            } catch (Throwable e) {
                Bukkit.getScheduler().runTask(plugin, runnable);
            }
        } else {
            Bukkit.getScheduler().runTask(plugin, runnable);
        }
    }

    public static void runSyncLater(Plugin plugin, Runnable runnable, long delayTicks) {
        if (FOLIA) {
            try {
                Bukkit.getGlobalRegionScheduler().runDelayed(plugin, t -> runnable.run(), delayTicks);
            } catch (Throwable e) {
                Bukkit.getScheduler().runTaskLater(plugin, runnable, delayTicks);
            }
        } else {
            Bukkit.getScheduler().runTaskLater(plugin, runnable, delayTicks);
        }
    }

    public static void runAtLocation(Plugin plugin, Location loc, Runnable runnable) {
        if (FOLIA) {
            try {
                Bukkit.getRegionScheduler().run(plugin, loc, t -> runnable.run());
            } catch (Throwable e) {
                Bukkit.getScheduler().runTask(plugin, runnable);
            }
        } else {
            Bukkit.getScheduler().runTask(plugin, runnable);
        }
    }

    /**
     * 在实体所在区域线程执行（用于安全访问玩家状态/传送玩家）。
     * Folia 使用 EntityScheduler；Paper 回退到主线程。
     */
    public static void runEntity(Plugin plugin, Entity entity, Runnable runnable) {
        if (FOLIA) {
            try {
                entity.getScheduler().execute(plugin, runnable, null, 0L);
            } catch (Throwable e) {
                runSync(plugin, runnable);
            }
        } else {
            Bukkit.getScheduler().runTask(plugin, runnable);
        }
    }

    // ==================== 异步任务 ====================

    public static void runAsync(Plugin plugin, Runnable runnable) {
        if (FOLIA) {
            try {
                Bukkit.getAsyncScheduler().runNow(plugin, t -> runnable.run());
            } catch (Throwable e) {
                Bukkit.getScheduler().runTaskAsynchronously(plugin, runnable);
            }
        } else {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, runnable);
        }
    }

    // ==================== 定时任务 ====================

    /**
     * 固定速率重复执行同步任务（按 tick）。
     * 返回任务对象（Folia ScheduledTask 或 Paper BukkitTask），用 cancelTask 取消。
     */
    public static Object runSyncTimer(Plugin plugin, Runnable runnable, long delayTicks, long periodTicks) {
        if (FOLIA) {
            try {
                return Bukkit.getGlobalRegionScheduler().runAtFixedRate(
                        plugin, t -> runnable.run(), delayTicks, periodTicks);
            } catch (Throwable e) {
                return Bukkit.getScheduler().runTaskTimer(plugin, runnable, delayTicks, periodTicks);
            }
        }
        return Bukkit.getScheduler().runTaskTimer(plugin, runnable, delayTicks, periodTicks);
    }

    public static Object runAsyncTimer(Plugin plugin, Runnable runnable, long delayTicks, long periodTicks) {
        if (FOLIA) {
            try {
                long delayMs  = delayTicks * 50L;
                long periodMs = periodTicks * 50L;
                return Bukkit.getAsyncScheduler().runAtFixedRate(
                        plugin, t -> runnable.run(), delayMs, periodMs, TimeUnit.MILLISECONDS);
            } catch (Throwable e) {
                return Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, runnable, delayTicks, periodTicks);
            }
        }
        return Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, runnable, delayTicks, periodTicks);
    }

    public static void cancelTask(Object task) {
        if (task == null) return;
        try {
            task.getClass().getMethod("cancel").invoke(task);
        } catch (Throwable ignored) {}
    }

    public static void cancelAll(Plugin plugin) {
        if (FOLIA) {
            try {
                Bukkit.getAsyncScheduler().cancelTasks(plugin);
                Bukkit.getGlobalRegionScheduler().cancelTasks(plugin);
            } catch (Throwable ignored) {}
        } else {
            Bukkit.getScheduler().cancelTasks(plugin);
        }
    }
}
