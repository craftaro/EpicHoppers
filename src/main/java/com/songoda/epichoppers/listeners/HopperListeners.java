package com.songoda.epichoppers.listeners;

import com.songoda.epichoppers.EpicHoppers;
import com.songoda.epichoppers.hopper.Hopper;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Chest;
import org.bukkit.block.DoubleChest;
import org.bukkit.entity.Minecart;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/**
 * Created by songoda on 4/18/2017.
 */
public class HopperListeners implements Listener {

    private final EpicHoppers instance;

    public HopperListeners(EpicHoppers instance) {
        this.instance = instance;
    }

    @EventHandler(ignoreCancelled = true)
    public void onHop(InventoryMoveItemEvent event) {
        Inventory source = event.getSource();
        Inventory destination = event.getDestination();

        if (!(source.getHolder() instanceof org.bukkit.block.Hopper)) return;

        if (instance.isLiquidtanks() && net.arcaniax.liquidtanks.object.LiquidTankAPI.isLiquidTank(event.getDestination().getLocation()))
            return;

        org.bukkit.block.Hopper sourceHopper = (org.bukkit.block.Hopper) source.getHolder();

        Location destinationLocation;
        if (destination.getHolder() instanceof org.bukkit.block.Hopper) {
            destinationLocation = ((org.bukkit.block.Hopper) destination.getHolder()).getLocation();
        } else if (destination.getHolder() instanceof Chest) {
            destinationLocation = ((Chest) destination.getHolder()).getLocation();
        } else if (destination.getHolder() instanceof DoubleChest) {
            destinationLocation = ((DoubleChest) destination.getHolder()).getLocation();
        } else {
            return;
        }

        if (!(destinationLocation.getBlock().getState() instanceof InventoryHolder)) return;

        Hopper hopper = instance.getHopperManager().getHopper(sourceHopper.getLocation());

        hopper.clearLinkedBlocks();
        hopper.addLinkedBlock(destinationLocation);

        event.setCancelled(true);

    }
}
