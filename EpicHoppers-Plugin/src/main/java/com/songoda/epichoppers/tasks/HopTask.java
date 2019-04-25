package com.songoda.epichoppers.tasks;

import com.songoda.epichoppers.EpicHoppersPlugin;
import com.songoda.epichoppers.api.hopper.levels.modules.Module;
import com.songoda.epichoppers.boost.BoostData;
import com.songoda.epichoppers.utils.SettingsManager;
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
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

/**
 * Created by songoda on 3/14/2017.
 */
public class HopTask extends BukkitRunnable {

    private static EpicHoppersPlugin plugin;

    public HopTask(EpicHoppersPlugin plug) {
        plugin = plug;
        runTaskTimer(plugin, 0, SettingsManager.Setting.HOP_TICKS.getInt());
    }

    @Override
    public void run() {
        main:
        for (com.songoda.epichoppers.api.hopper.Hopper hopper : new HashMap<>(plugin.getHopperManager().getHoppers()).values()) {
            Location location = hopper.getLocation();

            if (location.getWorld() == null || !location.getWorld().isChunkLoaded(location.getBlockX() >> 4, location.getBlockZ() >> 4))
                continue;

            Block block = location.getBlock();

            if (block.getType() != Material.HOPPER) {
                plugin.getHopperManager().removeHopper(location);
                continue;
            }

            if (block.isBlockPowered() || block.isBlockIndirectlyPowered()) continue;

            Hopper hopperState = (Hopper) block.getState();
            Inventory hopperInventory = hopperState.getInventory();

            List<Material> blockedMaterials = new ArrayList<>();

            for (Module module : hopper.getLevel().getRegisteredModules()) {
                // Run Module
                module.run(hopper, hopperInventory);

                // Add banned materials to list.
                List<Material> materials = module.getBlockedItems(hopper);
                if (materials == null || materials.isEmpty()) continue;
                blockedMaterials.addAll(materials);
            }

            // Fetch all hopper contents.
            ItemStack[] hopperContents = hopperInventory.getContents();

            Inventory override = null;
            List<Location> linked = hopper.getLinkedBlocks();

            if (hopper.getLinkedBlocks() == null || hopper.getLinkedBlocks().isEmpty()) {
                HopperDirection hopperDirection = HopperDirection.getDirection(hopperState.getRawData());
                Location check = hopperDirection.getLocation(location);

                linked.add(check);

                Collection<Entity> nearbyEntities = hopper.getLocation().getWorld().getNearbyEntities(check, .5, .5, .5);

                for (Entity entity : nearbyEntities) {
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

                BoostData boostData = plugin.getBoostManager().getBoost(hopper.getPlacedBy());

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
                        doBlacklist(hopperInventory, hopper, hopperContents[i].clone(), amount, i);
                        continue main;
                    }

                    if (blackList.stream().noneMatch(itemStack -> itemStack.isSimilar(hopperContents[finalIncrement]))) {
                        if (addItem(hopperInventory, hopper, destinationInventory, destinationBlock, hopperContents[i], amount, i)) {
                            continue main;
                        }
                    } else {
                        if (hopper.getFilter().getEndPoint() == null) continue;

                        doBlacklist(hopperInventory, hopper, hopperContents[i].clone(), amount, i);
                        continue main;
                    }
                }
            }
        }
    }


    private void doBlacklist(Inventory hopperInventory, com.songoda.epichoppers.api.hopper.Hopper hopper, ItemStack item, int amt, int place) {
        Location dest = hopper.getFilter().getEndPoint();
        if (!dest.getWorld().isChunkLoaded(dest.getBlockX() >> 4, dest.getBlockZ() >> 4))
            return;

        Block destinationBlock = dest.getBlock();
        BlockState state = destinationBlock.getState();
        if (!(state instanceof InventoryHolder)) {
            hopper.getFilter().setEndPoint(null);
            return;
        }
        Inventory destinationInventory = ((InventoryHolder) state).getInventory();

        addItem(hopperInventory, hopper, destinationInventory, destinationBlock, item, amt, place);
    }

    private boolean addItem(Inventory hopperInventory, com.songoda.epichoppers.api.hopper.Hopper hopper, Inventory destinationInventory, Block destinationBlock, ItemStack is, int amt, int place) {
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
                hopperInventory.setItem(place, is);
            }
            return true;
        }

        switch (destinationBlock.getType()) {
            case BLACK_SHULKER_BOX:
            case BLUE_SHULKER_BOX:
            case BROWN_SHULKER_BOX:
            case CYAN_SHULKER_BOX:
            case GRAY_SHULKER_BOX:
            case GREEN_SHULKER_BOX:
            case LIGHT_BLUE_SHULKER_BOX:
            case LIGHT_GRAY_SHULKER_BOX:
            case LIME_SHULKER_BOX:
            case MAGENTA_SHULKER_BOX:
            case ORANGE_SHULKER_BOX:
            case PINK_SHULKER_BOX:
            case PURPLE_SHULKER_BOX:
            case RED_SHULKER_BOX:
            case SHULKER_BOX:
            case WHITE_SHULKER_BOX:
            case YELLOW_SHULKER_BOX:
                return false;
            case BREWING_STAND: {
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
                    hopperInventory.setItem(place, is);
                    return true;
                }
                break;
            }
            case FURNACE: {
                FurnaceInventory furnaceInventory = (FurnaceInventory) destinationInventory;

                boolean isFuel = item.getType().isFuel() && !item.getType().name().contains("LOG");
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
                        hopperInventory.setItem(place, is);
                    }
                }
                return true;
            }
        }

        if (!canMove(destinationInventory, newItem)) return false;
        ItemStack finalIt = it;
        if (ovoid.stream().noneMatch(itemStack -> itemStack.isSimilar(finalIt))) {
            destinationInventory.addItem(newItem);
        }
        hopperInventory.setItem(place, is);
        return true;
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