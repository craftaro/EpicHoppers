package com.songoda.epichoppers.listeners;

import com.songoda.arconix.plugin.Arconix;
import com.songoda.epichoppers.EpicHoppersPlugin;
import com.songoda.epichoppers.api.hopper.Hopper;
import com.songoda.epichoppers.api.hopper.levels.Level;
import com.songoda.epichoppers.api.hopper.TeleportTrigger;
import com.songoda.epichoppers.hopper.EFilter;
import com.songoda.epichoppers.hopper.EHopper;
import com.songoda.epichoppers.utils.Debugger;
import com.songoda.epichoppers.utils.Methods;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.permissions.PermissionAttachmentInfo;

import java.util.Map;

/**
 * Created by songoda on 3/14/2017.
 */
public class BlockListeners implements Listener {

    private EpicHoppersPlugin instance;

    public BlockListeners(EpicHoppersPlugin instance) {
        this.instance = instance;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent e) {
        try {
            Player player = e.getPlayer();
            if (e.getBlock().getType().equals(Material.ENDER_CHEST)) {
                instance.getDataFile().getConfig().set("data.enderTracker." + Arconix.pl().getApi().serialize().serializeLocation(e.getBlock()), player.getUniqueId().toString());
                return;
            }

            if (e.getBlock().getType() != Material.HOPPER) return;

            int amt = count(e.getBlock().getChunk());

            int max = maxHoppers(player);

            if (max != -1 && amt >= max) {
                player.sendMessage(instance.getLocale().getMessage("event.hopper.toomany", max));
                e.setCancelled(true);
                return;
            }

            if (!e.getItemInHand().getItemMeta().hasDisplayName()) return;

            ItemStack item = e.getItemInHand().clone();

            instance.getHopperManager().addHopper(e.getBlock().getLocation(), new EHopper(e.getBlock(), instance.getLevelFromItem(item), player.getUniqueId(), player.getUniqueId(),null, new EFilter(), TeleportTrigger.DISABLED, null));

        } catch (Exception ee) {
            Debugger.runReport(ee);
        }
    }

    private int maxHoppers(Player player) {
        int limit = -1;
        for (PermissionAttachmentInfo permissionAttachmentInfo : player.getEffectivePermissions()) {
                if (!permissionAttachmentInfo.getPermission().toLowerCase().startsWith("epichoppers.limit")) continue;
                limit = Integer.parseInt(permissionAttachmentInfo.getPermission().split("\\.")[2]);
        }
        if (limit == -1) limit = instance.getConfig().getInt("Main.Max Hoppers Per Chunk");
        return limit;
    }

    private int count(Chunk c) {
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
                instance.getDataFile().getConfig().set("data.enderTracker." + Arconix.pl().getApi().serialize().serializeLocation(event.getBlock()), null);
            }

            Block block = event.getBlock();
            Player player = event.getPlayer();

            if (player.getInventory().getItemInMainHand() == null) return;

            handleSyncTouch(event);

            if (event.getBlock().getType() != Material.HOPPER) return;

            Hopper hopper = instance.getHopperManager().getHopper(block);

            Level level = hopper.getLevel();

            if (level.getLevel() != 0) {
                event.setCancelled(true);
                ItemStack item = instance.newHopperItem(level);

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

            instance.getPlayerDataManager().getPlayerData(player).setSyncType(null);


        } catch (Exception ee) {
            Debugger.runReport(ee);
        }
    }

    private void handleSyncTouch(BlockBreakEvent e) {
        if (!Methods.isSync(e.getPlayer())) return;

        ItemStack tool = e.getPlayer().getInventory().getItemInMainHand();
        ItemMeta meta = tool.getItemMeta();
        if (tool.getItemMeta().getLore().size() != 2) return;

        Location location = Arconix.pl().getApi().serialize().unserializeLocation(meta.getLore().get(1).replaceAll("§", ""));

        if (location.getBlock().getType() != Material.CHEST) return;

        if (e.getBlock().getType() == Material.SHULKER_BOX
                || e.getBlock().getType() == Material.SPAWNER
                || e.getBlock().getType() == Material.HOPPER
                || e.getBlock().getType() == Material.DISPENSER) {
            return;
        }

        InventoryHolder ih = (InventoryHolder) location.getBlock().getState();
        if (e.getPlayer().getInventory().getItemInMainHand().getItemMeta().hasEnchant(Enchantment.SILK_TOUCH)) {
            ih.getInventory().addItem(new ItemStack(e.getBlock().getType(), 1, e.getBlock().getData()));
        } else {
            for (ItemStack is : e.getBlock().getDrops())
                ih.getInventory().addItem(is);
        }
        e.setDropItems(false);
    }
}