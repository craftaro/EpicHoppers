package com.songoda.epichoppers.listeners;

import com.songoda.epichoppers.EpicHoppersPlugin;
import com.songoda.epichoppers.api.hopper.Hopper;
import com.songoda.epichoppers.api.hopper.TeleportTrigger;
import com.songoda.epichoppers.hopper.EHopper;
import com.songoda.epichoppers.player.PlayerData;
import com.songoda.epichoppers.player.SyncType;
import com.songoda.epichoppers.utils.Debugger;
import com.songoda.epichoppers.utils.Methods;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

/**
 * Created by songoda on 3/14/2017.
 */
public class InteractListeners implements Listener {

    private final EpicHoppersPlugin instance;

    public InteractListeners(EpicHoppersPlugin instance) {
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
                    instance.getTeleportHandler().tpPlayer(player, hopper);
            } else if (instance.getHopperManager().isHopper(location)) {
                Hopper hopper = instance.getHopperManager().getHopper(location);
                if (hopper.getTeleportTrigger() == TeleportTrigger.SNEAK)
                    instance.getTeleportHandler().tpPlayer(player, hopper);
            }
        }
    }

    @EventHandler
    public void onMoveChunk(PlayerMoveEvent e) {
        updateHopper(e.getFrom().getChunk(), e.getTo().getChunk());
    }

    @EventHandler
    public void onTeleport(PlayerTeleportEvent e) {
        updateHopper(e.getFrom().getChunk(), e.getTo().getChunk());
    }

    private void updateHopper(Chunk from, Chunk to) {
        if (from == to) return;

        for (Hopper hopper : instance.getHopperManager().getHoppers().values()) {
            Location location = hopper.getLocation();

            int x = location.getBlockX() >> 4;
            int z = location.getBlockZ() >> 4;

            if (location.getWorld().getChunkAt(x, z) == to) {
                ((EHopper)hopper).reloadHopper();
            }
        }
    }

    @EventHandler
    public void onBlockInteract(PlayerInteractEvent e) {
        try {
            Player player = e.getPlayer();
            if (e.getAction() != Action.LEFT_CLICK_BLOCK
                    || e.getClickedBlock() == null
                    || player.isSneaking()
                    || !player.hasPermission("EpicHoppers.overview")
                    || !instance.canBuild(player, e.getClickedBlock().getLocation())
                    || !(e.getClickedBlock().getState() instanceof InventoryHolder || e.getClickedBlock().getType().equals(Material.ENDER_CHEST))) {
                return;
            }

            if (e.getClickedBlock().getType() == Material.CHEST && Methods.isSync(player)) {
                ItemStack item = e.getPlayer().getInventory().getItemInMainHand();
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
                    Hopper hopper = instance.getHopperManager().getHopper(e.getClickedBlock());
                    playerData.setLastHopper(hopper);
                    if (instance.getConfig().getBoolean("Main.Allow hopper Upgrading")
                            && !player.getInventory().getItemInHand().getType().name().contains("PICKAXE")) {
                        ((EHopper) hopper).overview(player);
                        e.setCancelled(true);
                        return;
                    }
                }
                return;
            }

            if (e.getClickedBlock().getType() == Material.BREWING_STAND) return;

            if (e.getClickedBlock().getState() instanceof InventoryHolder || e.getClickedBlock().getType().equals(Material.ENDER_CHEST) && instance.getConfig().getBoolean("Main.Support Enderchests")) {
                if (playerData.getSyncType() != null && e.getClickedBlock().getLocation().equals(playerData.getLastHopper().getLocation())) {
                    player.sendMessage(instance.getLocale().getMessage("event.hopper.syncself"));
                } else if (playerData.getSyncType() != null) {
                    playerData.getLastHopper().sync(e.getClickedBlock(), playerData.getSyncType() == SyncType.FILTERED, player);
                }
                e.setCancelled(true);
                playerData.setSyncType(null);
            }
        } catch (Exception ee) {
            Debugger.runReport(ee);
        }
    }
}
