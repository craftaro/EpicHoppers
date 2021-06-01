package com.songoda.epichoppers.listeners;

import com.songoda.core.hooks.ProtectionManager;
import com.songoda.core.hooks.WorldGuardHook;
import com.songoda.epichoppers.EpicHoppers;
import com.songoda.epichoppers.hopper.Hopper;
import com.songoda.epichoppers.player.PlayerData;
import com.songoda.epichoppers.player.SyncType;
import com.songoda.epichoppers.settings.Settings;
import com.songoda.epichoppers.hopper.teleport.TeleportTrigger;
import com.songoda.skyblock.SkyBlock;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.InventoryHolder;

/**
 * Created by songoda on 3/14/2017.
 */
public class InteractListeners implements Listener {

    private final EpicHoppers plugin;

    public InteractListeners(EpicHoppers plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerToggleSneakEvent(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();
        if (player.isSneaking() && plugin.getHopperManager().isReady()) {
            Location location = player.getLocation().getBlock().getRelative(BlockFace.SELF).getLocation();
            Location down = location.getBlock().getRelative(BlockFace.DOWN).getLocation();
            if (plugin.getHopperManager().isHopper(down)) {
                Hopper hopper = plugin.getHopperManager().getHopper(down);
                if (hopper.getTeleportTrigger() == TeleportTrigger.SNEAK)
                    plugin.getTeleportHandler().tpEntity(player, hopper);
            } else if (plugin.getHopperManager().isHopper(location)) {
                Hopper hopper = plugin.getHopperManager().getHopper(location);
                if (hopper.getTeleportTrigger() == TeleportTrigger.SNEAK)
                    plugin.getTeleportHandler().tpEntity(player, hopper);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (event.getAction() != Action.LEFT_CLICK_BLOCK
                || event.getClickedBlock() == null
                || player.isSneaking()
                || !player.hasPermission("EpicHoppers.overview")
                || !(event.getClickedBlock().getState() instanceof InventoryHolder || event.getClickedBlock().getType().equals(Material.ENDER_CHEST))) {
            return;
        }

        if (WorldGuardHook.isInteractAllowed(event.getClickedBlock().getLocation()))
            return;

        if (Settings.USE_PROTECTION_PLUGINS.getBoolean() && !ProtectionManager.canInteract(player, event.getClickedBlock().getLocation())) {
            player.sendMessage(plugin.getLocale().getMessage("event.general.protected").getPrefixedMessage());
            return;
        }

        if (Bukkit.getPluginManager().isPluginEnabled("FabledSkyBlock")) {
            SkyBlock skyBlock = SkyBlock.getInstance();

            if (skyBlock.getWorldManager().isIslandWorld(event.getPlayer().getWorld()))
                if (!skyBlock.getPermissionManager().hasPermission(event.getPlayer(),
                        skyBlock.getIslandManager().getIslandAtLocation(event.getClickedBlock().getLocation()),
                        "EpicHoppers"))
                    return;
        }

        PlayerData playerData = plugin.getPlayerDataManager().getPlayerData(player);

        if (playerData.getSyncType() == null) {
            if (event.getClickedBlock().getType() == Material.HOPPER) {
                if (!plugin.getHopperManager().isReady()) {
                    player.sendMessage(plugin.getLocale().getMessage("event.hopper.notready").getMessage());
                    event.setCancelled(true);
                    return;
                }

                if (Settings.ALLOW_NORMAL_HOPPERS.getBoolean() && !plugin.getHopperManager().isHopper(event.getClickedBlock().getLocation()))
                    return;

                Hopper hopper = plugin.getHopperManager().getHopper(event.getClickedBlock());
                if (!player.getInventory().getItemInHand().getType().name().contains("PICKAXE")) {
                    hopper.overview(plugin.getGuiManager(), player);
                    event.setCancelled(true);
                    return;
                }
            }
            return;
        }

        if (event.getClickedBlock().getState() instanceof InventoryHolder
                || (event.getClickedBlock().getType().equals(Material.ENDER_CHEST)
                && Settings.ENDERCHESTS.getBoolean())) {
            Hopper hopper = playerData.getLastHopper();
            if (event.getClickedBlock().getLocation().equals(playerData.getLastHopper().getLocation())) {
                if (!hopper.getLinkedBlocks().isEmpty())
                    plugin.getLocale().getMessage("event.hopper.syncfinish").sendPrefixedMessage(player);
                else
                    plugin.getLocale().getMessage("event.hopper.synccanceled").sendPrefixedMessage(player);
                hopper.cancelSync(player);
            } else if (playerData.getSyncType() != null) {
                hopper.link(event.getClickedBlock(), playerData.getSyncType() == SyncType.FILTERED, player);
            }
            event.setCancelled(true);
            int amountLinked = hopper.getLevel().getLinkAmount();
            if (hopper.getLinkedBlocks().size() >= amountLinked) {
                playerData.setSyncType(null);
            }
        }
    }
}
