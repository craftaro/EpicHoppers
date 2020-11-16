package com.songoda.epichoppers.listeners;

import com.songoda.epichoppers.EpicHoppers;
import com.songoda.epichoppers.hopper.Hopper;
import com.songoda.epichoppers.hopper.HopperBuilder;
import com.songoda.epichoppers.hopper.levels.Level;
import com.songoda.epichoppers.settings.Settings;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.permissions.PermissionAttachmentInfo;


/**
 * Created by songoda on 3/14/2017.
 */
public class BlockListeners implements Listener {

    private final EpicHoppers plugin;

    public BlockListeners(EpicHoppers plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent e) {
        Player player = e.getPlayer();

        if (e.getBlock().getType() != Material.HOPPER)
            return;

        if (plugin.isLiquidtanks() && net.arcaniax.liquidtanks.object.LiquidTankAPI.isLiquidTank(e.getBlock().getLocation()))
            return;

        int amt = count(e.getBlock().getChunk());

        int max = maxHoppers(player);

        if (max != -1 && amt > max) {
            player.sendMessage(plugin.getLocale().getMessage("event.hopper.toomany").processPlaceholder("amount", max).getMessage());
            e.setCancelled(true);
            return;
        }

        ItemStack item = e.getItemInHand().clone();

        if (Settings.ALLOW_NORMAL_HOPPERS.getBoolean() && !plugin.getLevelManager().isEpicHopper(item))
            return;

        Hopper hopper = plugin.getHopperManager().addHopper(
                new HopperBuilder(e.getBlock())
                        .setLevel(plugin.getLevelManager().getLevel(item))
                        .setPlacedBy(player)
                        .setLastPlayerOpened(player).build());
        EpicHoppers.getInstance().getDataManager().createHopper(hopper);
    }

    private int maxHoppers(Player player) {
        int limit = -1;
        for (PermissionAttachmentInfo permissionAttachmentInfo : player.getEffectivePermissions()) {
            if (!permissionAttachmentInfo.getPermission().toLowerCase().startsWith("epichoppers.limit")) continue;
            int num = Integer.parseInt(permissionAttachmentInfo.getPermission().split("\\.")[2]);
            if (num > limit)
                limit = num;
        }
        if (limit == -1) limit = plugin.getConfig().getInt("Main.Max Hoppers Per Chunk");
        return limit;
    }

    private int count(Chunk c) {
        int count = 0;
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = 0; y < c.getWorld().getMaxHeight(); y++) {
                    if (c.getBlock(x, y, z).getType() == Material.HOPPER) count++;
                }
            }
        }
        return count;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        Player player = event.getPlayer();

        if (event.getBlock().getType() != Material.HOPPER) return;

        if (plugin.isLiquidtanks() && net.arcaniax.liquidtanks.object.LiquidTankAPI.isLiquidTank(block.getLocation()))
            return;

        if (Settings.ALLOW_NORMAL_HOPPERS.getBoolean() && !plugin.getHopperManager().isHopper(block.getLocation()))
            return;

        Hopper hopper = plugin.getHopperManager().getHopper(block);

        Level level = hopper.getLevel();

        if (level.getLevel() > 1 || Settings.ALLOW_NORMAL_HOPPERS.getBoolean()) {
            event.setCancelled(true);
            ItemStack item = plugin.newHopperItem(level);

            event.getBlock().setType(Material.AIR);
            event.getBlock().getLocation().getWorld().dropItemNaturally(event.getBlock().getLocation(), item);
        }

        hopper.getFilter().getWhiteList().stream()
                .filter(m -> m != null)
                .forEach(m -> event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation(), m));
        hopper.getFilter().getBlackList().stream()
                .filter(m -> m != null)
                .forEach(m -> event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation(), m));
        hopper.getFilter().getVoidList().stream().
                filter(m -> m != null)
                .forEach(m -> event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation(), m));

        plugin.getHopperManager().removeHopper(block.getLocation());
        plugin.getDataManager().deleteHopper(hopper);

        plugin.getPlayerDataManager().getPlayerData(player).setSyncType(null);
    }
}