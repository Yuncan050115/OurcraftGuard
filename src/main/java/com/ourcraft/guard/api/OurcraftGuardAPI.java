package com.ourcraft.guard.api;

import com.ourcraft.guard.OurcraftGuard;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * 公共 API，供其他插件调用。
 */
public class OurcraftGuardAPI {

    private final OurcraftGuard plugin;

    public OurcraftGuardAPI(OurcraftGuard plugin) {
        this.plugin = plugin;
    }

    /** 给玩家添加飞行强制检查（用于反飞挂额外约束） */
    public void addFlightEnforcement(UUID playerId, boolean groundOnViolation,
                                     int airTickThreshold, boolean groundWhenStationary) {
        plugin.getMovementChecker().addFlightEnforcement(
                playerId, groundOnViolation, airTickThreshold, groundWhenStationary);
    }

    public void removeFlightEnforcement(UUID playerId) {
        plugin.getMovementChecker().removeFlightEnforcement(playerId);
    }

    public boolean isFlightEnforced(UUID playerId) {
        return plugin.getMovementChecker().isFlightEnforced(playerId);
    }

    public boolean isBypassAll(Player player) {
        return plugin.isBypassAll(player);
    }
}
