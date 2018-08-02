package com.songoda.epichoppers.Events;

import com.songoda.epichoppers.EpicHoppers;
import com.songoda.epichoppers.Hopper.Hopper;
import com.songoda.epichoppers.Utils.Debugger;
import com.songoda.epichoppers.Utils.Methods;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

/**
 * Created by songoda on 3/14/2017.
 */
public class InteractListeners implements Listener {

    private EpicHoppers instance;

    public InteractListeners(EpicHoppers instance) {
        this.instance = instance;
    }

    @EventHandler
    public void onBlockInteract(PlayerInteractEvent e) {
        try {
            Player player = e.getPlayer();
            if (e.getAction() != Action.LEFT_CLICK_BLOCK
                    || e.getClickedBlock() == null
                    || player.isSneaking()
                    || !player.hasPermission("EpicHoppers.overview")
                    || !instance.hooks.canBuild(player, e.getClickedBlock().getLocation())
                    || !(e.getClickedBlock().getState() instanceof InventoryHolder || e.getClickedBlock().getType().equals(Material.ENDER_CHEST))) {
                return;
            }

            if (e.getClickedBlock().getType() == Material.CHEST && Methods.isSync(player)) {
                ItemStack item = e.getPlayer().getInventory().getItemInMainHand();
                if (item.getItemMeta().getLore().size() == 2) {
                    player.sendMessage(instance.getLocale().getMessage("event.hopper.desyncchest", item.getType().toString()));
                    instance.enchant.createSyncTouch(item, null);
                } else {
                    player.sendMessage(instance.getLocale().getMessage("event.hopper.syncchest", item.getType().toString()));
                    instance.enchant.createSyncTouch(item, e.getClickedBlock());
                }
                e.setCancelled(true);
                return;
            }


            if (!instance.sync.containsKey(player) && !instance.bsync.containsKey(player)) {
                if (e.getClickedBlock().getType() == Material.HOPPER) {
                    instance.lastBlock.put(player, e.getClickedBlock());
                    Hopper hopper = instance.getHopperManager().getHopper(e.getClickedBlock());
                    if (instance.getConfig().getBoolean("Main.Allow Hopper Upgrading")) {
                        if (!player.getInventory().getItemInMainHand().getType().name().contains("PICKAXE")) {
                            hopper.overview(player);
                            e.setCancelled(true);
                        }
                    } else { //ToDO: What is this?
                        if (player.hasPermission("EpicHoppers.Admin")) {
                            instance.sync.put(player, instance.lastBlock.get(player));
                            player.sendMessage(instance.getLocale().getMessage("event.hopper.syncnext"));
                            hopper.timeout(player);
                            player.closeInventory();
                        }
                        e.setCancelled(true);
                    }
                }
                return;
            }

            if (e.getClickedBlock().getType() == Material.BREWING_STAND) return;

            if (e.getClickedBlock().getState() instanceof InventoryHolder || e.getClickedBlock().getType().equals(Material.ENDER_CHEST) && instance.getConfig().getBoolean("Main.Support Enderchests")) {
                if (instance.sync.containsKey(player) && instance.sync.get(player).equals(e.getClickedBlock()) || instance.bsync.containsKey(player) && instance.bsync.get(player).equals(e.getClickedBlock())) {
                    player.sendMessage(instance.getLocale().getMessage("event.hopper.syncself"));
                } else {
                    if (instance.sync.containsKey(player)) {
                        Hopper hopper = instance.getHopperManager().getHopper(instance.sync.get(player));
                        hopper.sync(e.getClickedBlock(), false, player);
                    } else if (instance.bsync.containsKey(player)) {
                        Hopper hopper = instance.getHopperManager().getHopper(instance.bsync.get(player));
                        hopper.sync(e.getClickedBlock(), true, player);
                    }
                }
                e.setCancelled(true);
                instance.sync.remove(player);
                instance.bsync.remove(player);
            }


        } catch (Exception ee) {
            Debugger.runReport(ee);
        }
    }
}
