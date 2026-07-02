package com.ourcraft.guard.listeners;

import com.ourcraft.guard.OurcraftGuard;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRiptideEvent;

/**
 * 激流三叉戟事件监听：触发后记录 riptide 时间，让 knockback/riptide 速度容差模型感知。
 */
public class TridentListener implements Listener {

    private final OurcraftGuard plugin;

    public TridentListener(OurcraftGuard plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerRiptide(PlayerRiptideEvent event) {
        Player player = event.getPlayer();
        plugin.getMovementChecker().recordRiptideUse(player);
    }
}
