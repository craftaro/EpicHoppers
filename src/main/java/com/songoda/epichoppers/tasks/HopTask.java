package com.songoda.epichoppers.tasks;

import com.songoda.epichoppers.EpicHoppers;
import com.songoda.epichoppers.boost.BoostData;
import com.songoda.epichoppers.hopper.levels.modules.Module;
import com.songoda.epichoppers.utils.HopperDirection;
import com.songoda.epichoppers.utils.Methods;
import com.songoda.epichoppers.utils.ServerVersion;
import com.songoda.epichoppers.utils.settings.Setting;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Hopper;
import org.bukkit.inventory.BrewerInventory;
import org.bukkit.inventory.FurnaceInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Created by songoda on 3/14/2017.
 */
public class HopTask extends BukkitRunnable {

    // Hop to the bop to the be bop top.

    private static EpicHoppers plugin;

    private final Map<InventoryHolder, ItemStack> blacklist = new HashMap<>();

    public HopTask(EpicHoppers plug) {
        plugin = plug;
        runTaskTimer(plugin, 0, Setting.HOP_TICKS.getInt());
    }

    @Override
    public void run() {
        Collection<com.songoda.epichoppers.hopper.Hopper> hoppers = plugin.getHopperManager().getHoppers().values();
        Iterator<com.songoda.epichoppers.hopper.Hopper> itr = hoppers.iterator();

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
                    plugin.getHopperManager().removeHopper(location);
                    continue;
                }

                // If hopper block is powered continue.
                if (block.isBlockPowered() || block.isBlockIndirectlyPowered())
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

                // Fetch all hopper contents.
                ItemStack[] hopperContents = hopperState.getInventory().getContents();

                // Get filter endpoint
                InventoryHolder filterEndpoint = getFilterEndpoint(hopper);

                // Loop through our container list.
                for (Location destinationLocation : linkedContainers) {

                    // Make sure the destination chunk is loaded.
                    if (!destinationLocation.getWorld().isChunkLoaded(destinationLocation.getBlockX() >> 4,
                            destinationLocation.getBlockZ() >> 4))
                        continue;

                    // Get the destination block.
                    Block destinationBlock = destinationLocation.getBlock();

                    // Get the destination state.
                    BlockState blockState = destinationBlock.getState();

                    // Remove if destination is not a inventory holder.
                    if (!(blockState instanceof InventoryHolder)) {
                        hopper.removeLinkedBlock(destinationLocation);
                        continue;
                    }

                    // Cast blockState to container
                    InventoryHolder destinationContainer = ((InventoryHolder) blockState);

                    // Loop through all of our hoppers item slots.
                    for (int i = 0; i < 5; i++) {

                        // Skip if slot empty.
                        if (hopperContents[i] == null)
                            continue;

                        // Get potential item to move.
                        ItemStack item = hopperContents[i];

                        // Skip if item blacklisted.
                        System.out.println(blacklist.size() + " | " + blacklist.containsKey(hopperState) + " | " + (blacklist.containsKey(hopperState) && blacklist.get(hopperState).isSimilar(item)));
                        System.out.println("Blacklist check: " + hopperState);
                        if ((blacklist.containsKey(hopperState) && blacklist.get(hopperState).isSimilar(item)) || blockedMaterials.contains(item.getType())) {
                            destinationLocation.getWorld().spawnParticle(Particle.CRIT, destinationLocation.clone().add(0.5, 1, 0.5), 10);
                            continue;
                        }

                        // Get amount to move.
                        int amountToMove = item.getAmount() < amount ? item.getAmount() : amount;

                        // Create item that will be moved.
                        ItemStack itemToMove = item.clone();
                        itemToMove.setAmount(amountToMove);

                        // Process void.
                        if (hopper.getFilter().getVoidList().stream().anyMatch(itemStack -> itemStack.isSimilar(item))) {
                            item.setAmount(item.getAmount() - amountToMove);
                            continue;
                        }

                        // Set current destination.
                        InventoryHolder currentDestination = destinationContainer;

                        // Process whitelist and blacklist.
                        boolean blocked = (!hopper.getFilter().getWhiteList().isEmpty() && hopper.getFilter().getWhiteList().stream().noneMatch(itemStack -> itemStack.isSimilar(item))
                                || hopper.getFilter().getBlackList().stream().anyMatch(itemStack -> itemStack.isSimilar(item)));

                        // If blocked check to see if a movement can be made if blacklist skip to the next slot
                        // otherwise set the current destination to the endpoint.
                        if (blocked) {
                            if (filterEndpoint == null || !canMove(filterEndpoint.getInventory(), itemToMove))
                                break;
                            currentDestination = filterEndpoint;
                        }

                        // Add item to container and continue on success.
                        if (addItem(hopper, hopperState, currentDestination, destinationBlock.getType(), item, itemToMove, amountToMove))
                            continue main;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // Empty blacklist in preparation for next cycle.
        this.blacklist.clear();
    }

    private boolean addItem(com.songoda.epichoppers.hopper.Hopper hopper, InventoryHolder currentHolder, InventoryHolder currentDestination,
                            Material destinationType, ItemStack item, ItemStack itemToMove, int amountToMove) {

        Inventory destinationInventory = currentDestination.getInventory();

        switch (destinationType.name()) {
            case "ENDER_CHEST":
                OfflinePlayer op = Bukkit.getOfflinePlayer(hopper.getPlacedBy());

                if (op.isOnline())
                    destinationInventory = op.getPlayer().getEnderChest();
                break;
            case "BLACK_SHULKER_BOX":
            case "BLUE_SHULKER_BOX":
            case "BROWN_SHULKER_BOX":
            case "CYAN_SHULKER_BOX":
            case "GRAY_SHULKER_BOX":
            case "GREEN_SHULKER_BOX":
            case "LIGHT_BLUE_SHULKER_BOX":
            case "LIGHT_GRAY_SHULKER_BOX":
            case "LIME_SHULKER_BOX":
            case "MAGENTA_SHULKER_BOX":
            case "ORANGE_SHULKER_BOX":
            case "PINK_SHULKER_BOX":
            case "PURPLE_SHULKER_BOX":
            case "RED_SHULKER_BOX":
            case "SHULKER_BOX":
            case "WHITE_SHULKER_BOX":
            case "YELLOW_SHULKER_BOX":
                return false;
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
                debt(item, amountToMove, currentHolder);
                return true;
            }
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
                    debt(item, amountToMove, currentHolder);
                }
                return true;
            }
        }

        // Continue if move would fail.
        if (!canMove(destinationInventory, itemToMove))
            return false;

        // Prevent item from being moved again during this cycle.
        // Only block if the hopper being transfered into doesn't already contain the same item.
        if (!destinationInventory.contains(itemToMove))
            this.blacklist.put(currentDestination, itemToMove);

        // Move item to destination.
        destinationInventory.addItem(itemToMove);

        // Debt hopper
        debt(item, amountToMove, currentHolder);

        // Update comparators for destination hopper.
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

}