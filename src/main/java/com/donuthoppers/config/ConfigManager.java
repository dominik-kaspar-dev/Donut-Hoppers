package com.donuthoppers.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public final class ConfigManager {

    private final JavaPlugin plugin;
    private double hopperSpeedMultiplier;
    private boolean chunkLimitEnabled;
    private int chunkPlacementLimit;
    private int hopperProcessIntervalTicks;

    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
        loadConfiguration();
    }

    public void reload() {
        plugin.reloadConfig();
        loadConfiguration();
    }

    private void loadConfiguration() {
        FileConfiguration config = plugin.getConfig();
        this.hopperSpeedMultiplier = Math.max(1.0, config.getDouble("hopper-speed-multiplier", 12.0));
        this.chunkLimitEnabled = config.getBoolean("chunk-limit-enabled", true);
        this.chunkPlacementLimit = Math.max(0, config.getInt("chunk-placement-limit", 64));
        this.hopperProcessIntervalTicks = Math.max(1, config.getInt("hopper-process-interval-ticks", 1));
    }

    public double getSpeedMultiplier() {
        return hopperSpeedMultiplier;
    }

    public boolean isChunkLimitEnabled() {
        return chunkLimitEnabled;
    }

    public int getChunkPlacementLimit() {
        return chunkPlacementLimit;
    }

    public int getProcessIntervalTicks() {
        return hopperProcessIntervalTicks;
    }
}
