package com.songoda.epichoppers.tasks;

import com.songoda.epichoppers.EpicHoppers;
import com.songoda.epichoppers.boost.BoostData;
import com.songoda.epichoppers.hopper.HopperManager;
import com.songoda.epichoppers.hopper.levels.modules.Module;
import com.songoda.epichoppers.utils.HopperDirection;
import com.songoda.epichoppers.utils.Methods;
import com.songoda.epichoppers.utils.ServerVersion;
import com.songoda.epichoppers.utils.settings.Setting;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Hopper;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.minecart.StorageMinecart;
import org.bukkit.inventory.BrewerInventory;
import org.bukkit.inventory.DoubleChestInventory;
import org.bukkit.inventory.FurnaceInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Created by songoda on 3/14/2017.
 */
public class HopTask extends BukkitRunnable {

    // Hop to the bop to the be bop top.

    private static EpicHoppers plugin;

    private final Map<InventoryHolder, ItemStack> blacklist = new HashMap<>();
    private final int hopTicks;

    public HopTask(EpicHoppers plug) {
        plugin = plug;
        this.hopTicks = Math.max(1, Setting.HOP_TICKS.getInt() / 2); // Purposeful integer division. Don't go below 1.
        this.runTaskTimer(plugin, 0, 2);
    }

