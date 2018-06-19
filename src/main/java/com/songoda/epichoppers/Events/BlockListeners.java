package com.songoda.epichoppers.Events;

import com.songoda.arconix.plugin.Arconix;
import com.songoda.epichoppers.EpicHoppers;
import com.songoda.epichoppers.Hopper.Filter;
import com.songoda.epichoppers.Hopper.Hopper;
import com.songoda.epichoppers.Utils.Debugger;
import com.songoda.epichoppers.Utils.Methods;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Collection;

/**
 * Created by songoda on 3/14/2017.
 */
public class BlockListeners implements Listener {

    private EpicHoppers instance;

    public BlockListeners(EpicHoppers instance) {
        this.instance = instance;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent e) {
        try {
            if (e.getBlock().getType().equals(Material.ENDER_CHEST)) {
                instance.dataFile.getConfig().set("data.enderTracker." + Arconix.pl().getApi().serialize().serializeLocation(e.getBlock()), e.getPlayer().getUniqueId().toString());
                return;
            }

            if (e.getBlock().getType() != Material.HOPPER) return;

            int amt = count(e.getBlock().getChunk());
            if (amt >= instance.getConfig().getInt("Main.Max Hoppers Per Chunk") && instance.getConfig().getInt("Main.Max Hoppers Per Chunk") != -1) {
                e.getPlayer().sendMessage(instance.getLocale().getMessage("event.hopper.toomany"));
                e.setCancelled(true);
                return;
            }

            if (!e.getItemInHand().getItemMeta().hasDisplayName()) return;

            ItemStack item = e.getItemInHand().clone();

            //not sure what this shit does
            byte b = e.getBlock().getData();
            e.getBlock().setType(Material.AIR);
            e.getBlock().getLocation().getBlock().setType(Material.HOPPER);
            e.getBlock().getLocation().getBlock().setData(b);

            instance.getHopperManager().addHopper(e.getBlock().getLocation(), new Hopper(e.getBlock(), instance.getLevelManager().getLevel(instance.getApi().getILevel(item)), e.getPlayer().getUniqueId(), null, new Filter(), false));

        } catch (Exception ee) {
            Debugger.runReport(ee);
        }
    }

    public int count(Chunk c) {
        try {
            int count = 0;
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    for (int y = 0; y < c.getWorld().getMaxHeight(); y++) {
                        if (c.getBlock(x, y, z).getType() == Material.HOPPER) count++;
                    }
                }
            }
            return count;
        } catch (Exception e) {
            Debugger.runReport(e);
        }
        return 9999;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        try {
                if (event.getBlock().getType().equals(Material.ENDER_CHEST)) {
                    instance.dataFile.getConfig().set("data.enderTracker." + Arconix.pl().getApi().serialize().serializeLocation(event.getBlock()), null);
                }

            Block block = event.getBlock();

            if (event.getPlayer().getItemInHand() == null) return;

            handleSyncTouch(event);

            if (event.getBlock().getType() != Material.HOPPER) return;

            Hopper hopper = instance.getHopperManager().getHopper(block);

            int level = hopper.getLevel().getLevel();

            if (level != 0) {
                event.setCancelled(true);
                ItemStack item = new ItemStack(Material.HOPPER, 1);
                ItemMeta itemmeta = item.getItemMeta();
                itemmeta.setDisplayName(Arconix.pl().getApi().format().formatText(Methods.formatName(level, true)));
                item.setItemMeta(itemmeta);

                event.getBlock().setType(Material.AIR);
                event.getBlock().getLocation().getWorld().dropItemNaturally(event.getBlock().getLocation(), item);
            }

                for (ItemStack i : hopper.getFilter().getWhiteList()) {
                    if (i != null)
                        event.getBlock().getLocation().getWorld().dropItemNaturally(event.getBlock().getLocation(), i);
                }

                for (ItemStack i : hopper.getFilter().getBlackList()) {
                    if (i != null)
                        event.getBlock().getLocation().getWorld().dropItemNaturally(event.getBlock().getLocation(), i);
                }
                for (ItemStack i : hopper.getFilter().getVoidList()) {
                    if (i != null)
                        event.getBlock().getLocation().getWorld().dropItemNaturally(event.getBlock().getLocation(), i);
                }
                instance.getHopperManager().removeHopper(block.getLocation());

            instance.sync.remove(event.getPlayer());


        } catch (Exception ee) {
            Debugger.runReport(ee);
        }
    }

    private void handleSyncTouch(BlockBreakEvent e) {
        if (!Methods.isSync(e.getPlayer())) return;

        ItemStack tool = e.getPlayer().getItemInHand();
        ItemMeta meta = tool.getItemMeta();
        if (tool.getItemMeta().getLore().size() != 2) return;

        Location location = Arconix.pl().getApi().serialize().unserializeLocation(meta.getLore().get(1).replaceAll("§", ""));

        if (location.getBlock().getType() != Material.CHEST) return;

        if (e.getBlock().getType() == Material.MOB_SPAWNER || e.getBlock().getType() == Material.HOPPER || e.getBlock().getType() == Material.DISPENSER) return;

        try {
            if (e.getBlock().getType().name().contains("SHULKER") && e.getBlock().getType() != Material.SHULKER_SHELL) return;
        } catch (Exception ee) {

        }

        InventoryHolder ih = (InventoryHolder) location.getBlock().getState();
        if (e.getPlayer().getItemInHand().getItemMeta().hasEnchant(Enchantment.SILK_TOUCH)) {
            ih.getInventory().addItem(new ItemStack(e.getBlock().getType(),1, e.getBlock().getData()));
        } else {
            for (ItemStack is : e.getBlock().getDrops())
                ih.getInventory().addItem(is);
        }
        if (instance.v1_12) {
            e.setDropItems(false);
            return;
        }

        e.isCancelled();
        e.getPlayer().getItemInHand().setDurability((short) (e.getPlayer().getItemInHand().getDurability() + 1));
        if (e.getPlayer().getItemInHand().getDurability() >= e.getPlayer().getItemInHand().getType().getMaxDurability()) {
            e.getPlayer().getItemInHand().setType(null);
        }
        if (e.getExpToDrop() > 0)
            e.getPlayer().getWorld().spawn(e.getBlock().getLocation(), ExperienceOrb.class).setExperience(e.getExpToDrop());
        e.getBlock().setType(Material.AIR);
    }
}