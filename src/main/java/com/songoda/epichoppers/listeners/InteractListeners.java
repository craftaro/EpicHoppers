package com.songoda.epichoppers.listeners;

import com.songoda.epichoppers.EpicHoppers;
import com.songoda.epichoppers.hopper.Hopper;
import com.songoda.epichoppers.player.PlayerData;
import com.songoda.epichoppers.player.SyncType;
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
    public void onBlockInteract(PlayerInteractEvent e) {
            Player player = e.getPlayer();
            if (e.getAction() != Action.LEFT_CLICK_BLOCK
                    || e.getClickedBlock() == null
                    || player.isSneaking()
                    || !player.hasPermission("EpicHoppers.overview")
                    || !(e.getClickedBlock().getState() instanceof InventoryHolder || e.getClickedBlock().getType().equals(Material.ENDER_CHEST))) {
                return;
            }

            if (e.getClickedBlock().getType() == Material.CHEST && Methods.isSync(player)) {
                ItemStack item = e.getPlayer().getInventory().getItemInHand();
                if (item.getItemMeta().getLore().size() == 2) {
                    player.sendMessage(instance.getLocale().getMessage("event.hopper.desyncchest", item.getType().toString()));
                    instance.enchantmentHandler.createSyncTouch(item, null);
                } else {
                    player.sendMessage(instance.getLocale().getMessage("event.hopper.syncchest", item.getType().toString()));
                    instance.enchantmentHandler.createSyncTouch(item, e.getClickedBlock());
                }
                e.setCancelled(true);
                return;
            }

            PlayerData playerData = instance.getPlayerDataManager().getPlayerData(player);

            if (playerData.getSyncType() == null) {
                if (e.getClickedBlock().getType() == Material.HOPPER) {
                    if (instance.isLiquidtanks() && net.arcaniax.liquidtanks.object.LiquidTankAPI.isLiquidTank(e.getClickedBlock().getLocation()))
                        return;
                    Hopper hopper = instance.getHopperManager().getHopper(e.getClickedBlock());
                    playerData.setLastHopper(hopper);
                    if (instance.getConfig().getBoolean("Main.Allow hopper Upgrading")
                            && !player.getInventory().getItemInHand().getType().name().contains("PICKAXE")) {
                        hopper.overview(player);
                        e.setCancelled(true);
                        return;
                    }
                }
                return;
            }

            if (e.getClickedBlock().getState() instanceof InventoryHolder || e.getClickedBlock().getType().equals(Material.ENDER_CHEST) && instance.getConfig().getBoolean("Main.Support Enderchests")) {
                Hopper hopper = playerData.getLastHopper();
                if (playerData.getSyncType() != null && e.getClickedBlock().getLocation().equals(playerData.getLastHopper().getLocation())) {
                    player.sendMessage(instance.getLocale().getMessage("event.hopper.syncself"));
                } else if (playerData.getSyncType() != null) {
                    hopper.link(e.getClickedBlock(), playerData.getSyncType() == SyncType.FILTERED, player);
                }
                e.setCancelled(true);
                int amountLinked = hopper.getLevel().getLinkAmount();
                if (hopper.getLinkedBlocks().size() >= amountLinked) {
                    playerData.setSyncType(null);
                }
            }
    }
}