    @Override
    public void run() {
        Collection<com.songoda.epichoppers.hopper.Hopper> hoppers = plugin.getHopperManager().getHoppers().values();
        Iterator<com.songoda.epichoppers.hopper.Hopper> itr = hoppers.iterator();

        Set<Location> toRemove = new HashSet<>();

        main:
        while (itr.hasNext()) {
            com.songoda.epichoppers.hopper.Hopper hopper = itr.next();

            try {
                // Get this hoppers location.
                Location location = hopper.getLocation();

                // Skip is chunk not loaded.
                if (location.getWorld() == null || !location.getWorld().isChunkLoaded(location.getBlockX() >> 4, location.getBlockZ() >> 4))
                    continue;

                // Get Hopper Block.
                Block block = location.getBlock();

                // If block is not a hopper remove and continue.
                if (block.getType() != Material.HOPPER) {
                    toRemove.add(location);
                    continue;
                }

                // If hopper block is powered continue.
                if (block.isBlockPowered() || block.isBlockIndirectlyPowered()) {
                    hopper.tryTick(this.hopTicks, false);
                    continue;
                }

                if (!hopper.tryTick(this.hopTicks, true))
                    continue;

                // Get hopper state.
                Hopper hopperState = (Hopper) block.getState();

                // Create list to hold blocked materials.
                List<Material> blockedMaterials = new ArrayList<>();

                // Cycle through modules.
                for (Module module : hopper.getLevel().getRegisteredModules()) {
                    // Run Module
                    module.run(hopper, hopperState.getInventory());

                    // Add banned materials to list.
                    List<Material> materials = module.getBlockedItems(hopper);
                    if (materials == null || materials.isEmpty())
                        continue;

                    blockedMaterials.addAll(materials);
                }

                // Get remote linked containers.
                List<Location> linkedContainers = hopper.getLinkedBlocks();

                // Add linked container that the hopper is attached to physically.
                HopperDirection hopperDirection = HopperDirection.getDirection(hopperState.getRawData());
                linkedContainers.add(hopperDirection.getLocation(location));

                // Amount to be moved.
                BoostData boostData = plugin.getBoostManager().getBoost(hopper.getPlacedBy());
                int amount = hopper.getLevel().getAmount() * (boostData == null ? 1 : boostData.getMultiplier());

                // Grab items from the container above (includes storage/hopper minecarts and EpicFarming farm items)
                // If the container above is a hopper, ignore it if it's pointing down
                Block above = block.getRelative(BlockFace.UP);
                boolean isFarmItem = false;
                Collection<Entity> nearbyEntities = null;
                outer:
                if ((above.getState() instanceof InventoryHolder
                        && (above.getType() != Material.HOPPER || HopperDirection.getDirection(above.getState().getRawData()) != HopperDirection.DOWN))
                        || !(nearbyEntities = above.getWorld().getNearbyEntities(above.getLocation().clone().add(0.5, 0.5, 0.5), 0.5, 0.5, 0.5)).isEmpty()
                        || (isFarmItem = this.isFarmItem(above))) {

                    // Get the inventory holder. Special check for EpicFarming.
                    // Get the slots that we can pull items from.
                    InventoryHolder aboveInvHolder;
                    int[] pullableSlots;
                    if (isFarmItem) {
                        aboveInvHolder = this.getEpicFarmingItemWrapped(above);
                        pullableSlots = IntStream.rangeClosed(27, 53).toArray();
                    } else if (nearbyEntities != null) {
                        if ((aboveInvHolder = this.getRandomInventoryHolderFromEntities(nearbyEntities)) == null)
                            break outer;
                        if (aboveInvHolder instanceof StorageMinecart) {
                            pullableSlots = IntStream.rangeClosed(0, 26).toArray();
                        } else {
                            pullableSlots = IntStream.rangeClosed(0, 4).toArray();
                        }
                    } else {
                        aboveInvHolder = (InventoryHolder) above.getState();
                        pullableSlots = this.getPullableSlots(aboveInvHolder, above.getType());
                    }

                    ItemStack[] contents = aboveInvHolder.getInventory().getContents();

                    // Loop over the pullable slots and try to pull something.
                    for (int i : pullableSlots) {
                        // Get the item
                        ItemStack item = contents[i];

                        // If item is invalid, try the next slot.
                        if (item == null)
                            continue;

                        // Get amount to move.
                        int amountToMove = item.getAmount() < amount ? item.getAmount() : amount;

                        // Create item that will be moved.
                        ItemStack itemToMove = item.clone();
                        itemToMove.setAmount(amountToMove);

                        // Add item to container and break on success.
                        if (this.addItem(hopper, aboveInvHolder, hopperState, block.getType(), item, itemToMove, amountToMove))
                            break;
                    }
                }

                // Fetch all hopper contents.
                ItemStack[] hopperContents = hopperState.getInventory().getContents();

                // Loop over hopper inventory to process void filtering.
                if (!hopper.getFilter().getVoidList().isEmpty()) {
                    for (ItemStack item : hopperContents) {
                        // Skip if slot empty.
                        if (item == null)
                            continue;

                        // Try to void it out
                        int amountToVoid = item.getAmount() < amount ? item.getAmount() : amount;
                        if (hopper.getFilter().getVoidList().stream().anyMatch(itemStack -> itemStack.isSimilar(item))) {
                            item.setAmount(item.getAmount() - amountToVoid);
                            break;
                        }
                    }
                }

                // Get filter endpoint
                InventoryHolder filterEndpoint = this.getFilterEndpoint(hopper);

                // Keep track of any destination containers
                List<InventoryHolder> destinationContainers = new ArrayList<>();

                // Add linked containers to the destinations
                for (Location linkedContainerLocation : linkedContainers) {
                    // Make sure the destination chunk is loaded.
                    if (!linkedContainerLocation.getWorld().isChunkLoaded(linkedContainerLocation.getBlockX() >> 4,
                            linkedContainerLocation.getBlockZ() >> 4))
                        continue;

                    // Get the destination block.
                    Block destinationBlock = linkedContainerLocation.getBlock();

                    // Get the destination state.
                    BlockState blockState = destinationBlock.getState();

                    // Remove if destination is not a inventory holder.
                    if (!(blockState instanceof InventoryHolder)) {
                        hopper.removeLinkedBlock(linkedContainerLocation);
                        continue;
                    }

                    // Add to the destination containers list
                    destinationContainers.add((InventoryHolder) blockState);
                }

                // Add storage/hopper minecarts the hopper is pointing into to the list if there aren't any destinations
                if (destinationContainers.size() < 2) {
                    destinationContainers.addAll(block.getWorld().getNearbyEntities(hopperDirection.getLocation(location).clone().add(0.5, 0.5, 0.5), 0.5, 0.5, 0.5)
                            .stream().filter(e -> e.getType() == EntityType.MINECART_CHEST || e.getType() == EntityType.MINECART_HOPPER)
                            .map(e -> (InventoryHolder) e).collect(Collectors.toSet()));
                }

                // Loop through our destination list.
                for (InventoryHolder currentDestination : destinationContainers) {

                    // Loop through all of our hoppers item slots.
                    for (int i = 0; i < 5; i++) {

                        // Get potential item to move.
                        ItemStack item = hopperContents[i];

                        // Skip if slot empty.
                        if (item == null)
                            continue;

                        // Skip if item blacklisted or void.
                        if ((this.blacklist.containsKey(hopperState) && this.blacklist.get(hopperState).isSimilar(item))
                                || blockedMaterials.contains(item.getType())
                                || hopper.getFilter().getVoidList().stream().anyMatch(itemStack -> itemStack.isSimilar(item)))
                            continue;

                        // Get amount to move.
                        int amountToMove = item.getAmount() < amount ? item.getAmount() : amount;

                        // Create item that will be moved.
                        ItemStack itemToMove = item.clone();
                        itemToMove.setAmount(amountToMove);

                        // Process whitelist and blacklist.
                        boolean blocked = (!hopper.getFilter().getWhiteList().isEmpty() && hopper.getFilter().getWhiteList().stream().anyMatch(itemStack -> itemStack.isSimilar(item))
                                || hopper.getFilter().getBlackList().stream().anyMatch(itemStack -> itemStack.isSimilar(item)));

                        // If blocked check to see if a movement can be made if blacklist skip to the next slot
                        // otherwise set the current destination to the endpoint.
                        if (blocked) {
                            if (filterEndpoint == null || !this.canMove(filterEndpoint.getInventory(), itemToMove))
                                continue;
                            currentDestination = filterEndpoint;
                        }

                        // Get the material of the destination
                        Material destinationMaterial = currentDestination instanceof BlockState ? ((BlockState) currentDestination).getType() : Material.AIR;

                        // Add item to container and continue on success.
                        if (this.addItem(hopper, hopperState, currentDestination, destinationMaterial, item, itemToMove, amountToMove))
                            continue main;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // Clear out invalid hoppers
        HopperManager hopperManager = plugin.getHopperManager();
        toRemove.forEach(hopperManager::removeHopper);

        // Empty blacklist in preparation for next cycle.
        this.blacklist.clear();
    }

    private boolean addItem(com.songoda.epichoppers.hopper.Hopper hopper, InventoryHolder currentHolder, InventoryHolder currentDestination,
                            Material destinationType, ItemStack item, ItemStack itemToMove, int amountToMove) {

        Inventory destinationInventory = currentDestination.getInventory();

        // Don't transfer shulker boxes into other shulker boxes, that's a bad idea.
        if (destinationType.name().contains("SHULKER_BOX") && item.getType().name().contains("SHULKER_BOX"))
            return false;

        switch (destinationType.name()) {
            case "ENDER_CHEST":
                OfflinePlayer op = Bukkit.getOfflinePlayer(hopper.getPlacedBy());

                if (op.isOnline())
                    destinationInventory = op.getPlayer().getEnderChest();
                break;
            case "BREWING_STAND": {
                BrewerInventory brewerInventory = (BrewerInventory) destinationInventory;

                int maxSize = itemToMove.getMaxStackSize();

                String typeStr = item.getType().name().toUpperCase();
                boolean isBottle = typeStr.contains("POTION") || typeStr.contains("BOTTLE");
                boolean isLeft = item.getType() == Material.BLAZE_POWDER;

                Map<Integer, ItemStack> output = new HashMap<>();
                if (isBottle) {
                    output.put(0, brewerInventory.getItem(0));
                    output.put(1, brewerInventory.getItem(1));
                    output.put(2, brewerInventory.getItem(2));
                } else if (isLeft) {
                    output.put(4, brewerInventory.getItem(4));
                } else {
                    output.put(3, brewerInventory.getItem(3));
                }

                for (Map.Entry<Integer, ItemStack> entry : output.entrySet()) {
                    ItemStack currentOutput = entry.getValue();
                    int currentOutputAmount = currentOutput == null ? 0 : currentOutput.getAmount();
                    if (currentOutput != null && (!currentOutput.isSimilar(itemToMove))
                            || currentOutputAmount + amountToMove > maxSize) continue;

                    if (currentOutput != null) {
                        currentOutput.setAmount(currentOutputAmount + amountToMove);
                    } else {
                        currentOutput = itemToMove;
                    }

                    brewerInventory.setItem(entry.getKey(), currentOutput);
                }
                this.debt(item, amountToMove, currentHolder);
                return true;
            }
            case "SMOKER":
            case "BLAST_FURNACE":
            case "BURNING_FURNACE":
            case "FURNACE": {
                FurnaceInventory furnaceInventory = (FurnaceInventory) destinationInventory;

                boolean isFuel = (plugin.isServerVersionAtLeast(ServerVersion.V1_13) ? item.getType().isFuel() : Methods.isLegacyFuel(item.getType())) && !item.getType().name().contains("LOG");
                ItemStack output = isFuel ? furnaceInventory.getFuel() : furnaceInventory.getSmelting();
                if (output != null && !output.isSimilar(itemToMove)) return false;
                int maxSize = itemToMove.getMaxStackSize();
                int currentOutputAmount = output == null ? 0 : output.getAmount();

                if (currentOutputAmount + amountToMove <= maxSize) {
                    if (output != null) {
                        output.setAmount(currentOutputAmount + amountToMove);
                    } else {
                        output = itemToMove;
                    }
                    if (isFuel) {
                        furnaceInventory.setFuel(output);
                    } else {
                        furnaceInventory.setSmelting(output);
                    }
                    this.debt(item, amountToMove, currentHolder);
                }
                return true;
            }
        }

        // Continue if move would fail.
        if (!this.canMove(destinationInventory, itemToMove))
            return false;

        // Prevent item from being moved again during this cycle.
        // Only block if the hopper being transfered into doesn't already contain the same item.
        // Don't blacklist if the block is transfering items into itself
        if (!destinationInventory.contains(itemToMove) && currentDestination != currentHolder && currentHolder instanceof Hopper)
            this.blacklist.put(currentDestination, itemToMove);

        // Move item to destination.
        destinationInventory.addItem(itemToMove);

        // Debt hopper
        this.debt(item, amountToMove, currentHolder);

        // Update comparators for destination block.
        if (currentDestination instanceof BlockState)
            updateAdjacentComparators(((BlockState) currentDestination).getLocation());

        // Update comparators for current hopper.
        updateAdjacentComparators(hopper.getLocation());

        // Continue to next hopper.
        return true;
    }

    private void debt(ItemStack item, int amountToMove, InventoryHolder currentHolder) {
        if (item.getAmount() - amountToMove > 0)
            item.setAmount(item.getAmount() - amountToMove);
        else
            currentHolder.getInventory().removeItem(item);
    }

    private InventoryHolder getFilterEndpoint(com.songoda.epichoppers.hopper.Hopper hopper) {
        // Get endpoint location.
        Location endPoint = hopper.getFilter().getEndPoint();

        // Check for null.
        if (hopper.getFilter().getEndPoint() == null) return null;

        // Make sure chunk is loaded.
        if (!endPoint.getWorld().isChunkLoaded(endPoint.getBlockX() >> 4, endPoint.getBlockZ() >> 4))
            return null;

        // Cast to state.
        BlockState state = endPoint.getBlock().getState();

        //Remove if not a container.
        if (!(state instanceof InventoryHolder)) {
            hopper.getFilter().setEndPoint(null);
            return null;
        }

        // Cast and return as InventoryHolder.
        return (InventoryHolder) state;
    }

    private static Class<?> clazzCraftWorld, clazzCraftBlock, clazzBlockPosition;
    private static Method getHandle, updateAdjacentComparators, getNMSBlock;

    public static void updateAdjacentComparators(Location location) {
        try {
            // Cache reflection.
            if (clazzCraftWorld == null) {
                String ver = Bukkit.getServer().getClass().getPackage().getName().substring(23);
                clazzCraftWorld = Class.forName("org.bukkit.craftbukkit." + ver + ".CraftWorld");
                clazzCraftBlock = Class.forName("org.bukkit.craftbukkit." + ver + ".block.CraftBlock");
                clazzBlockPosition = Class.forName("net.minecraft.server." + ver + ".BlockPosition");
                Class<?> clazzWorld = Class.forName("net.minecraft.server." + ver + ".World");
                Class<?> clazzBlock = Class.forName("net.minecraft.server." + ver + ".Block");

                getHandle = clazzCraftWorld.getMethod("getHandle");
                updateAdjacentComparators = clazzWorld.getMethod("updateAdjacentComparators", clazzBlockPosition, clazzBlock);
                getNMSBlock = clazzCraftBlock.getDeclaredMethod("getNMSBlock");
                getNMSBlock.setAccessible(true);
            }

            // invoke and cast objects.
            Object craftWorld = clazzCraftWorld.cast(location.getWorld());
            Object world = getHandle.invoke(craftWorld);
            Object craftBlock = clazzCraftBlock.cast(location.getBlock());

            // Invoke final method.
            updateAdjacentComparators
                    .invoke(world, clazzBlockPosition.getConstructor(double.class, double.class, double.class)
                                    .newInstance(location.getX(), location.getY(), location.getZ()),
                            getNMSBlock.invoke(craftBlock));

        } catch (ReflectiveOperationException e) {
            e.printStackTrace();
        }
    }

    private boolean canMove(Inventory inventory, ItemStack item) {
        if (inventory.firstEmpty() != -1) return true;
        for (ItemStack stack : inventory.getContents()) {
            if (stack.isSimilar(item) && (stack.getAmount() + item.getAmount()) - 1 < stack.getMaxStackSize()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Gets a set of slots that can be pulled from based on the given material
     * @param material The material to get pullable slots for
     * @return A set of valid pullable slots
     */
    private int[] getPullableSlots(InventoryHolder inventoryHolder, Material material) {
        if (material.name().contains("SHULKER_BOX"))
            return IntStream.rangeClosed(0, 26).toArray();

        switch (material.name()) {
            case "BARREL":
            case "CHEST":
            case "TRAPPED_CHEST":
                if (inventoryHolder.getInventory() instanceof DoubleChestInventory)
                    return IntStream.rangeClosed(0, 53).toArray();
                return IntStream.rangeClosed(0, 26).toArray();
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
     * @param entities The collection of entities
     * @return A random InventoryHolder if one exists, otherwise null
     */
    private InventoryHolder getRandomInventoryHolderFromEntities(Collection<Entity> entities) {
        List<InventoryHolder> inventoryHolders = new ArrayList<>();
        entities.stream().filter(e -> e.getType() == EntityType.MINECART_CHEST || e.getType() == EntityType.MINECART_HOPPER)
                .forEach(e -> inventoryHolders.add((InventoryHolder) e));
        if (inventoryHolders.isEmpty())
            return null;
        if (inventoryHolders.size() == 1)
            return inventoryHolders.get(0);
        return inventoryHolders.get(ThreadLocalRandom.current().nextInt(inventoryHolders.size()));
    }

    /**
     * Checks if a given block is an EpicFarming farm item
     * @param block The block to check
     * @return true if the block is a farm item, otherwise false
     */
    private boolean isFarmItem(Block block) {
        return EpicHoppers.getInstance().isEpicFarming() && com.songoda.epicfarming.EpicFarmingPlugin.getInstance().getFarmManager().getFarm(block) != null;
    }

    /**
     * Gets an EpicFarming block as an InventoryHolder
     * Needed because EpicFarming doesn't natively support having an InventoryHolder for the farm item
     *
     * @param block The block to effectively attach an InventoryHolder to
     * @return An InventoryHolder wrapping the EpicFarming inventory
     */
    private InventoryHolder getEpicFarmingItemWrapped(Block block) {
        return () -> com.songoda.epicfarming.EpicFarmingPlugin.getInstance().getFarmManager().getFarm(block).getInventory();
    }

}