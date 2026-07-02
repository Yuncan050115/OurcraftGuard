package com.ourcraft.guard.managers;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import com.ourcraft.guard.OurcraftGuard;
import com.ourcraft.guard.utils.SchedulerUtil;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;

/**
 * 每个服务器 tick 向所有在线玩家发送 PING 包（ClientboundPingPacket）。
 * 客户端必须立即回应 PONG，由此建立客户端无法加速的服务器锚定时钟。
 *
 * 使用 ProtocolLib 发送数据包（跨版本兼容），使用 SchedulerUtil 兼容 Folia。
 */
public class TransactionManager {

    private final OurcraftGuard plugin;
    private final ProtocolManager protocolManager;
    private Object task;

    public TransactionManager(OurcraftGuard plugin) {
        this.plugin = plugin;
        this.protocolManager = ProtocolLibrary.getProtocolManager();
    }

    public void start() {
        task = SchedulerUtil.runSyncTimer(plugin, () -> {
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                sendTransaction(player);
            }
        }, 1L, 1L);
    }

    public void stop() {
        SchedulerUtil.cancelTask(task);
        task = null;
    }

    private void sendTransaction(Player player) {
        GameMode gm = player.getGameMode();
        if (gm == GameMode.CREATIVE || gm == GameMode.SPECTATOR) return;

        long sendNano = System.nanoTime();
        int id = plugin.getMovementChecker()
                .registerOutgoingTransaction(player.getUniqueId(), sendNano);
        if (id == Integer.MIN_VALUE) return;

        try {
            PacketContainer ping = protocolManager.createPacket(PacketType.Play.Server.PING);
            ping.getIntegers().write(0, id);
            protocolManager.sendServerPacket(player, ping);
        } catch (Exception e) {
            if (plugin.isDebugEnabled()) {
                plugin.getLogger().warning("Failed to send ping to "
                        + player.getName() + ": " + e.getMessage());
            }
        }
    }
}
