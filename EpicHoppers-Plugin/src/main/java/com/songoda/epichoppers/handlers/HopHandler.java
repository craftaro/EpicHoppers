package com.songoda.epichoppers.handlers;

import com.songoda.arconix.plugin.Arconix;
import com.songoda.epichoppers.EpicHoppersPlugin;
import com.songoda.epichoppers.api.hopper.levels.modules.Module;
import com.songoda.epichoppers.boost.BoostData;
import com.songoda.epichoppers.utils.Debugger;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.Hopper;
import org.bukkit.configuration.ConfigurationSection;
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
            if (!data.contains("sync")) return;
            for (String key : data.getConfigurationSection("sync").getKeys(false)) {
                if (Arconix.pl().getApi().serialize().unserializeLocation(key).getWorld() == null) continue;
                Block block = Arconix.pl().getApi().serialize().unserializeLocation(key).getBlock();
                if (block != null && block.getState() instanceof Hopper) continue;
                data.getConfigurationSection("sync").set(key, null);
                instance.getLogger().info("EpicHoppers Removing non-hopper entry: " + key);
            }
        } catch (Exception e) {
            Debugger.runReport(e);
        }
    }

    private void hopperRunner() {
        try {
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


                if (block == null || !(block.getState() instanceof Hopper)) {
                    instance.getHopperManager().removeHopper(location);
                    instance.getLogger().info("EpicHoppers Removing non-hopper entry: " + location.toString());
                }

                Hopper hopperBlock = hopper.getHopper();

                ItemStack[] is = hopperBlock.getInventory().getContents();

                List<Material> materials = new ArrayList<>();

                for (Module module : hopper.getLevel().getRegisteredModules()) {

                    // Run Module
                    module.run(hopper);

                    // Add banned materials to list.
                    if (module.getBlockedItems(hopper) == null) continue;
                    materials.addAll(module.getBlockedItems(hopper));
                }


                if (hopper.getSyncedBlock() == null) continue;
                Location dest = hopper.getSyncedBlock().getLocation();
                if (dest == null) {
                    hopper.setSyncedBlock(null);
                    continue;
                }
                int destx = location.getBlockX() >> 4;
                int destz = location.getBlockZ() >> 4;
                if (!dest.getWorld().isChunkLoaded(destx, destz)) {
                    continue;
                }
                Block b2 = dest.getBlock();
                if (!b2.getType().equals(Material.HOPPER) && !b2.getType().equals(Material.CHEST) && !b2.getType().equals(Material.TRAPPED_CHEST) && !(b2.getType().equals(Material.ENDER_CHEST))) {
                    hopper.setSyncedBlock(null);
                    continue;
                }

                BoostData boostData = instance.getBoostManager().getBoost(hopper.getPlacedBy());

                int amt = hopper.getLevel().getAmount() * (boostData == null ? 1 : boostData.getMultiplier());

                List<ItemStack> whiteList = hopper.getFilter().getWhiteList();

                List<ItemStack> blackList = hopper.getFilter().getBlackList();

                int num = 0;
                while (num != 5) {
                    ItemStack it = null;
                    if (is[num] != null) {
                        it = is[num].clone();
                        it.setAmount(1);
                    }

                    if (is[num] != null
                            && materials.contains(is[num].getType())) {
                        num++;
                        continue;
                    }

                    if (is[num] != null
                            && !whiteList.isEmpty()
                            && !whiteList.contains(it)) {
                        doBlacklist(hopperBlock, hopper, is[num].clone(), is, amt, num);
                    } else if (is[num] != null && !blackList.contains(it)) {
                        int numm = addItem(hopperBlock, hopper, b2, is[num], is, amt, num);
                        if (numm != 10)
                            num = numm;
                    } else if (is[num] != null && blackList.contains(it)) {
                        doBlacklist(hopperBlock, hopper, is[num].clone(), is, amt, num);
                    }
                    num++;
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

    private int addItem(Hopper hopperBlock, com.songoda.epichoppers.api.hopper.Hopper hopper, Block b2, ItemStack is, ItemStack[] isS, int amt, int place) {
        try {
            ItemStack it = null;
            if (is != null) {
                it = is.clone();
                it.setAmount(1);
            }

            List<Material> ovoid = new ArrayList<>();

            for (ItemStack iss : hopper.getFilter().getVoidList()) {
                ovoid.add(iss.getType());
            }

            if (is.getType() == Material.AIR) {
                return 10;
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
            InventoryHolder ih = null;
            if (!b2.getType().equals(Material.ENDER_CHEST)) {
                ih = (InventoryHolder) b2.getState();
            }

            if (b2.getType().equals(Material.ENDER_CHEST)) {
                try {
                    OfflinePlayer op = Bukkit.getOfflinePlayer(UUID.fromString(instance.getDataFile().getConfig().getString("data.enderTracker." + Arconix.pl().getApi().serialize().serializeLocation(b2))));
                    if (op.isOnline() && canHop(op.getPlayer().getEnderChest(), newItem, amt)) {
                        if (!ovoid.contains(it.getType())) {
                            op.getPlayer().getEnderChest().addItem(newItem);
                        }
                        isS[place] = is;
                        hopperBlock.getInventory().setContents(isS);
                    }

                } catch (Exception ignore) {
                }
            } else {
                if (!canHop(ih.getInventory(), newItem, amt) || b2.getType() == Material.BREWING_STAND) {
                    return 4;
                }
                if (b2.getType() == Material.FURNACE) {
                    FurnaceInventory fi = (FurnaceInventory) ih.getInventory();
                    int amtt = 0;
                    boolean dont = false;
                    if (fi.getSmelting() != null) {
                        amtt = fi.getSmelting().getAmount();
                        if (fi.getSmelting().getType() != newItem.getType()) {
                            dont = true;
                        } else {
                            if (fi.getSmelting().getAmount() == fi.getSmelting().getMaxStackSize()) {
                                dont = true;
                            }
                        }
                    }
                    if (!dont) {
                        if (amtt + newItem.getAmount() <= 64) {
                            if (!ovoid.contains(it.getType())) {
                                ih.getInventory().addItem(newItem);
                            }
                            isS[place] = is;
                            hopperBlock.getInventory().setContents(isS);
                        }
                    }
                } else {
                    if (!ovoid.contains(it.getType())) {
                        ih.getInventory().addItem(newItem);
                    }
                    isS[place] = is;
                    hopperBlock.getInventory().setContents(isS);
                }
            }
            return 4;
        } catch (Exception e) {
            Debugger.runReport(e);
        }
        return 0;
    }

    public boolean canHop(Inventory i, ItemStack item, int hop) {
        try {
            if (i.firstEmpty() != -1) {
                return true;
            }
            boolean can = false;
            for (ItemStack it : i.getContents()) {
                if (it == null) {
                    can = true;
                    break;
                } else {
                    if (it.isSimilar(item)) {
                        if ((it.getAmount() + hop) <= it.getMaxStackSize()) {
                            can = true;
                            break;
                        }
                    }
                }
            }
            return can;
        } catch (Exception e) {
            Debugger.runReport(e);
        }
        return false;
    }
}