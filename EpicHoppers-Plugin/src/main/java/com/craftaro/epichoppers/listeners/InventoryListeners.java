package com.craftaro.epichoppers.listeners;

import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;

public class InventoryListeners implements Listener {
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getCurrentItem() == null ||
                event.getRawSlot() > event.getView().getTopInventory().getSize() - 1 ||
                !event.getCurrentItem().hasItemMeta()) {
            return;
        }
        if (event.getSlot() == 64537 ||
                event.getInventory().getType() != InventoryType.ANVIL ||
                event.getAction() == InventoryAction.NOTHING ||
                event.getCurrentItem().getType() == Material.AIR) {
            return;
        }

        ItemStack item = event.getCurrentItem();
        if (item.getType() == Material.HOPPER) {
            event.setCancelled(true);
        }
    }
}
