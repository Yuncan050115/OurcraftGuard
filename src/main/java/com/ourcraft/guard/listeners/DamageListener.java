package com.ourcraft.guard.listeners;

import com.ourcraft.guard.OurcraftGuard;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

/**
 * 伤害事件监听：记录玩家被击中的时间，用于给击退/龙击退留出位移容差窗口。
 * 爆炸/弹射物造成的位移通过 knockback 模型自动容纳，不需要免检窗口。
 */
public class DamageListener implements Listener {

    private final OurcraftGuard plugin;

    public DamageListener(OurcraftGuard plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        boolean isDragon = event.getDamager() instanceof EnderDragon;
        plugin.getMovementChecker().recordPlayerDamage(player, isDragon);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        // 爆炸/火焰/闪电/龙击退等非实体伤害来源，记录伤害让 knockback 模型感知
        if (event.getCause() == EntityDamageEvent.DamageCause.ENTITY_EXPLOSION
                || event.getCause() == EntityDamageEvent.DamageCause.BLOCK_EXPLOSION
                || event.getCause() == EntityDamageEvent.DamageCause.LIGHTNING
                || event.getCause() == EntityDamageEvent.DamageCause.FALLING_BLOCK) {
            plugin.getMovementChecker().recordPlayerDamage(player, false);
        }
    }
}
