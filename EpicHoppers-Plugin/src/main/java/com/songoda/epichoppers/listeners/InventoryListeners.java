package com.songoda.epichoppers.listeners;

import com.songoda.epichoppers.EpicHoppersPlugin;
import com.songoda.epichoppers.api.hopper.Hopper;
import com.songoda.epichoppers.api.hopper.TeleportTrigger;
import com.songoda.epichoppers.hopper.EHopper;
import com.songoda.epichoppers.player.MenuType;
import com.songoda.epichoppers.player.PlayerData;
import com.songoda.epichoppers.player.SyncType;
import com.songoda.epichoppers.utils.Debugger;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/**
 * Created by songoda on 3/14/2017.
 */
public class InventoryListeners implements Listener {

    private final EpicHoppersPlugin instance;

    public InventoryListeners(EpicHoppersPlugin instance) {
        this.instance = instance;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        try {
            Inventory inv = event.getInventory();
            Player player = (Player) event.getWhoClicked();
            if (inv == null || event.getCurrentItem() == null) return;

            if (event.getRawSlot() > event.getView().getTopInventory().getSize() - 1) return;

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
            if (!event.getCurrentItem().hasItemMeta()) return;

            if (doFilter(event)) return;

            if (event.getSlot() != 64537
                    && event.getInventory().getType() == InventoryType.ANVIL
                    && event.getAction() != InventoryAction.NOTHING
                    && event.getCurrentItem().getType() != Material.AIR) {
                ItemStack item = event.getCurrentItem();
                if (item.getType() == Material.HOPPER) {
                    event.setCancelled(true);
                }
            }

            PlayerData playerData = instance.getPlayerDataManager().getPlayerData(player);

            if (playerData.getInMenu() == MenuType.CRAFTING) {
                if (event.getSlot() == 13) {
                    return;
                }
                event.setCancelled(true);
                return;
            }

            if (playerData.getInMenu() != MenuType.OVERVIEW) return;

            event.setCancelled(true);
            Hopper hopper = playerData.getLastHopper();
            if (event.getCurrentItem().getItemMeta().hasDisplayName()
                    && event.getCurrentItem().getItemMeta().getDisplayName().equals(instance.getLocale().getMessage("interface.hopper.perltitle"))
                    && (hopper.getLevel().isTeleport() || player.hasPermission("EpicHoppers.Teleport"))) {
                if (event.isLeftClick()) {
                    if (hopper.getSyncedBlock() != null) {
                        instance.getTeleportHandler().tpPlayer(player, hopper);
                    }
                } else {
                    if (hopper.getTeleportTrigger() == TeleportTrigger.DISABLED) {
                        hopper.setTeleportTrigger(TeleportTrigger.SNEAK);
                    } else if (hopper.getTeleportTrigger() == TeleportTrigger.SNEAK) {
                        hopper.setTeleportTrigger(TeleportTrigger.WALK_ON);
                    } else if (hopper.getTeleportTrigger() == TeleportTrigger.WALK_ON) {
                        hopper.setTeleportTrigger(TeleportTrigger.DISABLED);
                    }
                    ((EHopper) hopper).overview(player);
                    return;
                }
                player.closeInventory();
            } else if (event.getCurrentItem().getItemMeta().hasDisplayName() && event.getCurrentItem().getItemMeta().getDisplayName().equals(instance.getLocale().getMessage("interface.hopper.filtertitle")) && (hopper.getLevel().isFilter() || player.hasPermission("EpicHoppers.Filter"))) {
                if (!event.getCurrentItem().getItemMeta().getDisplayName().equals("§l")) {
                    ((EHopper) hopper).filter(player);
                }
            } else if (event.getSlot() == 11 && player.hasPermission("EpicHoppers.Upgrade.XP")) {
                if (!event.getCurrentItem().getItemMeta().getDisplayName().equals("§l")) {
                    ((EHopper) hopper).upgrade("XP", player);
                    player.closeInventory();
                }
            } else if (event.getSlot() == 15 && player.hasPermission("EpicHoppers.Upgrade.ECO")) {
                if (!event.getCurrentItem().getItemMeta().getDisplayName().equals("§l")) {
                    ((EHopper) hopper).upgrade("ECO", player);
                    player.closeInventory();
                }
            } else if (event.getCurrentItem().getItemMeta().getDisplayName().equals(instance.getLocale().getMessage("interface.hopper.craftingtitle"))) {
                ((EHopper) hopper).crafting(player);
            } else if (event.getCurrentItem().getItemMeta().getDisplayName().equals(instance.getLocale().getMessage("interface.hopper.synchopper"))) {
                if (event.isRightClick()) {
                    player.sendMessage(instance.references.getPrefix() + instance.getLocale().getMessage("event.hopper.desync"));
                    hopper.setSyncedBlock(null);
                } else {
                    boolean can = true;
                    if (hopper.getLastPlayer() != null) {
                        if (!hopper.getLastPlayer().equals(player.getUniqueId())) {
                            player.sendMessage(instance.references.getPrefix() + instance.getLocale().getMessage("event.hopper.syncdidnotplace"));
                            can = false;
                        }
                    }
                    if (can) {
                        playerData.setSyncType(SyncType.REGULAR);
                        player.sendMessage(instance.references.getPrefix() + instance.getLocale().getMessage("event.hopper.syncnext"));
                        ((EHopper) hopper).timeout(player);
                    }
                }
                player.closeInventory();
            }
        } catch (Exception ee) {
            Debugger.runReport(ee);
        }
    }


