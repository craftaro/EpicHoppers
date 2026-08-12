package com.songoda.epichoppers.tasks;

import com.songoda.core.compatibility.ServerVersion;
import com.songoda.epichoppers.boost.BoostData;
import com.songoda.epichoppers.containers.CustomContainer;
import com.songoda.epichoppers.hopper.HopperImpl;
import com.songoda.epichoppers.hopper.levels.modules.ModuleAutoCrafting;
import com.songoda.epichoppers.settings.Settings;
import com.songoda.epichoppers.utils.HopperDirection;
import com.songoda.epichoppers.utils.Methods;
import com.songoda.epichoppers.EpicHoppers;
import com.songoda.epichoppers.hopper.levels.modules.Module;
import com.songoda.epichoppers.utils.StorageContainerCache;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Crafter;
import org.bukkit.block.Hopper;
import org.bukkit.block.data.Directional;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.minecart.StorageMinecart;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
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
        for (final HopperImpl hopper : this.plugin.getHopperManager().getHoppers().values()) {
            try {
                // Get hopper location and ensure chunk is loaded.
                Location location = hopper.getLocation();
                if (location.getWorld() == null
                        || !location.getWorld().isChunkLoaded(location.getBlockX() >> 4, location.getBlockZ() >> 4)) {
                    continue;
                }

                Block block = location.getBlock();
                if (block.getType() != Material.HOPPER) {
                    continue;
                }

                // Skip if hopper is powered.
                if (block.getBlockPower() > 0) {
                    hopper.tryTick(this.hopTicks, false);
                    continue;
                }

                if (!hopper.tryTick(this.hopTicks, true)) {
                    continue;
                }

                // Determine maxToMove based on hopper level and boost.
                BoostData boostData = this.plugin.getBoostManager().getBoost(hopper.getPlacedBy());
                int maxToMove = hopper.getLevel().getAmount() * (boostData == null ? 1 : boostData.getMultiplier());

                // Determine hopper pointing location.
                HopperDirection hopperDirection;
                Location pointingLocation;
                if (ServerVersion.isServerVersionBelow(ServerVersion.V1_20)) {
                    Hopper hopperState = (Hopper) block.getState();
                    hopperDirection = HopperDirection.getDirection(hopperState.getRawData());
                    pointingLocation = hopperDirection.getLocation(location);
                } else {
                    hopperDirection = HopperDirection.valueOf(((Directional) block.getBlockData()).getFacing().name());
                    BlockFace blockFace = hopperDirection.getDirectionFacing();
                    pointingLocation = block.getLocation().getBlock().getRelative(blockFace).getLocation();
                }

                // Retrieve hopper cache.
                final StorageContainerCache.Cache hopperCache = StorageContainerCache.getCachedInventory(block);

                // Gather blocked materials from all modules.
                List<Material> blockedMaterials = new ArrayList<>();
                hopper.getLevel().getRegisteredModules().stream()
                        .filter(Objects::nonNull)
                        .forEach(module -> {
                            try {
                                module.run(hopper, hopperCache);
                                List<Material> materials = module.getBlockedItems(hopper);
                                if (materials != null && !materials.isEmpty()) {
                                    blockedMaterials.addAll(materials);
                                }
                            } catch (Exception ex) {
                                ex.printStackTrace();
                            }
                        });

                // Process extra pull and void filter.
                pullItemsFromContainers(hopper, hopperCache, maxToMove);
                processVoidFilter(hopper, hopperCache, maxToMove);

                // Check if any slot is available for processing.
                boolean doProcess = false;
                for (int i = 0; i < hopperCache.cachedInventory.length; i++) {
                    final ItemStack item = hopperCache.cachedInventory[i];
                    if (item == null
                            || (hopperCache.cacheChanged[i] && item.getAmount() - hopperCache.cacheAdded[i] < maxToMove)
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

                // Transfer items directly to the destination container.
                CustomContainer container = this.plugin.getContainerManager().getCustomContainer(pointingLocation.getBlock());
                if (container != null) {
                    // Loop over every slot.
                    for (int i = 0; i < hopperCache.cachedInventory.length; i++) {
                        final ItemStack item = hopperCache.cachedInventory[i];
                        if (item == null) {
                            continue;
                        }
                        // Transfer a single item at a time.
                        ItemStack singleItem = item.clone();
                        singleItem.setAmount(1);
                        if (container.addToContainer(singleItem)) {
                            // Remove exactly one item from the hopper cache.
                            hopperCache.removeItems(singleItem);
                            break;
                        }
                    }
                }

                // Push remaining items into destination containers.
                pushItemsIntoContainers(hopper, hopperCache, maxToMove, blockedMaterials, hopperDirection);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        // Push out inventory changes and reset cache state.
        StorageContainerCache.update();
    }


    private void debt(ItemStack item, int amountToMove, InventoryHolder currentHolder) {
        if (item.getAmount() - amountToMove > 0) {
            item.setAmount(item.getAmount() - amountToMove);
        } else {
            currentHolder.getInventory().removeItem(item);
        }
    }

    private boolean tryPushCrafter(HopperImpl hopper,
                                  StorageContainerCache.Cache hopperCache,
                                  Crafter crafter,
                                  Inventory crafterInventory,
                                  StorageContainerCache.Cache filterCache,
                                  int maxToMove, Collection<Material> blockedMaterials) {

        for (int hopperSlot = 0; hopperSlot < 5; hopperSlot++) {
            ItemStack item = hopperCache.cachedInventory[hopperSlot];

            if (item == null
                    || (hopperCache.cacheChanged[hopperSlot] && item.getAmount() - hopperCache.cacheAdded[hopperSlot] < maxToMove)
                    || blockedMaterials.contains(item.getType())
                    || hopper.getFilter().getVoidList().stream().anyMatch(itemStack -> Methods.isSimilarMaterial(itemStack, item))) {
                continue;
            }

            ItemStack itemToMove = item.clone();
            itemToMove.setAmount(1);

            boolean blocked = (!hopper.getFilter().getWhiteList().isEmpty() && hopper.getFilter().getWhiteList().stream().noneMatch(itemStack -> itemStack.isSimilar(item))
                    || hopper.getFilter().getBlackList().stream().anyMatch(itemStack -> itemStack.isSimilar(item)));

            if (blocked) {
                if (filterCache != null && filterCache.addItem(itemToMove)) {
                    hopperCache.removeItems(itemToMove);
                    return true;
                }
                continue;
            }

            int moved = addToCrafterInventory(crafter, crafterInventory, itemToMove);
            if (moved > 0) {
                ItemStack removed = itemToMove.clone();
                removed.setAmount(moved);
                hopperCache.removeItems(removed);
                return true;
            }
        }

        return false;
    }

    private int addToCrafterInventory(Crafter crafter, Inventory crafterInventory, ItemStack itemToMove) {
        if (itemToMove == null || itemToMove.getAmount() <= 0) {
            return 0;
        }

        for (int crafterSlot = 0; crafterSlot < 9; crafterSlot++) {
            if (crafter.isSlotDisabled(crafterSlot)) {
                continue;
            }

            ItemStack existing = crafterInventory.getItem(crafterSlot);
            if (existing != null && existing.getAmount() != 0) {
                continue;
            }

            ItemStack placed = itemToMove.clone();
            placed.setAmount(1);
            crafterInventory.setItem(crafterSlot, placed);
            return 1;
        }

        return 0;
    }

    private StorageContainerCache.Cache getFilterEndpoint(HopperImpl hopper) {
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

    private void pullItemsFromContainers(HopperImpl toHopper, StorageContainerCache.Cache hopperCache, int maxToMove) {
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

    private void pushItemsIntoContainers(HopperImpl hopper, StorageContainerCache.Cache hopperCache, int maxToMove, Collection<Material> blockedMaterials, HopperDirection hopperDirection) {

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

            //Check if the target is a Crafter block
            if (ServerVersion.isServerVersionAtLeast(ServerVersion.V1_21_1) && targetBlock.getType() == Material.CRAFTER) {
                Crafter crafter = (Crafter) targetBlock.getState();
                PersistentDataContainer crafterData = crafter.getPersistentDataContainer();
                Inventory crafterInventory = crafter.getInventory();

                int lastFilledSlot = -1; //Get it from the persistent data container

                if (tryPushCrafter(hopper, hopperCache, crafter, crafterInventory, filterCache, maxToMove, blockedMaterials)) {
                    return;
                }

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

    private boolean tryPushCustomContainer(HopperImpl hopper,
                                           StorageContainerCache.Cache hopperCache,
                                           CustomContainer container,
                                           StorageContainerCache.Cache filterCache,
                                           int maxToMove, Collection<Material> blockedMaterials) {
        for (int i = 0; i < 5; i++) {
            // Get potential item to move.
            ItemStack item = hopperCache.cachedInventory[i];

            if (item == null ||
                    (hopperCache.cacheChanged[i] && item.getAmount() - hopperCache.cacheAdded[i] < maxToMove) ||
                    blockedMaterials.contains(item.getType()) ||
                    hopper.getFilter().getVoidList().stream().anyMatch(itemStack -> Methods.isSimilarMaterial(itemStack, item))) {
                continue;
            }

            // Create a clone with the amount to move.
            ItemStack itemToMove = item.clone();
            itemToMove.setAmount(Math.min(item.getAmount(), maxToMove));

            // Process whitelist/blacklist.
            boolean blocked = (!hopper.getFilter().getWhiteList().isEmpty() &&
                    hopper.getFilter().getWhiteList().stream().noneMatch(itemStack -> itemStack.isSimilar(item)))
                    || hopper.getFilter().getBlackList().stream().anyMatch(itemStack -> itemStack.isSimilar(item));

            // If blocked, try filterCache.
            if (blocked) {
                if (filterCache != null && filterCache.addItem(itemToMove)) {
                    // Remove from cache immediately.
                    hopperCache.removeItems(itemToMove);
                    return true;
                }
                continue;
            }

            // Remove items from cache immediately - this “locks” the transfer.
            hopperCache.removeItems(itemToMove);
            // Now pass the (full) itemToMove to the container.
            if (container.addToContainer(itemToMove)) {
                return true;
            } else {
                // If the container failed to accept the item, we have an issue.
                // Ideally, container.addToContainer should be atomic.
                // In production, you might want to add a rollback: e.g., re-add items to the cache.
                System.err.println("Transfer failed in container.addToContainer; duplication risk.");
            }
        }
        return false;
    }

    private boolean tryPush(HopperImpl hopper,
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

    private void processVoidFilter(HopperImpl hopper, StorageContainerCache.Cache hopperCache, int maxToMove) {
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
