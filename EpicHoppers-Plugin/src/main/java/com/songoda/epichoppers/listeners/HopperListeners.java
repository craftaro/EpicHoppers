package com.songoda.epichoppers.listeners;

import com.songoda.epichoppers.EpicHoppersPlugin;
import com.songoda.epichoppers.api.hopper.Hopper;
import com.songoda.epichoppers.utils.Debugger;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.inventory.Inventory;

/**
 * Created by songoda on 4/18/2017.
 */
public class HopperListeners implements Listener {

    private final EpicHoppersPlugin instance;

    public HopperListeners(EpicHoppersPlugin instance) {
        this.instance = instance;
    }

    @EventHandler(ignoreCancelled = true)
    public void onHop(InventoryMoveItemEvent event) {
        try {
            Inventory source = event.getSource();

            if (source.getLocation().getBlock().getType() != Material.HOPPER) return;

            if (instance.isLiquidtanks() && net.arcaniax.liquidtanks.object.LiquidTankAPI.isLiquidTank(event.getDestination().getLocation()))
                return;

            Hopper hopper = instance.getHopperManager().getHopper(source.getLocation());
            if (hopper.getLinkedBlocks() == null || hopper.getLinkedBlocks().isEmpty()) {
                hopper.clearLinkedBlocks();
                hopper.addLinkedBlock(event.getDestination().getLocation().getBlock());
            }
            event.setCancelled(true);
        } catch (Exception ee) {
            Debugger.runReport(ee);
        }
    }
}
