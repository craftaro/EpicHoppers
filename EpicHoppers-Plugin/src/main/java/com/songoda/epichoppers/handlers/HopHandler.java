package com.songoda.epichoppers.handlers;

import com.songoda.arconix.plugin.Arconix;
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
import org.bukkit.configuration.ConfigurationSection;
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
            Bukkit.getScheduler().scheduleSyncDelayedTask(instance, () -> {
                hopperCleaner();
                Bukkit.getScheduler().scheduleSyncRepeatingTask(instance, this::hopperRunner, 0, instance.getConfig().getLong("Main.Amount of Ticks Between Hops"));
            }, 40L);
        } catch (Exception e) {
            Debugger.runReport(e);
        }
    }

    private void hopperCleaner() {
        try {
            ConfigurationSection data = instance.getConfig().createSection("data");
            if (!data.contains("link")) return;
            for (String key : data.getConfigurationSection("link").getKeys(false)) {
                if (Arconix.pl().getApi().serialize().unserializeLocation(key).getWorld() == null) continue;
                Block block = Arconix.pl().getApi().serialize().unserializeLocation(key).getBlock();
                if (block != null && block.getState() instanceof Hopper) continue;
                data.getConfigurationSection("link").set(key, null);
                instance.getLogger().info("EpicHoppers Removing non-hopper entry: " + key);
            }
        } catch (Exception e) {
            Debugger.runReport(e);
        }
    }

    private void hopperRunner() {
        try {
            main:
            for (com.songoda.epichoppers.api.hopper.Hopper hopper : instance.getHopperManager().getHoppers().values()) {

                Location location = hopper.getLocation();

                int x = location.getBlockX() >> 4;
                int z = location.getBlockZ() >> 4;

                try {
                    if (!location.getWorld().isChunkLoaded(x, z)) {
                        continue;
                    }
                } catch (Exception e) {
                    continue;
                }
                Block block = location.getBlock();

                if (block.isBlockPowered()) continue;

                if (block == null || block.getType() != Material.HOPPER) {
                    instance.getHopperManager().removeHopper(location);
                    continue;
                }

                Hopper hopperBlock = hopper.getHopper();

                ItemStack[] is = hopperBlock.getInventory().getContents();

                List<Material> blockedMaterials = new ArrayList<>();

                for (Module module : hopper.getLevel().getRegisteredModules()) {
                    // Run Module
                    module.run(hopper);

                    // Add banned materials to list.
                    List<Material> materials = module.getBlockedItems(hopper);
                    if (materials == null || materials.isEmpty()) continue;
                    blockedMaterials.addAll(materials);
                }

                if (hopper.getLinkedBlocks() == null || hopper.getLinkedBlocks().isEmpty()) continue;

                for (Block destBlock : hopper.getLinkedBlocks()) {
                    Location dest = destBlock.getLocation();
                    if (dest == null) continue;

                    int destx = location.getBlockX() >> 4;
                    int destz = location.getBlockZ() >> 4;
                    if (!dest.getWorld().isChunkLoaded(destx, destz)) {
                        continue;
                    }

                    Block b2 = dest.getBlock();
                    if (!(b2.getState() instanceof InventoryHolder)) {
                        hopper.clearLinkedBlocks();
                        continue;
                    }

                    //InventoryHolder inventoryHolder = (InventoryHolder) b2.getState();
                    //TODO add some restrictions here if needed

                    BoostData boostData = instance.getBoostManager().getBoost(hopper.getPlacedBy());

                    int amt = hopper.getLevel().getAmount() * (boostData == null ? 1 : boostData.getMultiplier());

                    List<ItemStack> whiteList = hopper.getFilter().getWhiteList();

                    List<ItemStack> blackList = hopper.getFilter().getBlackList();

                    for (int i = 0; i < 5; i++) {
                        ItemStack it;
                        if (is[i] != null) {
                            it = is[i].clone();
                            it.setAmount(1);
                        }
                        if (hopper.getLocation().getBlock().isBlockPowered()
                                || is[i] != null && blockedMaterials.contains(is[i].getType())) {
                            i++;
                            if (i >= 5) continue;
                        }

                        int finalI = i;
                        if (is[i] != null
                                && !whiteList.isEmpty()
                                && whiteList.stream().noneMatch(itemStack -> itemStack.isSimilar(is[finalI]))) {
                            doBlacklist(hopperBlock, hopper, is[i].clone(), is, amt, i);
                            continue main;
                        } else {
                            if (is[i] != null && blackList.stream().noneMatch(itemStack -> itemStack.isSimilar(is[finalI]))) {

                                if (addItem(hopperBlock, hopper, b2, is[i], is, amt, i)) {
                                    continue main;
                                }
                            } else {
                                if (is[i] != null && blackList.stream().anyMatch(itemStack -> itemStack.isSimilar(is[finalI]))) {
                                    doBlacklist(hopperBlock, hopper, is[i].clone(), is, amt, i);
                                    continue main;
                                }
                            }
                        }

                    }
                }
            }
        } catch (Exception e) {
            Debugger.runReport(e);
        }
    }


    private void doBlacklist(Hopper hopperBlock, com.songoda.epichoppers.api.hopper.Hopper hopper, ItemStack item, ItemStack[] isS, int amt, int place) {
        try {
            Location loc = hopperBlock.getLocation();
            Block b = loc.getBlock();
            if (hopper.getFilter().getEndPoint() != null
                    && b != null && b.getState() instanceof Hopper) {
                Location dest = hopper.getFilter().getEndPoint().getLocation();
                int destx = loc.getBlockX() >> 4;
                int destz = loc.getBlockZ() >> 4;
                if (!dest.getWorld().isChunkLoaded(destx, destz)) {
                    return;
                }
                Block b2 = dest.getBlock();

                addItem(hopperBlock, hopper, b2, item, isS, amt, place);

            }
        } catch (Exception e) {
            Debugger.runReport(e);
        }
    }

    private boolean addItem(Hopper hopperBlock, com.songoda.epichoppers.api.hopper.Hopper hopper, Block b2, ItemStack is, ItemStack[] isS, int amt, int place) {
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

            if (b2.getType().equals(Material.ENDER_CHEST)) {
                OfflinePlayer op = Bukkit.getOfflinePlayer(hopper.getPlacedBy());
                if (op.isOnline() && canMove(op.getPlayer().getEnderChest(), newItem, amt)) {
                    if (!ovoid.contains(it.getType())) {
                        op.getPlayer().getEnderChest().addItem(newItem);
                    }
                    isS[place] = is;
                    hopperBlock.getInventory().setContents(isS);
                }
            } else {
                InventoryHolder outputContainer = (InventoryHolder) b2.getState();
                if (b2.getType() == Material.BREWING_STAND) {
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

                    for (Map.Entry<Integer, ItemStack> entry : output.entrySet()) {
                        ItemStack currentOutput = entry.getValue();
                        int currentOutputAmount = currentOutput == null ? 0 : currentOutput.getAmount();
                        if (currentOutput != null && (!currentOutput.isSimilar(newItem))
                                || currentOutputAmount + newItem.getAmount() > maxSize
                                || ovoid.contains(it.getType())) continue;

                        if (currentOutput != null) {
                            currentOutput.setAmount(currentOutputAmount + newItem.getAmount());
                        } else {
                            currentOutput = newItem.clone();
                        }

                        brewerInventory.setItem(entry.getKey(), currentOutput);

                        isS[place] = is;
                        hopperBlock.getInventory().setContents(isS);
                        return true;
                    }
                } else if (b2.getType() == Material.FURNACE) {
                    FurnaceInventory furnaceInventory = (FurnaceInventory) outputContainer.getInventory();

                    boolean isFuel = item.getType().isFuel();
                    ItemStack output = isFuel ? furnaceInventory.getFuel() : furnaceInventory.getSmelting();
                    if (output != null && !output.isSimilar(newItem)) return false;
                    int maxSize = newItem.getMaxStackSize();
                    int currentOutputAmount = output == null ? 0 : output.getAmount();

                    if (currentOutputAmount + newItem.getAmount() <= maxSize) {
                        if (!ovoid.contains(it.getType())) {
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
                            isS[place] = is;
                            hopperBlock.getInventory().setContents(isS);
                        }
                    }
                    return true;
                } else {
                    if (!canMove(outputContainer.getInventory(), newItem, amt)) return false;
                    ItemStack finalIt = it;
                    if (ovoid.stream().noneMatch(itemStack -> itemStack.isSimilar(finalIt))) {
                        outputContainer.getInventory().addItem(newItem);
                    }
                    isS[place] = is;
                    hopperBlock.getInventory().setContents(isS);
                }
            }
            return true;
        } catch (Exception e) {
            Debugger.runReport(e);
        }
        return false;
    }

    private boolean canMove(Inventory inventory, ItemStack item, int hop) {
        try {
            if (inventory.firstEmpty() != -1) return true;

            for (ItemStack stack : inventory.getContents()) {
                if (stack.isSimilar(item) && (stack.getAmount() + item.getAmount() + hop) < stack.getMaxStackSize()) {
                    return true;
                }
            }
        } catch (Exception e) {
            Debugger.runReport(e);
        }
        return false;
    }
}