package com.donuthoppers.manager;

import com.donuthoppers.config.ConfigManager;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Hopper;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.inventory.FurnaceInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.CrafterInventory;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

public final class HopperManager implements Listener {

    private final Plugin plugin;
    private final ConfigManager configManager;
    private final Set<Location> trackedHoppers = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final Map<Chunk, Integer> chunkHopperCounts = new ConcurrentHashMap<>();
    private BukkitTask task;

    public HopperManager(Plugin plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
    }

    public void initialize() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        loadExistingHoppers();
        scheduleProcessingTask();
    }

    public void shutdown() {
        if (task != null) {
            task.cancel();
        }
        trackedHoppers.clear();
        chunkHopperCounts.clear();
    }

    public void addHopper(Block block) {
        if (block.getType() != Material.HOPPER) {
            return;
        }
        Location location = block.getLocation();
        if (trackedHoppers.add(location)) {
            chunkHopperCounts.merge(block.getChunk(), 1, Integer::sum);
        }
    }

    public void removeHopper(Block block) {
        if (block.getType() != Material.HOPPER) {
            return;
        }
        Location location = block.getLocation();
        if (trackedHoppers.remove(location)) {
            chunkHopperCounts.computeIfPresent(block.getChunk(), (chunk, count) -> count > 1 ? count - 1 : null);
        }
    }

    public int getHopperCount(Chunk chunk) {
        return chunkHopperCounts.getOrDefault(chunk, 0);
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        if (!event.isNewChunk()) {
            for (BlockState tile : event.getChunk().getTileEntities()) {
                if (tile instanceof Hopper) {
                    addHopper(tile.getBlock());
                }
            }
        }
    }

    @EventHandler
    public void onChunkUnload(ChunkUnloadEvent event) {
        Chunk chunk = event.getChunk();
        Iterator<Location> iterator = trackedHoppers.iterator();
        while (iterator.hasNext()) {
            Location location = iterator.next();
            if (location.getChunk().equals(chunk)) {
                iterator.remove();
            }
        }
        chunkHopperCounts.remove(chunk);
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryMoveItem(InventoryMoveItemEvent event) {
        InventoryHolder sourceHolder = event.getSource().getHolder();
        InventoryHolder destinationHolder = event.getDestination().getHolder();
        if (sourceHolder instanceof Hopper || destinationHolder instanceof Hopper) {
            event.setCancelled(true);
        }
    }

    private void loadExistingHoppers() {
        for (var world : plugin.getServer().getWorlds()) {
            for (Chunk chunk : world.getLoadedChunks()) {
                for (BlockState tile : chunk.getTileEntities()) {
                    if (tile instanceof Hopper) {
                        addHopper(tile.getBlock());
                    }
                }
            }
        }
    }

    private void scheduleProcessingTask() {
        task = new BukkitRunnable() {
            @Override
            public void run() {
                processHoppers();
            }
        }.runTaskTimer(plugin, configManager.getProcessIntervalTicks(), configManager.getProcessIntervalTicks());
    }

    private void processHoppers() {
        if (trackedHoppers.isEmpty()) {
            return;
        }

        Set<Location> copy = new HashSet<>(trackedHoppers);
        for (Location location : copy) {
            if (location.getChunk() == null || !location.getChunk().isLoaded()) {
                removeHopper(location.getBlock());
                continue;
            }

            Block block = location.getBlock();
            if (block.getType() != Material.HOPPER) {
                removeHopper(block);
                continue;
            }

            BlockState state = block.getState();
            if (!(state instanceof Hopper)) {
                removeHopper(block);
                continue;
            }

            Hopper hopper = (Hopper) state;
            if (!isHopperEnabled(hopper.getBlock())) {
                continue;
            }

            if (!processHopper(hopper)) {
                attemptPullFromAbove(hopper);
            }
        }
    }

    private boolean processHopper(Hopper hopper) {
        return attemptPushToTarget(hopper);
    }

    private boolean attemptPushToTarget(Hopper hopper) {
        Inventory sourceInventory = hopper.getInventory();
        Inventory targetInventory = getTargetInventory(hopper);
        if (targetInventory == null) {
            return false;
        }

        int remaining = getTransferLimit();
        BlockFace incomingFace = getHopperFacing(hopper.getBlock()).getOppositeFace();
        List<Integer> allowedTargetSlots = getAllowedTargetSlots(targetInventory, incomingFace);

        for (int slot = 0; slot < sourceInventory.getSize() && remaining > 0; slot++) {
            ItemStack current = sourceInventory.getItem(slot);
            if (current == null || current.getAmount() == 0) {
                continue;
            }

            int amountToMove = Math.min(current.getAmount(), remaining);
            ItemStack moving = current.clone();
            moving.setAmount(amountToMove);
            ItemStack remainder = moveItemsToInventory(targetInventory, moving, allowedTargetSlots);
            int moved = amountToMove - (remainder == null ? 0 : remainder.getAmount());
            if (moved <= 0) {
                continue;
            }

            int newAmount = current.getAmount() - moved;
            if (newAmount > 0) {
                ItemStack remainingStack = current.clone();
                remainingStack.setAmount(newAmount);
                sourceInventory.setItem(slot, remainingStack);
            } else {
                sourceInventory.setItem(slot, null);
            }
            remaining -= moved;
        }

        return remaining < getTransferLimit();
    }

    private boolean attemptPullFromAbove(Hopper hopper) {
        Block above = hopper.getBlock().getRelative(BlockFace.UP);
        Inventory sourceInventory = getInventoryFromBlock(above);
        if (sourceInventory == null) {
            return false;
        }

        int remaining = getTransferLimit();
        List<Integer> allowedSourceSlots = getAllowedSourceSlots(sourceInventory, BlockFace.DOWN);
        List<Integer> hopperSlots = getAllowedTargetSlots(hopper.getInventory(), BlockFace.UP);

        boolean movedAny = false;
        for (int slot : allowedSourceSlots) {
            if (remaining <= 0) {
                break;
            }
            ItemStack current = sourceInventory.getItem(slot);
            if (current == null || current.getAmount() == 0) {
                continue;
            }

            int amountToMove = Math.min(current.getAmount(), remaining);
            ItemStack moving = current.clone();
            moving.setAmount(amountToMove);
            ItemStack remainder = moveItemsToInventory(hopper.getInventory(), moving, hopperSlots);
            int moved = amountToMove - (remainder == null ? 0 : remainder.getAmount());
            if (moved <= 0) {
                continue;
            }

            int newAmount = current.getAmount() - moved;
            if (newAmount > 0) {
                ItemStack remainingStack = current.clone();
                remainingStack.setAmount(newAmount);
                sourceInventory.setItem(slot, remainingStack);
            } else {
                sourceInventory.setItem(slot, null);
            }
            remaining -= moved;
            movedAny = true;
        }

        return movedAny;
    }

    private Inventory getTargetInventory(Hopper hopper) {
        BlockFace facing = getHopperFacing(hopper.getBlock());
        Block targetBlock = hopper.getBlock().getRelative(facing);
        return getInventoryFromBlock(targetBlock);
    }

    private BlockFace getHopperFacing(Block block) {
        var data = block.getBlockData();
        if (data instanceof org.bukkit.block.data.type.Hopper hopperData) {
            return hopperData.getFacing();
        }
        return BlockFace.DOWN;
    }

    private boolean isHopperEnabled(Block block) {
        var data = block.getBlockData();
        if (data instanceof org.bukkit.block.data.type.Hopper hopperData) {
            return hopperData.isEnabled();
        }
        return true;
    }

    private Inventory getInventoryFromBlock(Block block) {
        BlockState state = block.getState();
        if (state instanceof InventoryHolder) {
            return ((InventoryHolder) state).getInventory();
        }
        return null;
    }

    private int getTransferLimit() {
        return Math.max(1, (int) Math.round(configManager.getSpeedMultiplier()));
    }

    private List<Integer> getAllowedTargetSlots(Inventory targetInventory, BlockFace incomingFace) {
        if (targetInventory instanceof FurnaceInventory) {
            return getFurnaceInsertionSlots((FurnaceInventory) targetInventory, incomingFace);
        }

        if (targetInventory instanceof CrafterInventory) {
            return getCrafterInsertionSlots((CrafterInventory) targetInventory, incomingFace);
        }

        List<Integer> slots = new ArrayList<>(targetInventory.getSize());
        for (int slot = 0; slot < targetInventory.getSize(); slot++) {
            slots.add(slot);
        }
        return slots;
    }

    private List<Integer> getAllowedSourceSlots(Inventory sourceInventory, BlockFace fromFace) {
        if (sourceInventory instanceof FurnaceInventory) {
            return getFurnaceExtractionSlots((FurnaceInventory) sourceInventory, fromFace);
        }

        if (sourceInventory instanceof CrafterInventory) {
            return getCrafterExtractionSlots((CrafterInventory) sourceInventory, fromFace);
        }

        List<Integer> slots = new ArrayList<>(sourceInventory.getSize());
        for (int slot = 0; slot < sourceInventory.getSize(); slot++) {
            slots.add(slot);
        }
        return slots;
    }

    private List<Integer> getFurnaceInsertionSlots(FurnaceInventory inventory, BlockFace incomingFace) {
        List<Integer> slots = new ArrayList<>(1);
        if (incomingFace == BlockFace.UP) {
            slots.add(0); // input/source slot
        } else if (incomingFace == BlockFace.NORTH || incomingFace == BlockFace.EAST || incomingFace == BlockFace.SOUTH || incomingFace == BlockFace.WEST) {
            slots.add(1); // fuel slot
        }
        return slots;
    }

    private List<Integer> getFurnaceExtractionSlots(FurnaceInventory inventory, BlockFace fromFace) {
        List<Integer> slots = new ArrayList<>(1);
        if (fromFace == BlockFace.DOWN) {
            slots.add(2); // result/output slot
        }
        return slots;
    }

    private List<Integer> getCrafterInsertionSlots(CrafterInventory inventory, BlockFace incomingFace) {
        List<Integer> allowed = new ArrayList<>();
        int size = inventory.getSize();
        for (int slot = 0; slot < size; slot++) {
            if (inventory.getItem(slot) == null) {
                allowed.add(slot);
            }
        }
        if (allowed.isEmpty()) {
            for (int slot = 0; slot < size; slot++) {
                allowed.add(slot);
            }
        }
        return allowed;
    }

    private List<Integer> getCrafterExtractionSlots(CrafterInventory inventory, BlockFace fromFace) {
        List<Integer> allowed = new ArrayList<>(1);
        if (fromFace == BlockFace.DOWN) {
            allowed.add(0);
        }
        return allowed;
    }

    private ItemStack moveItemsToInventory(Inventory inventory, ItemStack stack, List<Integer> allowedSlots) {
        if (stack == null || stack.getAmount() <= 0 || allowedSlots.isEmpty()) {
            return stack;
        }

        int remaining = stack.getAmount();
        ItemStack template = stack.clone();

        for (int slot : allowedSlots) {
            if (remaining <= 0) {
                break;
            }
            ItemStack existing = inventory.getItem(slot);
            if (existing == null || existing.getType().isAir()) {
                int transferAmount = Math.min(remaining, stack.getMaxStackSize());
                ItemStack toInsert = template.clone();
                toInsert.setAmount(transferAmount);
                inventory.setItem(slot, toInsert);
                remaining -= transferAmount;
                continue;
            }
            if (!existing.isSimilar(stack)) {
                continue;
            }
            int availableSpace = existing.getMaxStackSize() - existing.getAmount();
            if (availableSpace <= 0) {
                continue;
            }
            int transferAmount = Math.min(remaining, availableSpace);
            existing.setAmount(existing.getAmount() + transferAmount);
            inventory.setItem(slot, existing);
            remaining -= transferAmount;
        }

        if (remaining <= 0) {
            return null;
        }

        ItemStack remainder = stack.clone();
        remainder.setAmount(remaining);
        return remainder;
    }
}
