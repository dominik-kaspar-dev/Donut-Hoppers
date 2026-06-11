package com.donuthoppers.listeners;

import com.donuthoppers.config.ConfigManager;
import com.donuthoppers.locale.MessageManager;
import com.donuthoppers.manager.HopperManager;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

public final class ChunkLimitListener implements Listener {

    private final ConfigManager configManager;
    private final MessageManager messageManager;
    private final HopperManager hopperManager;

    public ChunkLimitListener(ConfigManager configManager, MessageManager messageManager, HopperManager hopperManager) {
        this.configManager = configManager;
        this.messageManager = messageManager;
        this.hopperManager = hopperManager;
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Block block = event.getBlockPlaced();
        if (block.getType() != Material.HOPPER) {
            return;
        }

        if (configManager.isChunkLimitEnabled()) {
            Chunk chunk = block.getChunk();
            int currentCount = hopperManager.getHopperCount(chunk);
            if (currentCount >= configManager.getChunkPlacementLimit()) {
                event.setCancelled(true);
                if (event.getPlayer() instanceof Player) {
                    messageManager.send(event.getPlayer(), "chunk-limit-denied", "limit", String.valueOf(configManager.getChunkPlacementLimit()));
                }
                return;
            }
        }

        hopperManager.addHopper(block);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (block.getType() != Material.HOPPER) {
            return;
        }
        hopperManager.removeHopper(block);
    }
}
