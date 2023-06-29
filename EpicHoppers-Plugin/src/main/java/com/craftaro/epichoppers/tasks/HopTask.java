package com.craftaro.epichoppers.tasks;

import com.craftaro.epichoppers.boost.BoostData;
import com.craftaro.epichoppers.containers.CustomContainer;
import com.craftaro.epichoppers.hopper.levels.modules.ModuleAutoCrafting;
import com.craftaro.epichoppers.settings.Settings;
import com.craftaro.epichoppers.utils.HopperDirection;
import com.craftaro.epichoppers.utils.Methods;
import com.craftaro.epichoppers.EpicHoppers;
import com.craftaro.epichoppers.hopper.levels.modules.Module;
import com.craftaro.epichoppers.utils.StorageContainerCache;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Hopper;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.minecart.StorageMinecart;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class HopTask extends BukkitRunnable {
    private final EpicHoppers plugin;
    private final int hopTicks;

    public HopTask(EpicHoppers plugin) {
        this.plugin = plugin;
        this.hopTicks = Math.max(1, Settings.HOP_TICKS.getInt() / 2); // Purposeful integer division. Don't go below 1.
        this.runTaskTimer(plugin, 0, 2);
    }

    @Override
    public void run() {
        for (final com.craftaro.epichoppers.hopper.Hopper hopper : this.plugin.getHopperManager().getHoppers().values()) {

            try {
                // Get this hopper's location.
                Location location = hopper.getLocation();

                // Skip if chunk is not loaded.
                if (location.getWorld() == null || !location.getWorld().isChunkLoaded(location.getBlockX() >> 4, location.getBlockZ() >> 4)) {
                    continue;
                }

                // Get Hopper Block.
                Block block = location.getBlock();

                // If block is not a hopper continue.
                if (block.getType() != Material.HOPPER) {
                    continue;
                }

                // If hopper block is powered, update its redstone state and continue.
                if (block.getBlockPower() > 0) {
                    hopper.tryTick(this.hopTicks, false);
                    continue;
                }

                if (!hopper.tryTick(this.hopTicks, true)) {
                    continue;
                }

                // Amount to be moved.
                BoostData boostData = this.plugin.getBoostManager().getBoost(hopper.getPlacedBy());
                int maxToMove = hopper.getLevel().getAmount() * (boostData == null ? 1 : boostData.getMultiplier());

                // Get hopper state data.
                Hopper hopperState = (Hopper) block.getState();
                HopperDirection hopperDirection = HopperDirection.getDirection(hopperState.getRawData());
                Location pointingLocation = hopperDirection.getLocation(location);
                final StorageContainerCache.Cache hopperCache = StorageContainerCache.getCachedInventory(block);

                // Create list to hold blocked materials.
                List<Material> blockedMaterials = new ArrayList<>();

                // Cycle through modules.
                hopper.getLevel().getRegisteredModules().stream()
                        .filter(Objects::nonNull)
                        .forEach(module -> {
                            try {
                                // Run Module
                                module.run(hopper, hopperCache);

                                // Add banned materials to list.
                                List<Material> materials = module.getBlockedItems(hopper);
                                if (materials != null && !materials.isEmpty()) {
                                    blockedMaterials.addAll(materials);
                                }
                            } catch (Exception ex) {
                                ex.printStackTrace();
                            }
                        });

                // Process extra hopper pull
                pullItemsFromContainers(hopper, hopperCache, maxToMove);

                // Void out items
                processVoidFilter(hopper, hopperCache, maxToMove);

                // don't proccess any further if the hopper is empty or if all items are blocked
                boolean doProcess = false;
                for (int i = 0; i < hopperCache.cachedInventory.length; i++) {
                    final ItemStack item = hopperCache.cachedInventory[i];

                    // Can we check this item?
                    if (    // Ignore this one if the slot is empty
                            item == null
                                    // Don't try to move items that we've added this round
                                    || (hopperCache.cacheChanged[i] && item.getAmount() - hopperCache.cacheAdded[i] < maxToMove)
                                    // skip if blocked or voidlisted
                                    || blockedMaterials.contains(item.getType())
                                    || hopper.getFilter().getVoidList().stream().anyMatch(itemStack -> Methods.isSimilarMaterial(itemStack, item))) {
                        continue;
                    }

                    doProcess = true;
                    break;
                }
                if (!doProcess) {
                    continue;
                }

                CustomContainer container = this.plugin.getContainerManager().getCustomContainer(pointingLocation.getBlock());
                if (container != null) {
                    for (int i = 0; i < 5; i++) {
                        final ItemStack item = hopperCache.cachedInventory[i];
                        if (item == null) {
                            continue;
                        }

                        if (container.addToContainer(item)) {
                            if (item.getAmount() == 1) {
                                hopperCache.removeItem(i);
                            } else {
                                item.setAmount(item.getAmount() - 1);
                                hopperCache.dirty = hopperCache.cacheChanged[i] = true;
                            }
                            break;
                        }
                    }
                }

                // Move items into destination containers
                pushItemsIntoContainers(hopper, hopperCache, maxToMove, blockedMaterials, hopperDirection);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // push out inventory changes
        StorageContainerCache.update();
    }

    private void debt(ItemStack item, int amountToMove, InventoryHolder currentHolder) {
        if (item.getAmount() - amountToMove > 0) {
            item.setAmount(item.getAmount() - amountToMove);
        } else {
            currentHolder.getInventory().removeItem(item);
        }
    }

    private StorageContainerCache.Cache getFilterEndpoint(com.craftaro.epichoppers.hopper.Hopper hopper) {
        // Get endpoint location.
        Location endPoint = hopper.getFilter().getEndPoint();

        // Check for null.
        if (endPoint == null) {
            return null;
        }

        // Make sure chunk is loaded.
        if (!endPoint.getWorld().isChunkLoaded(endPoint.getBlockX() >> 4, endPoint.getBlockZ() >> 4)) {
            return null;
        }

        // Fetch Cache
        StorageContainerCache.Cache cache = StorageContainerCache.getCachedInventory(endPoint.getBlock());

        // Remove if not a container.
        if (cache == null) {
            hopper.getFilter().setEndPoint(null);
            return null;
        }

        return cache;
    }

    private void pullItemsFromContainers(com.craftaro.epichoppers.hopper.Hopper toHopper, StorageContainerCache.Cache hopperCache, int maxToMove) {
        // Grab items from the container above (includes storage/hopper minecarts, EpicFarming farm items and AdvancedChests chest)
        // If the container above is a hopper, ignore it if it's pointing down
        Block above = toHopper.getBlock().getRelative(BlockFace.UP);

        Collection<Entity> nearbyEntities = null;
        StorageContainerCache.Cache aboveCache = null;

        CustomContainer container = this.plugin.getContainerManager().getCustomContainer(above);
        if ((container != null)
                || (above.getType() != Material.AIR)
                && (above.getType() != Material.HOPPER || HopperDirection.getDirection(above.getState().getRawData()) != HopperDirection.DOWN)
                && (aboveCache = StorageContainerCache.getCachedInventory(above)) != null
                || !(nearbyEntities = above.getWorld().getNearbyEntities(above.getLocation().clone(), 0.5, 0.5, 0.5)).isEmpty()) {
            // Get the inventory holder. Special check for EpicFarming.
            // Get the slots that we can pull items from.
            InventoryHolder aboveInvHolder;
            final int[] pullableSlots;
            final ItemStack[] contents;
            if (aboveCache != null) {
                pullableSlots = this.getPullableSlots(above.getType(), aboveCache.cachedInventory.length - 1);
                contents = aboveCache.cachedInventory;
                aboveInvHolder = null;
            } else if (container != null) {
                aboveInvHolder = null;
                contents = container.getItems();
                pullableSlots = IntStream.rangeClosed(0, contents.length - 1).toArray();
            } else {
                if ((aboveInvHolder = this.getRandomInventoryHolderFromEntities(nearbyEntities)) == null
                        || ((Minecart) aboveInvHolder).getLocation().getBlockY() + 1 == above.getY()) {
                    return;
                }
                if (aboveInvHolder instanceof StorageMinecart) {
                    pullableSlots = IntStream.rangeClosed(0, 26).toArray();
                } else {
                    pullableSlots = IntStream.rangeClosed(0, 4).toArray();
                }
                contents = aboveInvHolder.getInventory().getContents();
            }

            // Don't fill the last inventory slot on crafting hoppers (fixes crafters getting stuck)
            Module crafting = toHopper.getLevel().getModule("AutoCrafting");
            ItemStack toCraft = crafting instanceof ModuleAutoCrafting ? ((ModuleAutoCrafting) crafting).getAutoCrafting(toHopper) : null;

            // Loop over the pullable slots and try to pull something.
            for (int i : pullableSlots) {
                // Get the item
                final ItemStack toMove = contents[i];

                // If item is invalid, try the next slot.
                if (toMove == null || toMove.getAmount() == 0) {
                    continue;
                }

                // if we're not moving the item that we're trying to craft, we need to verify that we're not trying to fill the last slot
                // (filling every slot leaves no room for the crafter to function)
                if (toCraft != null && !Methods.isSimilarMaterial(toMove, toCraft) && !Methods.canMoveReserved(hopperCache.cachedInventory, toMove)) {
                    continue;
                }

                // respect whitelist/blacklist filters
                if (toHopper.getFilter().getEndPoint() == null
                        && !(toHopper.getFilter().getWhiteList().isEmpty() && toHopper.getFilter().getBlackList().isEmpty())) {
                    // this hopper has a filter with no rejection endpoint, so don't absorb disalowed items
                    // whitelist has priority
                    if (!toHopper.getFilter().getWhiteList().isEmpty()) {
                        // is this item on the whitelist?
                        if (toHopper.getFilter().getWhiteList().stream().noneMatch(item -> Methods.isSimilarMaterial(toMove, item))) {
                            // nope!
                            continue;
                        }
                    } else {
                        // check the blacklist
                        if (toHopper.getFilter().getBlackList().stream().anyMatch(item -> Methods.isSimilarMaterial(toMove, item))) {
                            // don't grab this, then
                            continue;
                        }
                    }
                }

                // Get amount to move.
                int amountToMove = Math.min(toMove.getAmount(), maxToMove);

                // Create item that will be moved.
                ItemStack itemToMove = toMove.clone();
                itemToMove.setAmount(amountToMove);

                // Add item to container and break on success.
                //if (this.addItem(toHopper, aboveInvHolder, hopperState, hopperState.getBlock().getType(), toMove, itemToMove, amountToMove))
                if (hopperCache.addItem(itemToMove)) {
                    // remove item from the container
                    if (aboveCache != null) {
                        aboveCache.removeItems(itemToMove);
                    } else {
                        if (container != null) {
                            container.removeFromContainer(itemToMove, amountToMove);
                        } else {
                            this.debt(itemToMove, amountToMove, aboveInvHolder);
                        }
                    }
                    break;
                }
            }
        }
    }

    private void pushItemsIntoContainers(com.craftaro.epichoppers.hopper.Hopper hopper, StorageContainerCache.Cache hopperCache, int maxToMove, Collection<Material> blockedMaterials, HopperDirection hopperDirection) {

        // Filter target, if any
        StorageContainerCache.Cache filterCache = getFilterEndpoint(hopper);

        // Get remote linked containers.
        List<Location> linkedContainers = hopper.getLinkedBlocks();
        boolean checkForMinecarts = false;

        // Add container that the hopper is attached to physically.
        final Location pointingLocation = hopper.getLocation().add(hopperDirection.getX(), hopperDirection.getY(), hopperDirection.getZ());
        if (!linkedContainers.contains(pointingLocation)
                && pointingLocation.getWorld().isChunkLoaded(pointingLocation.getBlockX() >> 4, pointingLocation.getBlockZ() >> 4)) {
            switch (pointingLocation.getBlock().getType().name()) {
                case "AIR":
                case "RAILS":
                case "RAIL":
                case "POWERED_RAIL":
                case "DETECTOR_RAIL":
                    // Add storage/hopper minecarts the hopper is pointing into if there aren't any destinations
                    checkForMinecarts = linkedContainers.size() < 2;
                    break;
                default:
                    linkedContainers.add(pointingLocation);
            }
        }

        // Loop through targets until we can move stuff into one of them
        for (Location targetLocation : linkedContainers) {

            // Don't check if it's not in a loaded chunk
            if (!targetLocation.getWorld().isChunkLoaded(targetLocation.getBlockX() >> 4, targetLocation.getBlockZ() >> 4)) {
                continue;
            }

            // special case for ender chests
            final Block targetBlock = targetLocation.getBlock();
            if (targetBlock.getType() == Material.ENDER_CHEST) {
                // Use the ender storage of whoever owns the hopper if they're online
                OfflinePlayer op = Bukkit.getOfflinePlayer(hopper.getPlacedBy());
                if (op.isOnline()) {
                    Inventory destinationInventory = op.getPlayer().getEnderChest();
                    StorageContainerCache.Cache cache = new StorageContainerCache.Cache(targetBlock.getType(), destinationInventory.getContents());
                    if (tryPush(hopper, hopperCache, cache, filterCache, maxToMove, blockedMaterials)) {
                        // update inventory and exit
                        if (cache.isDirty()) {
                            destinationInventory.setContents(cache.cachedInventory);
                        }
                        return;
                    }
                }
                // Can't put anything in there, so keep looking for targets
                continue;
            }

            CustomContainer container = this.plugin.getContainerManager().getCustomContainer(targetLocation.getBlock());
            if (container != null && tryPushCustomContainer(hopper, hopperCache, container, filterCache, maxToMove, blockedMaterials)) {
                return;
            }

            // Is this a storage container?
            StorageContainerCache.Cache targetCache = StorageContainerCache.getCachedInventory(targetBlock);
            if (targetCache == null) {
                // if it's not, we need to unlink it
                hopper.removeLinkedBlock(targetLocation);
                continue;
            }

            // Now attempt to push items into this container and exit on success
            if (tryPush(hopper, hopperCache, targetCache, filterCache, maxToMove, blockedMaterials)) {
                return;
            }
        }

        // if we've gotten this far, check if we can push into a minecart
        if (checkForMinecarts) {
            for (InventoryHolder minecartInventory : hopper.getWorld().getNearbyEntities(pointingLocation.clone().add(0.5, 0.5, 0.5), 0.5, 0.5, 0.5)
                    .stream().filter(e -> e.getType() == EntityType.MINECART_CHEST || e.getType() == EntityType.MINECART_HOPPER)
                    .map(InventoryHolder.class::cast).collect(Collectors.toSet())) {
                StorageContainerCache.Cache cache = new StorageContainerCache.Cache(Material.CHEST, minecartInventory.getInventory().getContents());
                if (tryPush(hopper, hopperCache, cache, filterCache, maxToMove, blockedMaterials)) {
                    if (cache.isDirty()) {
                        minecartInventory.getInventory().setContents(cache.cachedInventory);
                    }
                    return;
                }
            }
        }
    }

    private boolean tryPushCustomContainer(com.craftaro.epichoppers.hopper.Hopper hopper,
                                           StorageContainerCache.Cache hopperCache,
                                           CustomContainer container,
                                           StorageContainerCache.Cache filterCache,
                                           int maxToMove, Collection<Material> blockedMaterials) {
        for (int i = 0; i < 5; i++) {
            // Get potential item to move.
            ItemStack item = hopperCache.cachedInventory[i];

            // Can we check this item?
            if (    // Ignore this one if the slot is empty
                    item == null
                            // Don't try to move items that we've added this round
                            || (hopperCache.cacheChanged[i] && item.getAmount() - hopperCache.cacheAdded[i] < maxToMove)
                            // skip if blocked or voidlisted
                            || blockedMaterials.contains(item.getType())
                            || hopper.getFilter().getVoidList().stream().anyMatch(itemStack -> Methods.isSimilarMaterial(itemStack, item))) {
                continue;
            }

            // Create item that will be moved.
            ItemStack itemToMove = item.clone();
            itemToMove.setAmount(Math.min(item.getAmount(), maxToMove));

            // Process whitelist and blacklist.
            boolean blocked = (!hopper.getFilter().getWhiteList().isEmpty() && hopper.getFilter().getWhiteList().stream().noneMatch(itemStack -> itemStack.isSimilar(item))
                    || hopper.getFilter().getBlackList().stream().anyMatch(itemStack -> itemStack.isSimilar(item)));

            // If blocked check to see if a movement can be made
            if (blocked) {
                if (filterCache != null && filterCache.addItem(itemToMove)) {
                    hopperCache.removeItems(itemToMove);
                    return true;
                }
                // can't move into a filter chest, so keep looking for something else to move
                continue;
            }

            // Add item to container and return on success.
            if (container.addToContainer(itemToMove)) {
                hopperCache.removeItems(itemToMove);
                return true;
            }
        }
        return false;
    }

    private boolean tryPush(com.craftaro.epichoppers.hopper.Hopper hopper,
                            StorageContainerCache.Cache hopperCache,
                            StorageContainerCache.Cache targetCache,
                            StorageContainerCache.Cache filterCache,
                            int maxToMove, Collection<Material> blockedMaterials) {

        // Loop through all of our hopper's item slots.
        for (int i = 0; i < 5; i++) {
            // Get potential item to move.
            ItemStack item = hopperCache.cachedInventory[i];

            // Can we check this item?
            if (    // Ignore this one if the slot is empty
                    item == null
                            // Don't try to move items that we've added this round
                            || (hopperCache.cacheChanged[i] && item.getAmount() - hopperCache.cacheAdded[i] < maxToMove)
                            // skip if blocked or voidlisted
                            || blockedMaterials.contains(item.getType())
                            || hopper.getFilter().getVoidList().stream().anyMatch(itemStack -> Methods.isSimilarMaterial(itemStack, item))) {
                continue;
            }

            // Create item that will be moved.
            ItemStack itemToMove = item.clone();
            itemToMove.setAmount(Math.min(item.getAmount(), maxToMove));

            // Process whitelist and blacklist.
            boolean blocked = (!hopper.getFilter().getWhiteList().isEmpty() && hopper.getFilter().getWhiteList().stream().noneMatch(itemStack -> itemStack.isSimilar(item))
                    || hopper.getFilter().getBlackList().stream().anyMatch(itemStack -> itemStack.isSimilar(item)));

            // If blocked check to see if a movement can be made
            if (blocked) {
                if (filterCache != null && filterCache.addItem(itemToMove)) {
                    hopperCache.removeItems(itemToMove);
                    return true;
                }
                // can't move into a filter chest, so keep looking for something else to move
                continue;
            }

            // Add item to container and return on success.
            if (targetCache.addItem(itemToMove)) {
                hopperCache.removeItems(itemToMove);
                return true;
            }
        }
        return false;
    }

    private void processVoidFilter(com.craftaro.epichoppers.hopper.Hopper hopper, StorageContainerCache.Cache hopperCache, int maxToMove) {
        // Loop over hopper inventory to process void filtering.
        if (!hopper.getFilter().getVoidList().isEmpty()) {
            ItemStack[] hopperContents = hopperCache.cachedInventory;
            for (int i = 0; i < hopperContents.length; i++) {
                final ItemStack item = hopperContents[i];
                if (item != null && hopper.getFilter().getVoidList().stream().anyMatch(itemStack -> Methods.isSimilarMaterial(itemStack, item))) {
                    int amt = Math.max(0, item.getAmount() - maxToMove);
                    if (amt == 0) {
                        hopperCache.removeItem(i);
                    } else {
                        item.setAmount(amt);
                    }
                    hopperCache.setDirty(hopperCache.cacheChanged[i] = true);
                    break;
                }
            }
        }
    }

    /**
     * Gets a set of slots that can be pulled from based on the given material
     *
     * @param material The material to get pullable slots for
     * @return A set of valid pullable slots
     */
    private int[] getPullableSlots(Material material, int contentsLength) {
        if (material.name().contains("SHULKER_BOX")) {
            return IntStream.rangeClosed(0, 26).toArray();
        }

        switch (material.name()) {
            case "BARREL":
            case "CHEST":
            case "TRAPPED_CHEST":
                return IntStream.rangeClosed(0, contentsLength).toArray();
            case "BREWING_STAND":
                return IntStream.rangeClosed(0, 2).toArray();
            case "HOPPER":
                return IntStream.rangeClosed(0, 4).toArray();
            case "DISPENSER":
            case "DROPPER":
                return IntStream.rangeClosed(0, 8).toArray();
            case "SMOKER":
            case "BLAST_FURNACE":
            case "BURNING_FURNACE":
            case "FURNACE":
                return IntStream.of(2).toArray();
            default:
                return IntStream.empty().toArray();
        }
    }

    /**
     * Gets a random InventoryHolder from a collection of entities
     * Only grabs InventoryHolders from StorageMinecarts and HopperMinecarts
     *
     * @param entities The collection of entities
     * @return A random InventoryHolder if one exists, otherwise null
     */
    private InventoryHolder getRandomInventoryHolderFromEntities(Collection<Entity> entities) {
        List<InventoryHolder> inventoryHolders = new ArrayList<>();
        entities.stream().filter(e -> e.getType() == EntityType.MINECART_CHEST || e.getType() == EntityType.MINECART_HOPPER)
                .forEach(e -> inventoryHolders.add((InventoryHolder) e));
        if (inventoryHolders.isEmpty()) {
            return null;
        }
        if (inventoryHolders.size() == 1) {
            return inventoryHolders.get(0);
        }
        return inventoryHolders.get(ThreadLocalRandom.current().nextInt(inventoryHolders.size()));
    }
}
