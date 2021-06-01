package com.songoda.epichoppers.listeners;

import com.songoda.core.compatibility.ServerVersion;
import com.songoda.core.utils.BlockUtils;
import com.songoda.epichoppers.EpicHoppers;
import com.songoda.epichoppers.hopper.Hopper;
import com.songoda.epichoppers.hopper.levels.modules.Module;
import com.songoda.epichoppers.hopper.levels.modules.ModuleAutoCrafting;
import com.songoda.epichoppers.settings.Settings;
import com.songoda.epichoppers.utils.HopperDirection;
import com.songoda.epichoppers.utils.Methods;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.minecart.HopperMinecart;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/**
 * Created by songoda on 4/18/2017.
 */
public class HopperListeners implements Listener {

    private final EpicHoppers plugin;

    public HopperListeners(EpicHoppers plugin) {
        this.plugin = plugin;
    }

    // todo: InventoryMoveItemEvent for filters

    @EventHandler(ignoreCancelled = true)
    public void onHop(InventoryMoveItemEvent event) {
        Inventory source = event.getSource();
        Inventory destination = event.getDestination();
        Location sourceLocation = source.getHolder() instanceof BlockState ? ((BlockState) source.getHolder()).getLocation() : null;
        Location destinationLocation = destination.getHolder() instanceof BlockState ? ((BlockState) destination.getHolder()).getLocation() : null;

        if (sourceLocation != null && Settings.ALLOW_NORMAL_HOPPERS.getBoolean() && !plugin.getHopperManager().isHopper(sourceLocation))
            return;

        // Hopper minecarts should be able to take care of themselves
        // Let EpicHoppers take over if the hopper is pointing down though
        if (destination.getHolder() instanceof HopperMinecart
                && source.getHolder() instanceof org.bukkit.block.Hopper
                && HopperDirection.getDirection(((org.bukkit.block.Hopper) source.getHolder()).getRawData()) != HopperDirection.DOWN)
            return;

        // Shulker boxes have a mind of their own and relentlessly steal items from hoppers
        if (ServerVersion.isServerVersionAtLeast(ServerVersion.V1_11)
                && destination.getHolder() instanceof org.bukkit.block.ShulkerBox
                && source.getHolder() instanceof org.bukkit.block.Hopper) {
            event.setCancelled(true);
            return;
        }

        // Hopper going into minecarts
        if (destination.getHolder() instanceof Minecart && source.getHolder() instanceof org.bukkit.block.Hopper) {
            event.setCancelled(true);
            return;
        }

        // Special cases when a hopper is picking up items
        if (destination.getHolder() instanceof org.bukkit.block.Hopper) {
            if (destinationLocation != null && Settings.ALLOW_NORMAL_HOPPERS.getBoolean() && !plugin.getHopperManager().isHopper(destinationLocation))
                return;

            // Calling HopperManager#getHopper() automatically creates a new Hopper and we don't need to iterate over default-valued hoppers
            if (!plugin.getHopperManager().isHopper(destinationLocation))
                return;

            Hopper toHopper = plugin.getHopperManager().getHopper(destinationLocation);
            // minecraft 1.8 doesn't have a method to get the hopper's location from the inventory, so we use the holder instead
            final ItemStack toMove = event.getItem();

            // Don't fill the last inventory slot on crafting hoppers (fixes crafters getting stuck)
            Module crafting = toHopper == null ? null : toHopper.getLevel().getModule("AutoCrafting");
            ItemStack toCraft = crafting instanceof ModuleAutoCrafting ? ((ModuleAutoCrafting) crafting).getAutoCrafting(toHopper) : null;
            // if we're not moving the item that we're trying to craft, we need to verify that we're not trying to fill the last slot
            // (filling every slot leaves no room for the crafter to function)
            if (toCraft != null && toCraft.getType() != Material.AIR
                    && !Methods.isSimilarMaterial(toMove, toCraft)
                    && !Methods.canMoveReserved(destination, toMove)) {
                event.setCancelled(true);
                return;
            }

            // pay attention to whitelist/blacklist if no linked chest defined
            if (toHopper != null
                    && toHopper.getFilter().getEndPoint() == null
                    && !(toHopper.getFilter().getWhiteList().isEmpty() && toHopper.getFilter().getBlackList().isEmpty())) {
                // this hopper has a filter with no rejection endpoint, so don't absorb disallowed items
                boolean allowItem;
                ItemStack moveInstead = null;
                // whitelist has priority
                if (!toHopper.getFilter().getWhiteList().isEmpty()) {
                    // is this item on the whitelist?
                    allowItem = toHopper.getFilter().getWhiteList().stream().anyMatch(item -> Methods.isSimilarMaterial(toMove, item));
                    if (!allowItem) {
                        // can we change the item to something else?
                        searchReplacement:
                        for (ItemStack sourceItem : source.getContents()) {
                            if (sourceItem != null && Methods.canMove(destination, sourceItem)) {
                                for (ItemStack item : toHopper.getFilter().getWhiteList()) {
                                    if (Methods.isSimilarMaterial(sourceItem, item)) {
                                        moveInstead = new ItemStack(sourceItem);
                                        moveInstead.setAmount(1);
                                        break searchReplacement;
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // check the blacklist
                    allowItem = toHopper.getFilter().getBlackList().stream().noneMatch(item -> Methods.isSimilarMaterial(toMove, item));
                    if (!allowItem) {
                        // can we change the item to something else?
                        for (ItemStack sourceItem : source.getContents()) {
                            if (sourceItem != null && Methods.canMove(destination, sourceItem)) {
                                boolean blacklisted = toHopper.getFilter().getBlackList().stream().anyMatch(item -> Methods.isSimilarMaterial(sourceItem, item));
                                if (!blacklisted) {
                                    moveInstead = new ItemStack(sourceItem);
                                    moveInstead.setAmount(1);
                                    break;
                                }
                            }
                        }
                    }
                }
                if (!allowItem) {
                    event.setCancelled(true);
                    if (moveInstead != null) {
                        // hopper code is a bit derpy - changing the item doesn't change what's removed
                        //event.setItem(moveInstead);
                        // we need to instead cancel and manually remove the item to move
                        source.removeItem(moveInstead);
                        BlockUtils.updateAdjacentComparators(sourceLocation);
                        // now add it to the hopper
                        destination.addItem(moveInstead);
                        BlockUtils.updateAdjacentComparators(destinationLocation);
                    }
                    return;
                }
            }
        }

        if (!(source.getHolder() instanceof org.bukkit.block.Hopper))
            return;

        if (destinationLocation == null)
            return;

        // Handle hopper push events elsewhere
        event.setCancelled(true);
    }
}
