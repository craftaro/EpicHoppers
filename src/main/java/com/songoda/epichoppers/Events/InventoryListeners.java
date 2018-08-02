package com.songoda.epichoppers.Events;

import com.songoda.epichoppers.EpicHoppers;
import com.songoda.epichoppers.Hopper.Hopper;
import com.songoda.epichoppers.Utils.Debugger;
import com.songoda.epichoppers.Utils.Methods;
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

    private EpicHoppers instance;
    
    public InventoryListeners(EpicHoppers instance) {
        this.instance = instance;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        try {

            Inventory inv = e.getInventory();
            Player player = (Player) e.getWhoClicked();
            if (inv == null || e.getCurrentItem() == null) return;

            if (e.getCursor() != null && e.getCurrentItem() != null) {
                ItemStack c = e.getCursor();
                ItemStack item = e.getCurrentItem();
                if (c.hasItemMeta()
                        && c.getItemMeta().hasLore()
                        && c.getType() == Material.ENCHANTED_BOOK
                        && (item.getType().name().toUpperCase().contains("AXE") || item.getType().name().toUpperCase().contains("SPADE") || item.getType().name().toUpperCase().contains("SWORD"))
                        && c.getItemMeta().getLore().equals(instance.enchant.getbook().getItemMeta().getLore())) {
                    instance.enchant.createSyncTouch(item, null);
                    e.setCancelled(true);
                    player.setItemOnCursor(new ItemStack(Material.AIR));
                    player.updateInventory();


                }
            }
            if (!e.getCurrentItem().hasItemMeta()) return;

            if (doFilter(e)) return;

            if (e.getSlot() != 64537
                    && e.getInventory().getType() == InventoryType.ANVIL
                    && e.getAction() != InventoryAction.NOTHING
                    && e.getCurrentItem().getType() != Material.AIR) {
                ItemStack item = e.getCurrentItem();
                if (item.getType() == Material.HOPPER) {
                    e.setCancelled(true);
                }
            }

            if (!instance.inShow.containsKey(player.getUniqueId()) || instance.inFilter.containsKey(player.getUniqueId())) {
                return;
            }
            e.setCancelled(true);
            Hopper hopper = instance.getHopperManager().getHopper(instance.lastBlock.get(player));
            if (e.getCurrentItem().getItemMeta().hasDisplayName()
                    && e.getCurrentItem().getItemMeta().getDisplayName().equals(instance.getLocale().getMessage("interface.hopper.perltitle"))
                    && (instance.getConfig().getBoolean("Main.Allow Players To Teleport Through Hoppers") || player.hasPermission("EpicHoppers.Teleport"))) {
                if (e.isLeftClick()) {
                    if (hopper.getSyncedBlock() != null) {
                        instance.getTeleportHandler().tpPlayer(player, hopper);
                    }
                } else {
                    if (!hopper.isWalkOnTeleport()) {
                        player.sendMessage(instance.references.getPrefix() + instance.getLocale().getMessage("event.hopper.walkteleenabled"));
                        hopper.setWalkOnTeleport(true);
                    } else {
                        player.sendMessage(instance.references.getPrefix() + instance.getLocale().getMessage("event.hopper.walkteledisabled"));
                        hopper.setWalkOnTeleport(false);
                    }
                }
                player.closeInventory();


            } else if (e.getCurrentItem().getItemMeta().hasDisplayName() && e.getCurrentItem().getItemMeta().getDisplayName().equals(instance.getLocale().getMessage("interface.hopper.filtertitle")) && player.hasPermission("EpicHoppers.Filter")) {
                if (!e.getCurrentItem().getItemMeta().getDisplayName().equals("§l")) {
                    hopper.filter(player);
                }
            } else if (e.getSlot() == 11 && player.hasPermission("EpicHoppers.Upgrade.XP")) {
                if (!e.getCurrentItem().getItemMeta().getDisplayName().equals("§l")) {
                    hopper.upgrade("XP", player);
                    player.closeInventory();
                }
            } else if (e.getSlot() == 15 && player.hasPermission("EpicHoppers.Upgrade.ECO")) {
                if (!e.getCurrentItem().getItemMeta().getDisplayName().equals("§l")) {
                    hopper.upgrade("ECO", player);
                    player.closeInventory();
                }
            } else if (e.getSlot() == 22) {
                if (e.isRightClick()) {
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
                        instance.sync.put(player, instance.lastBlock.get(player));
                        player.sendMessage(instance.references.getPrefix() + instance.getLocale().getMessage("event.hopper.syncnext"));
                        hopper.timeout(player);
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
        if (!instance.inFilter.containsKey(player.getUniqueId())
                || e.getInventory() == null
                || !e.getInventory().equals(player.getOpenInventory().getTopInventory())) {
            return false;
        }
        if (e.getClick().equals(ClickType.SHIFT_LEFT)) {
            e.setCancelled(true);
            return true;
        }

        Hopper hopper = instance.getHopperManager().getHopper(instance.lastBlock.get(player));
        hopper.compile(player);
        if (e.getSlot() == 40) {
            instance.bsync.put(player, instance.lastBlock.get(player));
            player.sendMessage(instance.references.getPrefix() + instance.getLocale().getMessage("event.hopper.syncnext"));
            hopper.timeout(player);
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
            final Player player = (Player) event.getPlayer();
            instance.inShow.remove(player.getUniqueId());
            if (instance.inFilter.containsKey(player.getUniqueId())) {

                instance.getHopperManager().getHopper(instance.lastBlock.get(player)).compile(player);
                instance.inFilter.remove(player.getUniqueId());
            }
        } catch (Exception e) {
            Debugger.runReport(e);
        }
    }
}