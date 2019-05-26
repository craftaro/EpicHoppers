package com.songoda.epichoppers.listeners;

import com.songoda.epichoppers.EpicHoppers;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/**
 * Created by songoda on 3/14/2017.
 */
public class InventoryListeners implements Listener {

    private final EpicHoppers instance;

    public InventoryListeners(EpicHoppers instance) {
        this.instance = instance;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        if (event.getCurrentItem() == null) return;

        if (event.getCursor() != null && event.getCurrentItem() != null) {
            ItemStack c = event.getCursor();
            ItemStack item = event.getCurrentItem();
            if (c.hasItemMeta()
                    && c.getItemMeta().hasLore()
                    && c.getType() == Material.ENCHANTED_BOOK
                    && (item.getType().name().toUpperCase().contains("AXE") || item.getType().name().toUpperCase().contains("SHOVEL") || item.getType().name().toUpperCase().contains("SWORD"))
                    && c.getItemMeta().getLore().equals(instance.enchantmentHandler.getbook().getItemMeta().getLore())) {
                instance.enchantmentHandler.createSyncTouch(item, null);
                event.setCancelled(true);
                player.setItemOnCursor(new ItemStack(Material.AIR));
                player.updateInventory();
            }
        }
        if (event.getRawSlot() > event.getView().getTopInventory().getSize() - 1) return;

        if (!event.getCurrentItem().hasItemMeta()) return;

        if (event.getSlot() != 64537
                && event.getInventory().getType() == InventoryType.ANVIL
                && event.getAction() != InventoryAction.NOTHING
                && event.getCurrentItem().getType() != Material.AIR) {
            ItemStack item = event.getCurrentItem();
            if (item.getType() == Material.HOPPER) {
                event.setCancelled(true);
            }
        }
    }
}