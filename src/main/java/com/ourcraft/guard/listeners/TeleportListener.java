package com.ourcraft.guard.listeners;

import com.ourcraft.guard.OurcraftGuard;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerTeleportEvent;

/**
 * 传送事件监听：传送后重置玩家状态，避免把传送距离误判为加速。
 */
public class TeleportListener implements Listener {

    private final OurcraftGuard plugin;

    public TeleportListener(OurcraftGuard plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        Location to = event.getTo();
        if (player == null || to == null) return;

        // 忽略末影珍珠/紫颂果等造成的小位移（这些会被 AbilityListener 处理）
        PlayerTeleportEvent.TeleportCause cause = event.getCause();
        if (cause == PlayerTeleportEvent.TeleportCause.UNKNOWN) return;

        plugin.getMovementChecker().resetPlayerState(player, to);
    }
}
