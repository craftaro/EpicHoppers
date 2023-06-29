package com.craftaro.epichoppers.listeners;

import com.craftaro.core.hooks.ProtectionManager;
import com.craftaro.core.hooks.WorldGuardHook;
import com.craftaro.epichoppers.settings.Settings;
import com.craftaro.epichoppers.EpicHoppers;
import com.craftaro.epichoppers.gui.GUIOverview;
import com.craftaro.epichoppers.hopper.Hopper;
import com.craftaro.epichoppers.hopper.teleport.TeleportTrigger;
import com.craftaro.epichoppers.player.PlayerData;
import com.craftaro.epichoppers.player.SyncType;
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

public class InteractListeners implements Listener {
    private final EpicHoppers plugin;

    public InteractListeners(EpicHoppers plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerToggleSneakEvent(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();
        if (player.isSneaking() && this.plugin.getHopperManager().isReady()) {
            Location location = player.getLocation().getBlock().getRelative(BlockFace.SELF).getLocation();
            Location down = location.getBlock().getRelative(BlockFace.DOWN).getLocation();
            if (this.plugin.getHopperManager().isHopper(down)) {
                Hopper hopper = this.plugin.getHopperManager().getHopper(down);
                if (hopper.getTeleportTrigger() == TeleportTrigger.SNEAK) {
                    this.plugin.getTeleportHandler().tpEntity(player, hopper);
                }
            } else if (this.plugin.getHopperManager().isHopper(location)) {
                Hopper hopper = this.plugin.getHopperManager().getHopper(location);
                if (hopper.getTeleportTrigger() == TeleportTrigger.SNEAK) {
                    this.plugin.getTeleportHandler().tpEntity(player, hopper);
                }
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
                || !(event.getClickedBlock().getState() instanceof InventoryHolder || event.getClickedBlock().getType() == Material.ENDER_CHEST)) {
            return;
        }

        if (Settings.USE_PROTECTION_PLUGINS.getBoolean() && ProtectionManager.canInteract(player, event.getClickedBlock().getLocation()) && WorldGuardHook.isInteractAllowed(event.getClickedBlock().getLocation())) {
            player.sendMessage(this.plugin.getLocale().getMessage("event.general.protected").getPrefixedMessage());
            return;
        }

        if (Bukkit.getPluginManager().isPluginEnabled("FabledSkyBlock")) {
            SkyBlock skyBlock = SkyBlock.getInstance();

            if (skyBlock.getWorldManager().isIslandWorld(event.getPlayer().getWorld()) &&
                    !skyBlock.getPermissionManager().hasPermission(event.getPlayer(), skyBlock.getIslandManager().getIslandAtLocation(event.getClickedBlock().getLocation()), "EpicHoppers")) {
                return;
            }
        }

        PlayerData playerData = this.plugin.getPlayerDataManager().getPlayerData(player);

        if (playerData.getSyncType() == null) {
            if (event.getClickedBlock().getType() == Material.HOPPER) {
                if (!this.plugin.getHopperManager().isReady()) {
                    player.sendMessage(this.plugin.getLocale().getMessage("event.hopper.notready").getMessage());
                    event.setCancelled(true);
                    return;
                }

                if (Settings.ALLOW_NORMAL_HOPPERS.getBoolean() && !this.plugin.getHopperManager().isHopper(event.getClickedBlock().getLocation())) {
                    return;
                }

                Hopper hopper = this.plugin.getHopperManager().getHopper(event.getClickedBlock());
                if (!player.getInventory().getItemInHand().getType().name().contains("PICKAXE")) {
                    if (hopper.prepareForOpeningOverviewGui(player)) {
                        this.plugin.getGuiManager().showGUI(player, new GUIOverview(this.plugin, hopper, player));
                    }
                    event.setCancelled(true);
                    return;
                }
            }
            return;
        }

        if (event.getClickedBlock().getState() instanceof InventoryHolder ||
                (event.getClickedBlock().getType() == Material.ENDER_CHEST && Settings.ENDERCHESTS.getBoolean())) {
            Hopper hopper = playerData.getLastHopper();
            if (event.getClickedBlock().getLocation().equals(playerData.getLastHopper().getLocation())) {
                if (!hopper.getLinkedBlocks().isEmpty()) {
                    this.plugin.getLocale().getMessage("event.hopper.syncfinish").sendPrefixedMessage(player);
                } else {
                    this.plugin.getLocale().getMessage("event.hopper.synccanceled").sendPrefixedMessage(player);
                }
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
