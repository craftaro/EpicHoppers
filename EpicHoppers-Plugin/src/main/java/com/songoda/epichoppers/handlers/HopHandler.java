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
import org.bukkit.block.Hopper;
import org.bukkit.inventory.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

                for (Module module : hopper.getLevel().getRegisteredModules()) {
                    // Run Module
                    module.run(hopper);

                    // Add banned materials to list.
                    List<Material> materials = module.getBlockedItems(hopper);
                    if (materials == null || materials.isEmpty()) continue;
                    blockedMaterials.addAll(materials);
                }

                ItemStack[] hopperContents = hopperState.getInventory().getContents();

                if (hopper.getLinkedBlocks() == null || hopper.getLinkedBlocks().isEmpty()) continue;

                for (Location destinationLocation : hopper.getLinkedBlocks()) {
                    if (destinationLocation == null) continue;

                    if (!destinationLocation.getWorld().isChunkLoaded(destinationLocation.getBlockX() >> 4,
                            destinationLocation.getBlockZ() >> 4))
                        continue;

                    Block destinationBlock = destinationLocation.getBlock();
                    if (destinationBlock.getType() != Material.HOPPER) {
                        hopper.clearLinkedBlocks();
                        continue;
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
                            if (addItem(hopperState, hopper, destinationBlock, hopperContents[i], amount, i)) {
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
            if (!dest.getWorld().isChunkLoaded(dest.getBlockX() >> 4, dest.getBlockZ() >> 4))
                return;

            Block destinationBlock = dest.getBlock();

            if (destinationBlock.getType() != Material.HOPPER) {
                hopper.getFilter().setEndPoint(null);
                return;
            }

            addItem(hopperState, hopper, destinationBlock, item, amt, place);
        } catch (Exception e) {
            Debugger.runReport(e);
        }
    }

    private boolean addItem(Hopper hopperState, com.songoda.epichoppers.api.hopper.Hopper hopper, Block destinationBlock, ItemStack is, int amt, int place) {
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

            InventoryHolder outputContainer = (InventoryHolder) destinationBlock.getState();

            if (destinationBlock.getType() == Material.BREWING_STAND) {
                BrewerInventory brewerInventory = (BrewerInventory) outputContainer.getInventory();

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
            } else if (destinationBlock.getType() == Material.FURNACE || destinationBlock.getType() == Material.BURNING_FURNACE) {
                FurnaceInventory furnaceInventory = (FurnaceInventory) outputContainer.getInventory();

                boolean isFuel = Methods.isFuel(item.getType());
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
            if (!canMove(outputContainer.getInventory(), newItem)) return false;
            ItemStack finalIt = it;
            if (ovoid.stream().noneMatch(itemStack -> itemStack.isSimilar(finalIt))) {
                outputContainer.getInventory().addItem(newItem);
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
}