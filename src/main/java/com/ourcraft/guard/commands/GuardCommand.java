package com.ourcraft.guard.commands;

import com.ourcraft.guard.OurcraftGuard;
import com.ourcraft.guard.utils.SchedulerUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GuardCommand implements CommandExecutor, TabCompleter {

    private final OurcraftGuard plugin;

    public GuardCommand(OurcraftGuard plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("ourcraftguard.admin")) {
            sender.sendMessage("§c[OurcraftGuard] §f你没有权限使用此命令。");
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender, label);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload" -> {
                SchedulerUtil.runSync(plugin, () -> {
                    plugin.reloadConfigManager();
                    sender.sendMessage("§a[OurcraftGuard] §f配置已重载。");
                });
            }
            case "status" -> {
                sender.sendMessage("§6[OurcraftGuard] §f状态:");
                sender.sendMessage("§7 Folia: §e" + SchedulerUtil.isFolia());
                sender.sendMessage("§7 Debug: §e" + plugin.isDebugEnabled());
                sender.sendMessage("§7 Timer: §e" + plugin.getConfigManager().isTimerCheckEnabled());
                sender.sendMessage("§7 Flight: §e" + plugin.getConfigManager().isFlightCheckEnabled());
                sender.sendMessage("§7 宽松倍率: §e" + plugin.getConfigManager().getLeniencyMultiplier());
                if (sender instanceof Player player) {
                    sender.sendMessage("§7 你的 bypass (all): §e" + plugin.isBypassAll(player));
                    sender.sendMessage("§7 你的 bypass (speed): §e" + plugin.isBypassSpeed(player));
                    sender.sendMessage("§7 你的 bypass (flight): §e" + plugin.isBypassFlight(player));
                    sender.sendMessage("§7 你的 bypass (timer): §e" + plugin.isBypassTimer(player));
                }
            }
            case "help" -> sendHelp(sender, label);
            default -> sender.sendMessage("§c[OurcraftGuard] §f未知命令。使用 /" + label + " help 查看帮助。");
        }
        return true;
    }

    private void sendHelp(CommandSender sender, String label) {
        sender.sendMessage("§6[OurcraftGuard] §f命令:");
        sender.sendMessage("§e/" + label + " reload §7- 重载配置");
        sender.sendMessage("§e/" + label + " status §7- 查看状态和你的 bypass 权限");
        sender.sendMessage("§e/" + label + " help   §7- 显示此帮助");
        sender.sendMessage("§7Bypass 节点: ourcraftguard.bypass[.speed|.flight|.timer]");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> opts = Arrays.asList("reload", "status", "help");
            List<String> result = new ArrayList<>();
            for (String opt : opts) {
                if (opt.startsWith(args[0].toLowerCase())) result.add(opt);
            }
            return result;
        }
        return new ArrayList<>();
    }
}
