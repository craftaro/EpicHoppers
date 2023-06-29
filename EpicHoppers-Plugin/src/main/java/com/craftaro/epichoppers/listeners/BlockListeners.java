package com.craftaro.epichoppers.listeners;

import com.craftaro.core.compatibility.ServerVersion;
import com.craftaro.epichoppers.EpicHoppers;
import com.craftaro.epichoppers.api.events.HopperBreakEvent;
import com.craftaro.epichoppers.api.events.HopperPlaceEvent;
import com.craftaro.epichoppers.gui.GUIAutoSellFilter;
import com.craftaro.epichoppers.gui.GUIFilter;
import com.craftaro.epichoppers.hopper.Hopper;
import com.craftaro.epichoppers.hopper.HopperBuilder;
import com.craftaro.epichoppers.hopper.levels.Level;
import com.craftaro.epichoppers.settings.Settings;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.permissions.PermissionAttachmentInfo;

import java.util.Objects;

public class BlockListeners implements Listener {
    private final EpicHoppers plugin;
    private static final boolean hasMinHeight = ServerVersion.isServerVersionAtLeast(ServerVersion.V1_16);

    public BlockListeners(EpicHoppers plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent e) {
        Player player = e.getPlayer();

        if (e.getBlock().getType() != Material.HOPPER) {
            return;
        }

        if (Settings.DISABLED_WORLDS.getStringList().contains(player.getWorld().getName())) {
            return;
        }

        int amt = count(e.getBlock().getChunk());

        int max = maxHoppers(player);

        if (max != -1 && amt > max) {
            player.sendMessage(this.plugin.getLocale().getMessage("event.hopper.toomany").processPlaceholder("amount", max).getMessage());
            e.setCancelled(true);
            return;
        }

        ItemStack item = e.getItemInHand().clone();

        if (Settings.ALLOW_NORMAL_HOPPERS.getBoolean() && !this.plugin.getLevelManager().isEpicHopper(item)) {
            return;
        }

        if (!this.plugin.getHopperManager().isReady()) {
            player.sendMessage(this.plugin.getLocale().getMessage("event.hopper.notready").getMessage());
            e.setCancelled(true);
            return;
        }

        Hopper hopper = this.plugin.getHopperManager().addHopper(
                new HopperBuilder(e.getBlock())
                        .setLevel(this.plugin.getLevelManager().getLevel(item))
                        .setPlacedBy(player)
                        .setLastPlayerOpened(player).build());

        HopperPlaceEvent hopperPlaceEvent = new HopperPlaceEvent(player, hopper);
        Bukkit.getPluginManager().callEvent(hopperPlaceEvent);

        this.plugin.getDataManager().createHopper(hopper);
    }

    private int maxHoppers(Player player) {
        int limit = -1;
        for (PermissionAttachmentInfo permissionAttachmentInfo : player.getEffectivePermissions()) {
            if (!permissionAttachmentInfo.getPermission().toLowerCase().startsWith("epichoppers.limit")) {
                continue;
            }
            int num = Integer.parseInt(permissionAttachmentInfo.getPermission().split("\\.")[2]);
            if (num > limit) {
                limit = num;
            }
        }
        if (limit == -1) {
            limit = Settings.MAX_CHUNK.getInt();
        }
        return limit;
    }

    private int count(Chunk chunk) {
        int count = 0;
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = getMinHeight(chunk.getWorld()); y < chunk.getWorld().getMaxHeight(); y++) {
                    if (chunk.getBlock(x, y, z).getType() == Material.HOPPER) {
                        count++;
                    }
                }
            }
        }
        return count;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        Player player = event.getPlayer();

        if (event.getBlock().getType() != Material.HOPPER) {
            return;
        }

        if (!this.plugin.getHopperManager().isReady()) {
            player.sendMessage(this.plugin.getLocale().getMessage("event.hopper.notready").getMessage());
            event.setCancelled(true);
            return;
        }

        if (Settings.ALLOW_NORMAL_HOPPERS.getBoolean() && !this.plugin.getHopperManager().isHopper(block.getLocation())) {
            return;
        }

        Hopper hopper = this.plugin.getHopperManager().getHopper(block);

        GUIFilter.compileOpenGuiFilter(hopper);
        GUIAutoSellFilter.compileOpenAutoSellFilter(hopper);

        Level level = hopper.getLevel();

        if (level.getLevel() > 1 || Settings.ALLOW_NORMAL_HOPPERS.getBoolean()) {
            HopperBreakEvent hopperBreakEvent = new HopperBreakEvent(player, hopper);
            Bukkit.getPluginManager().callEvent(hopperBreakEvent);

            event.setCancelled(true);
            ItemStack item = this.plugin.newHopperItem(level);

            hopper.dropItems();

            event.getBlock().setType(Material.AIR);
            event.getBlock().getLocation().getWorld().dropItemNaturally(event.getBlock().getLocation(), item);
        }

        hopper.forceClose();

        hopper.getFilter().getWhiteList()
                .stream()
                .filter(Objects::nonNull)
                .forEach(item -> event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation(), item));
        hopper.getFilter().getBlackList()
                .stream()
                .filter(Objects::nonNull)
                .forEach(item -> event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation(), item));
        hopper.getFilter().getVoidList()
                .stream()
                .filter(Objects::nonNull)
                .forEach(item -> event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation(), item));

        hopper.getFilter().getAutoSellWhiteList()
                .stream()
                .filter(Objects::nonNull)
                .forEach(item -> event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation(), item));
        hopper.getFilter().getAutoSellBlackList()
                .stream()
                .filter(Objects::nonNull)
                .forEach(item -> event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation(), item));

        this.plugin.getHopperManager().removeHopper(block.getLocation());
        this.plugin.getDataManager().deleteHopper(hopper);

        this.plugin.getPlayerDataManager().getPlayerData(player).setSyncType(null);
    }

    public int getMinHeight(World world) {
        return hasMinHeight ? world.getMinHeight() : 0;
    }
}
