package com.ourcraft.guard.listeners;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.reflect.StructureModifier;
import com.ourcraft.guard.OurcraftGuard;
import com.ourcraft.guard.utils.SchedulerUtil;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

import java.util.UUID;

/**
 * 使用 ProtocolLib 进行跨版本数据包监听。
 *
 * 监听：
 *   - POSITION / POSITION_LOOK  → 玩家移动
 *   - VEHICLE_MOVE              → 载具移动
 *   - PONG                      → 计时器时钟回应
 *
 * 所有监听运行在 Netty 线程上（ASYNC）。
 */
public class PacketListener implements Listener {

    private final OurcraftGuard plugin;
    private final ProtocolManager protocolManager;

    public PacketListener(OurcraftGuard plugin) {
        this.plugin = plugin;
        this.protocolManager = ProtocolLibrary.getProtocolManager();
    }

    public void inject() {
        if (protocolManager == null) {
            plugin.getLogger().severe("未找到 ProtocolLib！OurcraftGuard 无法工作。");
            plugin.getServer().getPluginManager().disablePlugin(plugin);
            return;
        }

        final OurcraftGuard pl = plugin;

        // PONG 包（计时器时钟回应）
        protocolManager.addPacketListener(new PacketAdapter(
                pl, ListenerPriority.LOWEST, PacketType.Play.Client.PONG) {
            @Override
            public void onPacketReceiving(PacketEvent event) {
                if (event.isCancelled()) return;
                try {
                    StructureModifier<Integer> ints = event.getPacket().getIntegers();
                    if (ints.size() == 0) return;
                    int id = ints.read(0);
                    if (pl.getMovementChecker().handlePong(
                            event.getPlayer().getUniqueId(), id, System.nanoTime())) {
                        event.setCancelled(true);
                    }
                } catch (Throwable ignored) {
                    // ProtocolLib 字段结构不兼容，跳过本包
                }
            }
        });

        // 玩家移动包（含位置）
        protocolManager.addPacketListener(new PacketAdapter(
                pl, ListenerPriority.LOWEST,
                PacketType.Play.Client.POSITION,
                PacketType.Play.Client.POSITION_LOOK) {
            @Override
            public void onPacketReceiving(PacketEvent event) {
                if (event.isCancelled()) return;
                Player player = event.getPlayer();
                if (player == null) return;
                try {
                    PacketContainer packet = event.getPacket();
                    StructureModifier<Double> doubles = packet.getDoubles();
                    if (doubles.size() < 3) return;
                    StructureModifier<Boolean> bools = packet.getBooleans();
                    boolean onGround = bools.size() > 0 && bools.read(0);

                    double x = doubles.read(0);
                    double y = doubles.read(1);
                    double z = doubles.read(2);

                    Location from = player.getLocation();
                    Location to = new Location(player.getWorld(), x, y, z);

                    if (from.distanceSquared(to) > 0.001) {
                        boolean allowed = pl.getMovementChecker().processMovement(
                                player, from, to, false, onGround);
                        if (!allowed) event.setCancelled(true);
                    }
                } catch (Throwable ignored) {
                    // ProtocolLib 字段结构不兼容，跳过本包
                }
            }
        });

        // 载具移动包
        protocolManager.addPacketListener(new PacketAdapter(
                pl, ListenerPriority.LOWEST,
                PacketType.Play.Client.VEHICLE_MOVE) {
            @Override
            public void onPacketReceiving(PacketEvent event) {
                if (event.isCancelled()) return;
                Player player = event.getPlayer();
                if (player == null) return;
                Entity vehicle = player.getVehicle();
                if (vehicle == null) return;
                try {
                    PacketContainer packet = event.getPacket();
                    StructureModifier<Double> doubles = packet.getDoubles();
                    if (doubles.size() < 3) return;

                    double packetX = doubles.read(0);
                    double packetY = doubles.read(1);
                    double packetZ = doubles.read(2);

                    Location vLoc = vehicle.getLocation();
                    double dx = packetX - vLoc.getX();
                    double dy = packetY - vLoc.getY();
                    double dz = packetZ - vLoc.getZ();

                    if (dx * dx + dy * dy + dz * dz > 0.001) {
                        Location playerFrom = player.getLocation();
                        Location playerTo = playerFrom.clone().add(dx, dy, dz);
                        boolean allowed = pl.getMovementChecker().processMovement(
                                player, playerFrom, playerTo, true, false);
                        if (!allowed) event.setCancelled(true);
                    }
                } catch (Throwable ignored) {
                    // ProtocolLib 字段结构不兼容，跳过本包
                }
            }
        });

        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            plugin.getMovementChecker().registerPlayer(player);
        }
        plugin.getLogger().info("ProtocolLib 数据包监听器已注册。");
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        SchedulerUtil.runSync(plugin, () -> plugin.getMovementChecker().registerPlayer(player));
        if (plugin.isDebugEnabled()) {
            plugin.getLogger().info("OurcraftGuard now tracking " + player.getName());
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        plugin.getMovementChecker().unregisterPlayer(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        plugin.getMovementChecker().resetPlayerState(
                event.getPlayer(), event.getRespawnLocation());
    }

    public void uninject() {
        // ProtocolLib 会自动在插件卸载时移除监听器
    }
}
