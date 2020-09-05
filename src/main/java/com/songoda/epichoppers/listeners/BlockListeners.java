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

import java.util.Random;


/**
 * Created by songoda on 3/14/2017.
 */
public class BlockListeners implements Listener {

    private final EpicHoppers instance;
    private final Random random;

    public BlockListeners(EpicHoppers instance) {
        this.instance = instance;
        this.random = new Random();
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent e) {
        Player player = e.getPlayer();

        if (e.getBlock().getType() != Material.HOPPER)
            return;

        if (instance.isLiquidtanks() && net.arcaniax.liquidtanks.object.LiquidTankAPI.isLiquidTank(e.getBlock().getLocation()))
            return;

        int amt = count(e.getBlock().getChunk());

        int max = maxHoppers(player);

        if (max != -1 && amt > max) {
            player.sendMessage(instance.getLocale().getMessage("event.hopper.toomany").processPlaceholder("amount", max).getMessage());
            e.setCancelled(true);
            return;
        }

        ItemStack item = e.getItemInHand().clone();

        if (Settings.ALLOW_NORMAL_HOPPERS.getBoolean() && !instance.getLevelManager().isEpicHopper(item))
            return;

        Hopper hopper = instance.getHopperManager().addHopper(
                new HopperBuilder(e.getBlock())
                        .setLevel(instance.getLevelManager().getLevel(item))
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
        if (limit == -1) limit = instance.getConfig().getInt("Main.Max Hoppers Per Chunk");
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

        if (instance.isLiquidtanks() && net.arcaniax.liquidtanks.object.LiquidTankAPI.isLiquidTank(block.getLocation()))
            return;

        if (Settings.ALLOW_NORMAL_HOPPERS.getBoolean() && !instance.getHopperManager().isHopper(block.getLocation()))
            return;

        Hopper hopper = instance.getHopperManager().getHopper(block);

        Level level = hopper.getLevel();

        if (level.getLevel() > 1) {
            event.setCancelled(true);
            ItemStack item = instance.newHopperItem(level);

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

        instance.getHopperManager().removeHopper(block.getLocation());
        instance.getDataManager().deleteHopper(hopper);

        instance.getPlayerDataManager().getPlayerData(player).setSyncType(null);
    }
}