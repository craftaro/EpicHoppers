package com.songoda.epichoppers.handlers;

import com.songoda.epichoppers.EpicHoppersPlugin;
import com.songoda.epichoppers.api.hopper.levels.modules.Module;
import com.songoda.epichoppers.boost.BoostData;
import com.songoda.epichoppers.utils.Debugger;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Hopper;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.minecart.HopperMinecart;
import org.bukkit.entity.minecart.StorageMinecart;
import org.bukkit.inventory.*;

import java.util.*;

/**
 * Created by songoda on 3/14/2017.
 */
public class HopHandler {

    private EpicHoppersPlugin instance;

    public HopHandler(EpicHoppersPlugin instance) {
        try {
            this.instance = instance;
            Bukkit.getScheduler().scheduleSyncDelayedTask(instance, () ->
                    Bukkit.getScheduler().scheduleSyncRepeatingTask(instance, this::hopperRunner, 0,
                            instance.getConfig().getLong("Main.Amount of Ticks Between Hops")), 40L);
        } catch (Exception e) {
            Debugger.runReport(e);
        }
    }

    private void hopperRunner() {
        try {
            main:
            for (com.songoda.epichoppers.api.hopper.Hopper hopper : new HashMap<>(instance.getHopperManager().getHoppers()).values()) {
                Location location = hopper.getLocation();

                if (!location.getWorld().isChunkLoaded(location.getBlockX() >> 4, location.getBlockZ() >> 4))
                    continue;

                Block block = location.getBlock();

                if (block == null || block.getType() != Material.HOPPER) {
                    instance.getHopperManager().removeHopper(location);
                    continue;
                }

                if (block.isBlockPowered() || block.isBlockIndirectlyPowered()) continue;

                Hopper hopperState = (Hopper) block.getState();

                List<Material> blockedMaterials = new ArrayList<>();

                if (hopperState == null || hopperState.getInventory() == null) continue;

                for (Module module : hopper.getLevel().getRegisteredModules()) {
                    // Run Module
                    module.run(hopper);

                    // Add banned materials to list.
                    List<Material> materials = module.getBlockedItems(hopper);
                    if (materials == null || materials.isEmpty()) continue;
                    blockedMaterials.addAll(materials);
                }

                ItemStack[] hopperContents = hopperState.getInventory().getContents();

                Inventory override = null;
                List<Location> linked = hopper.getLinkedBlocks();

                if (hopper.getLinkedBlocks() == null || hopper.getLinkedBlocks().isEmpty()) {
                    HopperDirection hopperDirection = HopperDirection.getDirection(hopperState.getRawData());
                    Location check = hopperDirection.getLocation(location);

                    linked.add(check);

                    Collection<Entity> nearbyEntite = hopper.getLocation().getWorld().getNearbyEntities(check, .5, .5, .5);

                    for (Entity entity : nearbyEntite) {
                        if (entity.getType() == EntityType.MINECART_HOPPER)
                            override = ((HopperMinecart) entity).getInventory();
                        else if (entity.getType() == EntityType.MINECART_CHEST)
                            override = ((StorageMinecart) entity).getInventory();
                    }

                    if (linked.isEmpty()) continue;
                }

                for (Location destinationLocation : linked) {
                    Block destinationBlock = destinationLocation.getBlock();
                    Inventory destinationInventory = override;
                    if (override == null) {
                        if (destinationLocation == null) continue;

                        if (!destinationLocation.getWorld().isChunkLoaded(destinationLocation.getBlockX() >> 4,
                                destinationLocation.getBlockZ() >> 4))
                            continue;

                        destinationBlock = destinationLocation.getBlock();
                        BlockState state = destinationBlock.getState();
                        if (!(state instanceof InventoryHolder)) {
                            hopper.clearLinkedBlocks();
                            continue;
                        }
                        destinationInventory = ((InventoryHolder) state).getInventory();
                    }

                    BoostData boostData = instance.getBoostManager().getBoost(hopper.getPlacedBy());

                    int amount = hopper.getLevel().getAmount() * (boostData == null ? 1 : boostData.getMultiplier());

                    List<ItemStack> whiteList = hopper.getFilter().getWhiteList();
                    List<ItemStack> blackList = hopper.getFilter().getBlackList();

                    for (int i = 0; i < 5; i++) {
                        if (hopperContents[i] == null) continue;

                        ItemStack item = hopperContents[i].clone();
                        item.setAmount(1);

                        if (hopper.getLocation().getBlock().isBlockPowered()
                                || hopperContents[i] != null && blockedMaterials.contains(hopperContents[i].getType())) {
                            continue;
                        }

                        int finalIncrement = i;

                        if (!whiteList.isEmpty()
                                && whiteList.stream().noneMatch(itemStack -> itemStack.isSimilar(hopperContents[finalIncrement]))) {
                            doBlacklist(hopperState, hopper, hopperContents[i].clone(), amount, i);
                            continue main;
                        }

                        if (blackList.stream().noneMatch(itemStack -> itemStack.isSimilar(hopperContents[finalIncrement]))) {
                            if (addItem(hopperState, hopper, destinationInventory, destinationBlock, hopperContents[i], amount, i)) {
                                continue main;
                            }
                        }

                        if (blackList.stream().anyMatch(itemStack -> itemStack.isSimilar(hopperContents[finalIncrement]))) {
                            doBlacklist(hopperState, hopper, hopperContents[i].clone(), amount, i);
                            continue main;
                        }
                    }
                }
            }
        } catch (Exception e) {
            Debugger.runReport(e);
        }
    }


