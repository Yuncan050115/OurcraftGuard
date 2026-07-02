package com.ourcraft.guard.utils;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

import java.util.Set;
import java.util.logging.Logger;

/**
 * 移动辅助工具集（基于 VelocityGuard 3.3 原版逻辑改造）。
 *
 * 多版本兼容：所有 EntityType 引用都通过 name() 字符串比较，
 * 避免直接引用较新版本才有的枚举常量。
 */
public final class MovementUtils {

    private MovementUtils() {}

    private static final Set<Material> PASSABLE = Set.of(
            Material.AIR, Material.CAVE_AIR, Material.VOID_AIR,
            Material.WATER, Material.LAVA);

    private static final Set<Material> ICE_TYPES = Set.of(
            Material.ICE, Material.PACKED_ICE, Material.BLUE_ICE, Material.FROSTED_ICE);

    private static final Set<Material> CLIMBABLE = Set.of(
            Material.LADDER, Material.VINE, Material.SCAFFOLDING,
            Material.WEEPING_VINES, Material.WEEPING_VINES_PLANT,
            Material.TWISTING_VINES, Material.TWISTING_VINES_PLANT,
            Material.CAVE_VINES, Material.CAVE_VINES_PLANT);

    private static final Set<Material> SOUL_BLOCKS = Set.of(
            Material.SOUL_SAND, Material.SOUL_SOIL);

    public static boolean isNearGround(Player player) {
        return isNearGroundAt(player.getLocation());
    }

    public static boolean isNearGroundAt(Location loc) {
        for (double x = -0.3; x <= 0.3; x += 0.3) {
            for (double z = -0.3; z <= 0.3; z += 0.3) {
                Block b = loc.clone().add(x, -0.5, z).getBlock();
                if (!PASSABLE.contains(b.getType())) return true;
            }
        }
        return false;
    }

    public static boolean isInLiquid(Player player) {
        if (player.isSwimming()) return true;
        Location loc = player.getLocation();
        if (isLiquid(loc.getBlock())) return true;
        if (isLiquid(loc.clone().subtract(0, 0.1, 0).getBlock())) return true;
        if (isLiquid(player.getEyeLocation().getBlock())) return true;
        if (isLiquid(loc.clone().subtract(0, 0.5, 0).getBlock())) return true;
        for (double x = -0.3; x <= 0.3; x += 0.3) {
            for (double z = -0.3; z <= 0.3; z += 0.3) {
                if (isLiquid(loc.clone().add(x, -0.2, z).getBlock())) return true;
            }
        }
        return false;
    }

    private static boolean isLiquid(Block b) {
        return b.getType() == Material.WATER || b.getType() == Material.LAVA;
    }

    public static boolean isOnIce(Player player) {
        Block b = player.getLocation().clone().subtract(0, 0.2, 0).getBlock();
        return ICE_TYPES.contains(b.getType());
    }

    public static boolean isOnSoulBlock(Player player) {
        Block b = player.getLocation().clone().subtract(0, 0.2, 0).getBlock();
        return SOUL_BLOCKS.contains(b.getType());
    }

    public static boolean isClimbing(Player player) {
        Location loc = player.getLocation();
        if (CLIMBABLE.contains(loc.getBlock().getType())) return true;
        return CLIMBABLE.contains(player.getEyeLocation().getBlock().getType());
    }

    public static boolean isInBubbleColumn(Player player) {
        Location loc = player.getLocation();
        if (loc.getBlock().getType() == Material.BUBBLE_COLUMN) return true;
        return loc.clone().add(0, 1, 0).getBlock().getType() == Material.BUBBLE_COLUMN;
    }

    public static boolean isNearSlime(Player player) {
        Location loc = player.getLocation();
        for (double y = -0.1; y >= -1.5; y -= 0.5) {
            if (loc.clone().add(0, y, 0).getBlock().getType() == Material.SLIME_BLOCK) {
                return true;
            }
        }
        return false;
    }

    /** 判断玩家是否骑乘快乐恶魂（1.21.5+）。通过 name() 比较实现多版本兼容。 */
    public static boolean isRidingGhast(Player player) {
        if (!player.isInsideVehicle() || player.getVehicle() == null) return false;
        Entity vehicle = player.getVehicle();
        return "HAPPY_GHAST".equals(vehicle.getType().name());
    }

    public record FlightResult(int newAirTicks, boolean violation) {}

    public static FlightResult checkFlying(Player player, Location from, Location to,
                                           int currentAirTicks, boolean debugEnabled,
                                           Logger logger, int violationThreshold) {
        if (player.hasPotionEffect(PotionEffectType.LEVITATION)) {
            return new FlightResult(currentAirTicks, false);
        }
        if (isNearGround(player) || isInLiquid(player)
                || isClimbing(player) || isInBubbleColumn(player)) {
            return new FlightResult(0, false);
        }

        int ticks = currentAirTicks + 1;
        int hoverStart  = Math.max(15, Math.min(25, violationThreshold - 5));
        int ascendStart = Math.max(15, Math.min(30, violationThreshold));

        if (ticks > hoverStart && !player.isGliding() && !player.isFlying()) {
            if (Math.abs(to.getY() - from.getY()) < 0.05) {
                if (debugEnabled) logger.info(player.getName()
                        + " potential hover - airTicks=" + ticks);
                return new FlightResult(ticks, ticks >= violationThreshold);
            }
            if (to.getY() > from.getY() && ticks >= ascendStart) {
                if (debugEnabled) logger.info(player.getName()
                        + " ascending in air - airTicks=" + ticks);
                return new FlightResult(ticks, ticks >= violationThreshold);
            }
        }
        return new FlightResult(ticks, false);
    }
}
