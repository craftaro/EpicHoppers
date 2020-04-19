package com.songoda.epichoppers.listeners;

import com.songoda.core.hooks.WorldGuardHook;
import com.songoda.epichoppers.EpicHoppers;
import com.songoda.epichoppers.hopper.Hopper;
import com.songoda.epichoppers.player.PlayerData;
import com.songoda.epichoppers.player.SyncType;
import com.songoda.epichoppers.settings.Settings;
import com.songoda.epichoppers.utils.Methods;
import com.songoda.epichoppers.utils.TeleportTrigger;
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
import org.bukkit.inventory.ItemStack;

/**
 * Created by songoda on 3/14/2017.
 */
public class InteractListeners implements Listener {

    private final EpicHoppers instance;

    public InteractListeners(EpicHoppers instance) {
        this.instance = instance;
    }

    @EventHandler
    public void onPlayerToggleSneakEvent(PlayerToggleSneakEvent event) {
        Player player = event.getPlayer();
        if (player.isSneaking()) {
            Location location = player.getLocation().getBlock().getRelative(BlockFace.SELF).getLocation();
            Location down = location.getBlock().getRelative(BlockFace.DOWN).getLocation();
            if (instance.getHopperManager().isHopper(down)) {
                Hopper hopper = instance.getHopperManager().getHopper(down);
                if (hopper.getTeleportTrigger() == TeleportTrigger.SNEAK)
                    instance.getTeleportHandler().tpEntity(player, hopper);
            } else if (instance.getHopperManager().isHopper(location)) {
                Hopper hopper = instance.getHopperManager().getHopper(location);
                if (hopper.getTeleportTrigger() == TeleportTrigger.SNEAK)
                    instance.getTeleportHandler().tpEntity(player, hopper);
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

        Boolean flag;
        if ((flag = WorldGuardHook.getBooleanFlag(event.getClickedBlock().getLocation(), "use")) != null && !flag)
            return;

        if (event.getClickedBlock().getType() == Material.CHEST && Methods.isSync(player)) {
            ItemStack item = event.getPlayer().getInventory().getItemInHand();
            boolean isLinked = false;

            for (String lore : item.getItemMeta().getLore()) {
                if (!lore.contains(Methods.formatText("&aSync Touch"))) continue;
                isLinked = true;
                break;
            }

            if (isLinked) {
                instance.getLocale().getMessage("event.hopper.desyncchest")
                        .processPlaceholder("name", item.getType().toString()).sendPrefixedMessage(player);
                instance.enchantmentHandler.createSyncTouch(item, null);
            } else {
                instance.getLocale().getMessage("event.hopper.syncchest")
                        .processPlaceholder("name", item.getType().toString()).sendPrefixedMessage(player);
                instance.enchantmentHandler.createSyncTouch(item, event.getClickedBlock());
            }
            event.setCancelled(true);
            return;
        }

        PlayerData playerData = instance.getPlayerDataManager().getPlayerData(player);

        if (playerData.getSyncType() == null) {
            if (event.getClickedBlock().getType() == Material.HOPPER) {
                if (instance.isLiquidtanks() && net.arcaniax.liquidtanks.object.LiquidTankAPI.isLiquidTank(event.getClickedBlock().getLocation()))
                    return;

                if (Settings.ALLOW_NORMAL_HOPPERS.getBoolean() && !instance.getHopperManager().isHopper(event.getClickedBlock().getLocation()))
                    return;

                Hopper hopper = instance.getHopperManager().getHopper(event.getClickedBlock());
                if (!player.getInventory().getItemInHand().getType().name().contains("PICKAXE")) {
                    hopper.overview(instance.getGuiManager(), player);
                    event.setCancelled(true);
                    return;
                }
            }
            return;
        }

        if (event.getClickedBlock().getState() instanceof InventoryHolder || (event.getClickedBlock().getType().equals(Material.ENDER_CHEST) && instance.getConfig().getBoolean("Main.Support Enderchests"))) {
            Hopper hopper = playerData.getLastHopper();
            if (event.getClickedBlock().getLocation().equals(playerData.getLastHopper().getLocation())) {
                if (hopper.getLinkedBlocks().size() != 0)
                    instance.getLocale().getMessage("event.hopper.syncfinish").sendPrefixedMessage(player);
                else
                    instance.getLocale().getMessage("event.hopper.synccanceled").sendPrefixedMessage(player);
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
