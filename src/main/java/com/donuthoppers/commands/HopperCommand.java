package com.donuthoppers.commands;

import com.donuthoppers.DonutHoppersPlugin;
import com.donuthoppers.config.ConfigManager;
import com.donuthoppers.locale.MessageManager;
import java.util.List;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public final class HopperCommand implements CommandExecutor {

    private final DonutHoppersPlugin plugin;
    private final ConfigManager configManager;
    private final MessageManager messageManager;

    public HopperCommand(DonutHoppersPlugin plugin, ConfigManager configManager, MessageManager messageManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.messageManager = messageManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            messageManager.send(sender, "unknown-subcommand");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload":
                if (!sender.hasPermission("hoppers.reload")) {
                    messageManager.send(sender, "no-permission");
                    return true;
                }
                configManager.reload();
                messageManager.reload();
                messageManager.send(sender, "reload-success");
                break;
            case "info":
                if (!sender.hasPermission("hoppers.info")) {
                    messageManager.send(sender, "no-permission");
                    return true;
                }
                List<String> lines = messageManager.getLines("info-lines",
                    "version", plugin.getDescription().getVersion(),
                    "multiplier", String.valueOf(configManager.getSpeedMultiplier()),
                    "limit", String.valueOf(configManager.getChunkPlacementLimit()),
                    "enabled", String.valueOf(configManager.isChunkLimitEnabled())
                );
                for (String line : lines) {
                    sender.sendMessage(line);
                }
                break;
            default:
                messageManager.send(sender, "unknown-subcommand");
                break;
        }

        return true;
    }
}
