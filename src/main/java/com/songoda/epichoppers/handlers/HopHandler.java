package com.songoda.epichoppers.handlers;

import com.songoda.arconix.plugin.Arconix;
import com.songoda.epichoppers.EpicHoppers;
import com.songoda.epichoppers.utils.Debugger;
import com.songoda.epichoppers.utils.Methods;
import org.apache.commons.lang.StringUtils;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.Hopper;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.inventory.FurnaceInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;

import java.util.*;

/**
 * Created by songoda on 3/14/2017.
 */
public class HopHandler {

    private EpicHoppers instance;

    public HopHandler(EpicHoppers instance) {
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
            if (instance.dataFile.getConfig().contains("data.sync")) {
                ConfigurationSection cs = instance.dataFile.getConfig().getConfigurationSection("data.sync");
                for (String key : cs.getKeys(false)) {
                    if (Arconix.pl().getApi().serialize().unserializeLocation(key).getWorld() != null) {
                        Block b = Arconix.pl().getApi().serialize().unserializeLocation(key).getBlock();
                        if (b == null || !(b.getState() instanceof Hopper)) {
                            instance.dataFile.getConfig().getConfigurationSection("data.sync").set(key, null);
                            instance.getLogger().info("EpicHoppers Removing non-hopper entry: " + key);
                        }
                    }
                }
            }
        } catch (Exception e) {
            Debugger.runReport(e);
        }
    }

    public Map<Block, Integer> blockTick = new HashMap<>();

    private void hopperRunner() {
        try {
            Set<Entity> metaItems = new HashSet<>();

            for (com.songoda.epichoppers.hopper.Hopper hopper : instance.getHopperManager().getHoppers().values()) {

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

                org.bukkit.block.Hopper hopperBlock = (org.bukkit.block.Hopper) (block != null ? block.getState() : null);

                if (hopper.getLevel().getBlockBreak() != 0) {
                    int amt = hopper.getLevel().getBlockBreak();
                    if (!blockTick.containsKey(block)) {
                        blockTick.put(block, 1);
                    } else {
                        int tick = blockTick.get(block);
                        int put = tick + 1;
                        blockTick.put(block, put);
                        if (tick >= amt) {
                            Block above = block.getRelative(0, 1, 0);
                            if (above.getType() != Material.AIR && !instance.getConfig().getStringList("Main.BlockBreak Blacklisted Blocks").contains(above.getType().name())) {
                                above.getWorld().playSound(above.getLocation(), Sound.BLOCK_STONE_BREAK, 1F, 1F);
                                Location locationAbove = above.getLocation();
                                locationAbove.add(.5, .5, .5);

                                float ox = (float) (0 + (Math.random() * .5));
                                float oy = (float) (0 + (Math.random() * .5));
                                float oz = (float) (0 + (Math.random() * .5));
                                Arconix.pl().getApi().packetLibrary.getParticleManager().broadcastParticle(locationAbove, ox, oy, oz, 0, instance.getConfig().getString("Main.BlockBreak Particle Type"), 15);

                                above.breakNaturally();
                            }
                            blockTick.remove(block);
                        }
                    }
                }
                for (Entity e : metaItems) {
                    e.removeMetadata("grabbed", instance);
                }
                if (hopper.getLevel().getSuction() != 0) {
                    int suck = hopper.getLevel().getSuction();
                    double radius = suck + .5;

                    Collection<Entity> nearbyEntite = instance.v1_7
                            ? Methods.getNearbyEntities(block.getLocation().add(0.5, 0.5, 0.5), radius, radius, radius)
                            : block.getLocation().getWorld().getNearbyEntities(block.getLocation().add(0.5, 0.5, 0.5), radius, radius, radius);

                    for (Entity e : nearbyEntite) {
                        if (!(e instanceof Item) || e.getTicksLived() < 10 || e.getLocation().getBlock().getType() == Material.HOPPER) {
                            continue;
                        }
                        ItemStack hopItem = ((Item) e).getItemStack().clone();
                        if (hopItem.getType().name().contains("SHULKER_BOX"))
                            continue;
                        if (hopItem.hasItemMeta() && hopItem.getItemMeta().hasDisplayName() &&
                                StringUtils.substring(hopItem.getItemMeta().getDisplayName(), 0, 3).equals("***")) {
                            continue; //Compatibility with Shop instance: https://www.spigotmc.org/resources/shop-a-simple-intuitive-shop-instance.9628/
                        }
                        if (e.hasMetadata("grabbed"))
                            continue;
                        ItemStack item = ((Item) e).getItemStack();
                        if (!canHop(hopperBlock.getInventory(), item, 1)) {
                            continue;
                        }
                        ((Item) e).setPickupDelay(999);
                        e.setMetadata("grabbed", new FixedMetadataValue(instance, ""));
                        metaItems.add(e);
                        if (!e.isOnGround())
                            continue;
                        float xx = (float) (0 + (Math.random() * .3));
                        float yy = (float) (0 + (Math.random() * .3));
                        float zz = (float) (0 + (Math.random() * .3));
                        Arconix.pl().getApi().packetLibrary.getParticleManager().broadcastParticle(e.getLocation(), xx, yy, zz, 0, "FLAME", 5);
                        e.remove();
                        hopperBlock.getInventory().addItem(hopItem);
                        break;
                    }
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

                int amt = hopper.getLevel().getAmount();

                ItemStack[] is = hopperBlock.getInventory().getContents();

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

    private void doBlacklist(Hopper hopperBlock, com.songoda.epichoppers.hopper.Hopper hopper, ItemStack item, ItemStack[] isS, int amt, int place) {
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

    private int addItem(Hopper hopperBlock, com.songoda.epichoppers.hopper.Hopper hopper, Block b2, ItemStack is, ItemStack[] isS, int amt, int place) {
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
                    OfflinePlayer op = Bukkit.getOfflinePlayer(UUID.fromString(instance.dataFile.getConfig().getString("data.enderTracker." + Arconix.pl().getApi().serialize().serializeLocation(b2))));
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
                if (b2.getType() == Material.FURNACE || b2.getType() == Material.BURNING_FURNACE) {
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