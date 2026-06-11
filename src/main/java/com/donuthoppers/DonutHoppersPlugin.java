package com.donuthoppers;

import com.donuthoppers.config.ConfigManager;
import com.donuthoppers.locale.MessageManager;
import com.donuthoppers.manager.HopperManager;
import com.donuthoppers.listeners.ChunkLimitListener;
import com.donuthoppers.commands.HopperCommand;
import org.bstats.bukkit.Metrics;
import org.bukkit.plugin.java.JavaPlugin;

public final class DonutHoppersPlugin extends JavaPlugin {

    private ConfigManager configManager;
    private MessageManager messageManager;
    private HopperManager hopperManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveResource("messages.yml", false);

        this.configManager = new ConfigManager(this);
        this.messageManager = new MessageManager(this);
        this.hopperManager = new HopperManager(this, configManager);

        getServer().getPluginManager().registerEvents(new ChunkLimitListener(configManager, messageManager, hopperManager), this);
        getCommand("hoppers").setExecutor(new HopperCommand(this, configManager, messageManager));

        new Metrics(this, 31919);
        hopperManager.initialize();
        getLogger().info("Donut Hoppers enabled with multiplier " + configManager.getSpeedMultiplier() + "x.");
    }

    @Override
    public void onDisable() {
        if (hopperManager != null) {
            hopperManager.shutdown();
        }
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public MessageManager getMessageManager() {
        return messageManager;
    }
}
