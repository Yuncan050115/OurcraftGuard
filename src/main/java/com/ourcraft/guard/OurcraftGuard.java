package com.ourcraft.guard;

import com.ourcraft.guard.api.OurcraftGuardAPI;
import com.ourcraft.guard.commands.GuardCommand;
import com.ourcraft.guard.config.ConfigManager;
import com.ourcraft.guard.listeners.DamageListener;
import com.ourcraft.guard.listeners.PacketListener;
import com.ourcraft.guard.listeners.TeleportListener;
import com.ourcraft.guard.listeners.TridentListener;
import com.ourcraft.guard.managers.TransactionManager;
import com.ourcraft.guard.processors.MovementChecker;
import com.ourcraft.guard.utils.SchedulerUtil;
import org.bstats.bukkit.Metrics;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * OurcraftGuard 主类。
 *
 * 兼容 Paper 与 Folia（调度全部走 SchedulerUtil，不直接调用 Bukkit.getScheduler().runTask）。
 * 使用 ProtocolLib 进行跨版本数据包监听，不再依赖 NMS。
 *
 * 反作弊核心来自 VelocityGuard 3.3（作者 AlphaAlex115），由 Yuncan 改造为：
 *   1. 多版本适配（ProtocolLib 替换 NMS 通道注入）
 *   2. Folia 兼容（调度封装 + 实体区域线程）
 *   3. 分组 bypass 权限（ourcraftguard.bypass[.speed|.flight|.timer]）
 *   4. 宽松配置（leniency=10），只防 XZ 轴水平高速移动，原版附魔位移不误判
 *   5. bStats 集成（pluginId 32310，Shadow 重定位）
 *   6. ASCII 启动横幅
 */
public final class OurcraftGuard extends JavaPlugin {

    // bStats 插件 ID
    private static final int BSTATS_PLUGIN_ID = 32310;

    private ConfigManager configManager;
    private MovementChecker movementChecker;
    private OurcraftGuardAPI api;
    private PacketListener packetListener;
    private DamageListener damageListener;
    private TeleportListener teleportListener;
    private TridentListener tridentListener;
    private TransactionManager transactionManager;
    private Metrics metrics;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.configManager = new ConfigManager(this);

        this.movementChecker = new MovementChecker(this);
        this.api = new OurcraftGuardAPI(this);

        // ProtocolLib 数据包监听器
        this.packetListener = new PacketListener(this);
        this.packetListener.inject();

        this.damageListener     = new DamageListener(this);
        this.teleportListener   = new TeleportListener(this);
        this.tridentListener    = new TridentListener(this);

        this.transactionManager = new TransactionManager(this);
        this.transactionManager.start();

        // 注册事件监听器
        getServer().getPluginManager().registerEvents(damageListener, this);
        getServer().getPluginManager().registerEvents(teleportListener, this);
        getServer().getPluginManager().registerEvents(tridentListener, this);
        getServer().getPluginManager().registerEvents(packetListener, this);

        // 注册命令
        GuardCommand command = new GuardCommand(this);
        getCommand("ourcraftguard").setExecutor(command);
        getCommand("ourcraftguard").setTabCompleter(command);

        // bStats 集成
        try {
            this.metrics = new Metrics(this, BSTATS_PLUGIN_ID);
        } catch (Throwable e) {
            getLogger().warning("bStats 初始化失败（不影响插件运行）: " + e.getMessage());
        }

        // ASCII 横幅
        printBanner();
        getLogger().info("OurcraftGuard 已启用 (Folia=" + SchedulerUtil.isFolia()
                + ", bStats=" + (metrics != null) + ")");
    }

    @Override
    public void onDisable() {
        if (transactionManager != null) {
            transactionManager.stop();
        }
        if (packetListener != null) {
            packetListener.uninject();
        }
        SchedulerUtil.cancelAll(this);
        getLogger().info("OurcraftGuard 已禁用。");
    }

    public ConfigManager getConfigManager() { return configManager; }
    public MovementChecker getMovementChecker() { return movementChecker; }
    public boolean isDebugEnabled() {
        return configManager != null && configManager.isDebugModeEnabled();
    }
    public OurcraftGuardAPI getAPI() { return api; }

    public void reloadConfigManager() {
        reloadConfig();
        this.configManager = new ConfigManager(this);
        this.movementChecker = new MovementChecker(this);
        this.api = new OurcraftGuardAPI(this);
    }

    // ==================== Bypass 权限检查 ====================

    public boolean isBypassAll(Player p) {
        return p.hasPermission("ourcraftguard.bypass");
    }
    public boolean isBypassSpeed(Player p) {
        return isBypassAll(p) || p.hasPermission("ourcraftguard.bypass.speed");
    }
    public boolean isBypassFlight(Player p) {
        return isBypassAll(p) || p.hasPermission("ourcraftguard.bypass.flight");
    }
    public boolean isBypassTimer(Player p) {
        return isBypassAll(p) || p.hasPermission("ourcraftguard.bypass.timer");
    }

    // ==================== ASCII 横幅 ====================

    private void printBanner() {
        String[] banner = {
                "██╗   ██╗██╗   ██╗███╗   ██╗ ██████╗ █████╗ ███╗   ██╗",
                "╚██╗ ██╔╝██║   ██║████╗  ██║██╔════╝██╔══██╗████╗  ██║",
                " ╚████╔╝ ██║   ██║██╔██╗ ██║██║     ███████║██╔██╗ ██║",
                "  ╚██╔╝  ██║   ██║██║╚██╗██║██║     ██╔══██║██║╚██╗██║",
                "   ██║   ╚██████╔╝██║ ╚████║╚██████╗██║  ██║██║ ╚████║",
                "   ╚═╝    ╚═════╝ ╚═╝  ╚═══╝ ╚═════╝╚═╝  ╚═╝╚═╝  ╚═══╝",
                "        ▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓",
                "      ▓▓██ Yuncan-Ourcraft服务器出品 ████▓▓",
                "    ▓▓██                      ████▓▓",
                "      ▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀",
                "[OurcraftGuard] v" + getPluginMeta().getVersion()
                        + " - Ourcraft Yuncan 出品",
                "[OurcraftGuard] Author: Yuncan | https://github.com/Yuncan050115"
        };
        for (String line : banner) {
            getLogger().info(line);
        }
    }
}
