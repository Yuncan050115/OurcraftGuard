package com.ourcraft.guard.config;

import com.ourcraft.guard.OurcraftGuard;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * 配置管理器。一次性读取所有配置项，避免热路径中频繁访问 FileConfiguration。
 */
public class ConfigManager {

    private final boolean debugMode;

    private final double perTickTolerance;
    private final double violationThreshold;
    private final double violationDecay;
    private final double knockbackMultiplier;
    private final int    knockbackDuration;
    private final double riptideMultiplier;
    private final int    riptideDuration;
    private final int    elytraLandingDuration;
    private final double vehicleSpeedMultiplier;
    private final double vehicleIceSpeedMultiplier;
    private final double leniencyMultiplier;
    private final boolean flightCheckEnabled;
    private final boolean timerCheckEnabled;
    private final long    timerDriftNanos;
    private final int     timerMaxViolations;

    public ConfigManager(OurcraftGuard plugin) {
        plugin.saveDefaultConfig();
        FileConfiguration c = plugin.getConfig();

        debugMode = c.getBoolean("settings.debug-mode", false);

        perTickTolerance      = Math.max(0.0,  c.getDouble("checks.speed.per-tick-tolerance", 0.5));
        violationThreshold    = Math.max(0.1,  c.getDouble("checks.speed.violation-threshold", 5.0));
        violationDecay        = Math.max(0.01, c.getDouble("checks.speed.violation-decay", 0.5));
        knockbackMultiplier   = Math.max(0.5,  c.getDouble("checks.speed.knockback.multiplier", 6.0));
        knockbackDuration     = Math.max(200,  c.getInt   ("checks.speed.knockback.duration", 1000));
        riptideMultiplier     = Math.max(1.0,  c.getDouble("checks.speed.riptide.multiplier", 2.5));
        riptideDuration       = Math.max(500,  c.getInt   ("checks.speed.riptide.duration", 3000));
        elytraLandingDuration = Math.max(500,  c.getInt   ("checks.speed.elytra.landing-duration", 1500));
        vehicleSpeedMultiplier    = Math.max(1.0, c.getDouble("checks.speed.vehicle-speed-multiplier", 1.5));
        vehicleIceSpeedMultiplier = Math.max(1.0, c.getDouble("checks.speed.vehicle-ice-speed-multiplier", 4.3));
        leniencyMultiplier        = Math.max(1.0, c.getDouble("checks.leniency-multiplier", 10.0));
        flightCheckEnabled        = c.getBoolean("checks.flight.enabled", false);
        timerCheckEnabled         = c.getBoolean("checks.timer.enabled", false);
        timerDriftNanos           = Math.max(0, c.getInt("checks.timer.drift-millis", 120)) * 1_000_000L;
        timerMaxViolations        = Math.max(1, c.getInt("checks.timer.max-violations", 5));
    }

    public boolean isDebugModeEnabled()        { return debugMode; }
    public double getPerTickTolerance()        { return perTickTolerance; }
    public double getViolationThreshold()      { return violationThreshold; }
    public double getViolationDecay()          { return violationDecay; }
    public double getKnockbackMultiplier()     { return knockbackMultiplier; }
    public int    getKnockbackDuration()       { return knockbackDuration; }
    public double getRiptideMultiplier()       { return riptideMultiplier; }
    public int    getRiptideDuration()         { return riptideDuration; }
    public int    getElytraLandingDuration()   { return elytraLandingDuration; }
    public double getVehicleSpeedMultiplier()  { return vehicleSpeedMultiplier; }
    public double getVehicleIceSpeedMultiplier(){ return vehicleIceSpeedMultiplier; }
    public double getLeniencyMultiplier()      { return leniencyMultiplier; }
    public boolean isFlightCheckEnabled()      { return flightCheckEnabled; }
    public boolean isTimerCheckEnabled()       { return timerCheckEnabled; }
    public long    getTimerDriftNanos()        { return timerDriftNanos; }
    public int     getTimerMaxViolations()     { return timerMaxViolations; }
}
