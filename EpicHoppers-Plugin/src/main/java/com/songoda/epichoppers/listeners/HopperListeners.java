package com.songoda.epichoppers.listeners;

import com.songoda.epichoppers.EpicHoppersPlugin;
import com.songoda.epichoppers.api.hopper.Hopper;
import com.songoda.epichoppers.utils.Debugger;
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
    public void onHop(InventoryMoveItemEvent e) {
        try {
            Inventory source = e.getSource();

            if (!instance.getHopperManager().isHopper(e.getSource().getLocation())) return;

            Hopper hopper = instance.getHopperManager().getHopper(e.getSource().getLocation());

            if (source.getHolder() instanceof Hopper && hopper.getSyncedBlock() != null) {
                e.setCancelled(true);
            }
        } catch (Exception ee) {
            Debugger.runReport(ee);
        }
    }
}