    private void doBlacklist(Hopper hopperState, com.songoda.epichoppers.api.hopper.Hopper hopper, ItemStack item, int amt, int place) {
        try {
            Location dest = hopper.getFilter().getEndPoint();
            if (dest == null) return;
            if (!dest.getWorld().isChunkLoaded(dest.getBlockX() >> 4, dest.getBlockZ() >> 4))
                return;

            Block destinationBlock = dest.getBlock();
            BlockState state = destinationBlock.getState();
            if (!(state instanceof InventoryHolder)) {
                hopper.getFilter().setEndPoint(null);
                return;
            }
            Inventory destinationInventory = ((InventoryHolder) state).getInventory();

            addItem(hopperState, hopper, destinationInventory, destinationBlock, item, amt, place);
        } catch (Exception e) {
            Debugger.runReport(e);
        }
    }

    private boolean addItem(Hopper hopperState, com.songoda.epichoppers.api.hopper.Hopper hopper, Inventory destinationInventory, Block destinationBlock, ItemStack is, int amt, int place) {
        try {
            ItemStack it = null;
            if (is != null) {
                it = is.clone();
                it.setAmount(1);
            }

            List<ItemStack> ovoid = new ArrayList<>(hopper.getFilter().getVoidList());

            if (is.getType() == Material.AIR) {
                return true;
            }
            ItemStack item = is;
            ItemStack newItem = is.clone();

            if ((item.getAmount() - amt) <= 0) {
                amt = item.getAmount();
            }
            if ((item.getAmount() - amt) >= 1) {
                newItem.setAmount(newItem.getAmount() - amt);
                is = newItem.clone();
            } else {
                is = null;
            }

            newItem.setAmount(amt);

            if (destinationBlock.getType().equals(Material.ENDER_CHEST)) {
                OfflinePlayer op = Bukkit.getOfflinePlayer(hopper.getPlacedBy());

                if (op.isOnline() && canMove(op.getPlayer().getEnderChest(), newItem)) {
                    ItemStack finalIt = it;
                    if (ovoid.stream().noneMatch(itemStack -> itemStack.isSimilar(finalIt))) {
                        op.getPlayer().getEnderChest().addItem(newItem);
                    }
                    hopperState.getInventory().setItem(place, is);
                }
                return true;
            }

            if (destinationBlock.getType() == Material.BREWING_STAND) {
                BrewerInventory brewerInventory = (BrewerInventory) destinationInventory;

                int maxSize = newItem.getMaxStackSize();

                String typeStr = item.getType().name().toUpperCase();
                boolean isBottle = typeStr.contains("POTION") || typeStr.contains("BOTTLE") || item.getType() == Material.DRAGON_BREATH;
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

                ItemStack finalIt = it;
                for (Map.Entry<Integer, ItemStack> entry : output.entrySet()) {
                    if (ovoid.stream().noneMatch(itemStack -> itemStack.isSimilar(finalIt))) {
                        ItemStack currentOutput = entry.getValue();
                        int currentOutputAmount = currentOutput == null ? 0 : currentOutput.getAmount();
                        if (currentOutput != null && (!currentOutput.isSimilar(newItem))
                                || currentOutputAmount + newItem.getAmount() > maxSize) continue;

                        if (currentOutput != null) {
                            currentOutput.setAmount(currentOutputAmount + newItem.getAmount());
                        } else {
                            currentOutput = newItem.clone();
                        }

                        brewerInventory.setItem(entry.getKey(), currentOutput);
                    }
                    hopperState.getInventory().setItem(place, is);
                    return true;
                }
            } else if (destinationBlock.getType() == Material.FURNACE) {
                FurnaceInventory furnaceInventory = (FurnaceInventory) destinationInventory;

                boolean isFuel = item.getType().isFuel();
                ItemStack output = isFuel ? furnaceInventory.getFuel() : furnaceInventory.getSmelting();
                if (output != null && !output.isSimilar(newItem)) return false;
                int maxSize = newItem.getMaxStackSize();
                int currentOutputAmount = output == null ? 0 : output.getAmount();

                if (currentOutputAmount + newItem.getAmount() <= maxSize) {
                    ItemStack finalIt = it;
                    if (ovoid.stream().noneMatch(itemStack -> itemStack.isSimilar(finalIt))) {
                        if (output != null) {
                            output.setAmount(currentOutputAmount + newItem.getAmount());
                        } else {
                            output = newItem.clone();
                        }
                        if (isFuel) {
                            furnaceInventory.setFuel(output);
                        } else {
                            furnaceInventory.setSmelting(output);
                        }
                        hopperState.getInventory().setItem(place, is);
                    }
                }
                return true;
            }
            if (!canMove(destinationInventory, newItem)) return false;
            ItemStack finalIt = it;
            if (ovoid.stream().noneMatch(itemStack -> itemStack.isSimilar(finalIt))) {
                destinationInventory.addItem(newItem);
            }
            hopperState.getInventory().setItem(place, is);
            return true;
        } catch (Exception e) {
            Debugger.runReport(e);
        }
        return false;
    }