    private boolean doFilter(InventoryClickEvent e) {
        Player player = (Player) e.getWhoClicked();
        PlayerData playerData = instance.getPlayerDataManager().getPlayerData(player);
        if (playerData.getInMenu() != MenuType.FILTER
                || e.getInventory() == null
                || !e.getInventory().equals(player.getOpenInventory().getTopInventory())) {
            return false;
        }
        if (e.getClick().equals(ClickType.SHIFT_LEFT)) {
            e.setCancelled(true);
            return true;
        }

        Hopper hopper = playerData.getLastHopper();
        Bukkit.getScheduler().scheduleSyncDelayedTask(instance, () -> ((EHopper) hopper).compile(player), 1);
        if (e.getSlot() == 40) {
            playerData.setSyncType(SyncType.FILTERED);
            player.sendMessage(instance.references.getPrefix() + instance.getLocale().getMessage("event.hopper.syncnext"));
            ((EHopper) hopper).timeout(player);
            player.closeInventory();
            return true;
        }

        int[] a = {0, 1, 9, 10, 18, 19, 27, 28, 36, 37, 45, 46, 7, 8, 16, 17, 25, 26, 34, 35, 43, 44, 52, 53};
        e.setCancelled(true);
        for (int aa : a) {
            if (aa != e.getSlot()) continue;
            String name = "";
            if (e.getCurrentItem().hasItemMeta() && e.getCurrentItem().getItemMeta().hasDisplayName())
                name = e.getCurrentItem().getItemMeta().getDisplayName();
            if (!name.equals(instance.getLocale().getMessage("interface.filter.whitelist")) &&
                    !name.equals(instance.getLocale().getMessage("interface.filter.blacklist")) &&
                    !name.equals(instance.getLocale().getMessage("interface.filter.void"))) {
                e.setCancelled(false);
                return true;
            }

            if (e.getCursor().getType().equals(Material.AIR)) return true;
            if (e.getCursor().getAmount() != 1) {
                e.setCancelled(true);
                player.sendMessage(instance.references.getPrefix() + instance.getLocale().getMessage("event.hopper.onlyone"));
                return true;
            }

            e.setCancelled(false);
            Bukkit.getScheduler().scheduleSyncDelayedTask(instance, () -> player.setItemOnCursor(new ItemStack(Material.AIR)), 1L);
            return true;
        }
        return true;
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent e) {

        ItemStack item = e.getItemDrop().getItemStack();

        if (!item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) {
            return;
        }

        String name = item.getItemMeta().getDisplayName();

        if (!name.equals(instance.getLocale().getMessage("interface.filter.whitelist")) &&
                !name.equals(instance.getLocale().getMessage("interface.filter.blacklist")) &&
                !name.equals(instance.getLocale().getMessage("interface.filter.void"))) {
            return;
        }
        e.getItemDrop().remove();
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        try {
            Player player = (Player) event.getPlayer();
            PlayerData playerData = instance.getPlayerDataManager().getPlayerData(player);
            if (playerData.getInMenu() == MenuType.CRAFTING) {
                Hopper hopper = instance.getHopperManager().getHopperFromPlayer(player);
                ItemStack item = event.getInventory().getItem(13);
                hopper.setAutoCrafting(item == null ? Material.AIR : item.getType());
            }
            if (playerData.getInMenu() == MenuType.FILTER) {
                Hopper hopper = instance.getHopperManager().getHopperFromPlayer(player);
                ((EHopper) hopper).compile(player);
            }
            if (playerData.getInMenu() != MenuType.NOT_IN) {
                Hopper hopper = instance.getHopperManager().getHopperFromPlayer(player);
                if (hopper != null)
                    hopper.setLastPlayer(null);
            }
            playerData.setInMenu(MenuType.NOT_IN);
        } catch (Exception e) {
            Debugger.runReport(e);
        }
    }
}