    private boolean canMove(Inventory inventory, ItemStack item) {
        try {
            if (inventory.firstEmpty() != -1) return true;
            for (ItemStack stack : inventory.getContents()) {
                if (stack.isSimilar(item) && (stack.getAmount() + item.getAmount()) - 1 < stack.getMaxStackSize()) {
                    return true;
                }
            }
        } catch (Exception e) {
            Debugger.runReport(e);
        }
        return false;
    }

    public enum HopperDirection {

        DOWN(0, 8, 0, -1, 0),
        NORTH(2, 10, 0, 0, -1),
        SOUTH(3, 11, 0, 0, 1),
        WEST(4, 12, -1, 0, 0),
        EAST(5, 13, 1, 0, 0);

        private int unpowered;
        private int powered;

        private int x;
        private int y;
        private int z;

        HopperDirection(int unpowered, int powered, int x, int y, int z) {
            this.unpowered = unpowered;
            this.powered = powered;

            this.x = x;
            this.y = y;
            this.z = z;
        }

        public static HopperDirection getDirection(int value) {
            for (HopperDirection hopperDirection : HopperDirection.values()) {
                if (hopperDirection.getPowered() == value
                        || hopperDirection.getUnpowered() == value) return hopperDirection;
            }
            return null;
        }

        public Location getLocation(Location location) {
            return location.add(getX(), getY(), getZ());
        }

        public int getX() {
            return x;
        }

        public int getY() {
            return y;
        }

        public int getZ() {
            return z;
        }

        public int getUnpowered() {
            return unpowered;
        }

        public int getPowered() {
            return powered;
        }
    }
}