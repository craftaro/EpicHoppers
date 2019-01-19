package com.songoda.epichoppers.gui;

import com.songoda.epichoppers.EpicHoppersPlugin;
import com.songoda.epichoppers.api.CostType;
import com.songoda.epichoppers.api.hopper.TeleportTrigger;
import com.songoda.epichoppers.api.hopper.levels.Level;
import com.songoda.epichoppers.boost.BoostData;
import com.songoda.epichoppers.hopper.EHopper;
import com.songoda.epichoppers.player.SyncType;
import com.songoda.epichoppers.utils.Methods;
import com.songoda.epichoppers.utils.gui.AbstractGUI;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GUIHoppperOverview extends AbstractGUI {

    private final EpicHoppersPlugin plugin;
    private final EHopper hopper;
    private final Player player;

    private int task;

    public GUIHoppperOverview(EpicHoppersPlugin plugin, EHopper hopper, Player player) {
        super(player);
        this.plugin = plugin;
        this.player = player;
        this.hopper = hopper;

        init(Methods.formatName(hopper.getLevel().getLevel(), false), 27);
        runTask();
    }

    @Override
    protected void constructGUI() {
        plugin.getPlayerDataManager().getPlayerData(player).setLastHopper(hopper);

        Level level = hopper.getLevel();

        Level nextLevel = plugin.getLevelManager().getHighestLevel().getLevel() > level.getLevel() ? plugin.getLevelManager().getLevel(level.getLevel() + 1) : null;

        ItemStack perl = new ItemStack(Material.ENDER_PEARL, 1);
        ItemMeta perlmeta = perl.getItemMeta();
        perlmeta.setDisplayName(plugin.getLocale().getMessage("interface.hopper.perltitle"));
        ArrayList<String> loreperl = new ArrayList<>();
        String[] parts = plugin.getLocale().getMessage("interface.hopper.perllore2", hopper.getTeleportTrigger() == TeleportTrigger.DISABLED ? plugin.getLocale().getMessage("general.word.disabled") : hopper.getTeleportTrigger().name()).split("\\|");
        for (String line : parts) {
            loreperl.add(Methods.formatText(line));
        }
        perlmeta.setLore(loreperl);
        perl.setItemMeta(perlmeta);

        ItemStack filter = new ItemStack(Material.COMPARATOR, 1);
        ItemMeta filtermeta = filter.getItemMeta();
        filtermeta.setDisplayName(plugin.getLocale().getMessage("interface.hopper.filtertitle"));
        ArrayList<String> lorefilter = new ArrayList<>();
        parts = plugin.getLocale().getMessage("interface.hopper.filterlore").split("\\|");
        for (String line : parts) {
            lorefilter.add(Methods.formatText(line));
        }
        filtermeta.setLore(lorefilter);
        filter.setItemMeta(filtermeta);

        ItemStack crafting = new ItemStack(Material.CRAFTING_TABLE, 1);
        ItemMeta craftingmeta = crafting.getItemMeta();
        craftingmeta.setDisplayName(plugin.getLocale().getMessage("interface.hopper.craftingtitle"));
        ArrayList<String> lorecrafting = new ArrayList<>();
        parts = plugin.getLocale().getMessage("interface.hopper.craftinglore").split("\\|");
        for (String line : parts) {
            lorecrafting.add(Methods.formatText(line));
        }
        craftingmeta.setLore(lorecrafting);
        crafting.setItemMeta(craftingmeta);

        ItemStack sell = new ItemStack(Material.SUNFLOWER, 1);
        ItemMeta sellmeta = sell.getItemMeta();
        sellmeta.setDisplayName(plugin.getLocale().getMessage("interface.hopper.selltitle"));
        ArrayList<String> loresell = new ArrayList<>();
        parts = plugin.getLocale().getMessage("interface.hopper.selllore", hopper.getAutoSellTimer() == -9999 ? "\u221E" : (int) Math.floor(hopper.getAutoSellTimer() / 20)).split("\\|");
        for (String line : parts) {
            loresell.add(Methods.formatText(line));
        }
        sellmeta.setLore(loresell);
        sell.setItemMeta(sellmeta);


        ItemStack item = new ItemStack(Material.HOPPER, 1);
        ItemMeta itemmeta = item.getItemMeta();
        itemmeta.setDisplayName(plugin.getLocale().getMessage("interface.hopper.currentlevel", level.getLevel()));
        List<String> lore = level.getDescription();
        if (plugin.getConfig().getBoolean("Main.Allow hopper Upgrading")) {
            lore.add("");
            if (nextLevel == null) lore.add(plugin.getLocale().getMessage("interface.hopper.alreadymaxed"));
            else {
                lore.add(plugin.getLocale().getMessage("interface.hopper.nextlevel", nextLevel.getLevel()));
                lore.addAll(nextLevel.getDescription());
            }
        }

        BoostData boostData = plugin.getBoostManager().getBoost(hopper.getPlacedBy());
        if (boostData != null) {
            parts = plugin.getLocale().getMessage("interface.hopper.boostedstats", Integer.toString(boostData.getMultiplier()), Methods.makeReadable(boostData.getEndTime() - System.currentTimeMillis())).split("\\|");
            lore.add("");
            for (String line : parts)
                lore.add(Methods.formatText(line));
        }

        itemmeta.setLore(lore);
        item.setItemMeta(itemmeta);

        ItemStack hook = new ItemStack(Material.TRIPWIRE_HOOK, 1);
        ItemMeta hookmeta = hook.getItemMeta();
        hookmeta.setDisplayName(plugin.getLocale().getMessage("interface.hopper.synchopper"));
        ArrayList<String> lorehook = new ArrayList<>();
        parts = plugin.getLocale().getMessage("interface.hopper.synclore", hopper.getLinkedBlocks().size()).split("\\|");
        for (String line : parts) {
            lorehook.add(Methods.formatText(line));
        }
        hookmeta.setLore(lorehook);
        hook.setItemMeta(hookmeta);

        ItemStack itemXP = new ItemStack(Material.valueOf(plugin.getConfig().getString("Interfaces.XP Icon")), 1);
        ItemMeta itemmetaXP = itemXP.getItemMeta();
        itemmetaXP.setDisplayName(plugin.getLocale().getMessage("interface.hopper.upgradewithxp"));
        ArrayList<String> loreXP = new ArrayList<>();
        if (nextLevel != null)
            loreXP.add(plugin.getLocale().getMessage("interface.hopper.upgradewithxplore", nextLevel.getCostExperience()));
        else
            loreXP.add(plugin.getLocale().getMessage("interface.hopper.alreadymaxed"));
        itemmetaXP.setLore(loreXP);
        itemXP.setItemMeta(itemmetaXP);

        ItemStack itemECO = new ItemStack(Material.valueOf(plugin.getConfig().getString("Interfaces.Economy Icon")), 1);
        ItemMeta itemmetaECO = itemECO.getItemMeta();
        itemmetaECO.setDisplayName(plugin.getLocale().getMessage("interface.hopper.upgradewitheconomy"));
        ArrayList<String> loreECO = new ArrayList<>();
        if (nextLevel != null)
            loreECO.add(plugin.getLocale().getMessage("interface.hopper.upgradewitheconomylore", Methods.formatEconomy(nextLevel.getCostEconomy())));
        else
            loreECO.add(plugin.getLocale().getMessage("interface.hopper.alreadymaxed"));
        itemmetaECO.setLore(loreECO);
        itemECO.setItemMeta(itemmetaECO);

        int nu = 0;
        while (nu != 27) {
            inventory.setItem(nu, Methods.getGlass());
            nu++;
        }

        Map<Integer, Integer[]> layouts = new HashMap<>();
        layouts.put(1, new Integer[]{22});
        layouts.put(2, new Integer[]{22, 4});
        layouts.put(3, new Integer[]{22, 3, 5});
        layouts.put(4, new Integer[]{23, 3, 5, 21});
        layouts.put(5, new Integer[]{23, 3, 5, 21, 22});

        int amount = 1;

        boolean canFilter = level.isFilter() || player.hasPermission("EpicHoppers.Filter");
        boolean canTeleport = level.isTeleport() || player.hasPermission("EpicHoppers.Teleport");
        boolean canCraft = level.getRegisteredModules().removeIf(e -> e.getName().equals("AutoCrafting"));
        boolean canAutoSell = level.getAutoSell() != 0;
        if (canFilter) amount++;
        if (canTeleport) amount++;
        if (canAutoSell) amount++;
        if (canCraft) amount++;

        Integer[] layout = layouts.get(amount);

        for (int ii = 0; ii < amount; ii++) {
            int slot = layout[ii];

            if (ii == 0) {
                inventory.setItem(slot, hook);
            } else if (canTeleport) {
                inventory.setItem(slot, perl);
                canTeleport = false;
            } else if (canFilter) {
                inventory.setItem(slot, filter);
                canFilter = false;
            } else if (canCraft) {
                inventory.setItem(slot, crafting);
                canCraft = false;
            } else if (canAutoSell) {
                inventory.setItem(slot, sell);
                canAutoSell = false;
            }
        }

        if (plugin.getConfig().getBoolean("Main.Upgrade With XP")
                && player.hasPermission("EpicHoppers.Upgrade.XP")
                && level.getCostExperience() != -1)
            inventory.setItem(11, itemXP);

        inventory.setItem(13, item);

        if (plugin.getConfig().getBoolean("Main.Upgrade With Economy")
                && player.hasPermission("EpicHoppers.Upgrade.ECO")
                && level.getCostEconomy() != -1)
            inventory.setItem(15, itemECO);

        inventory.setItem(0, Methods.getBackgroundGlass(true));
        inventory.setItem(1, Methods.getBackgroundGlass(true));
        inventory.setItem(2, Methods.getBackgroundGlass(false));
        inventory.setItem(6, Methods.getBackgroundGlass(false));
        inventory.setItem(7, Methods.getBackgroundGlass(true));
        inventory.setItem(8, Methods.getBackgroundGlass(true));
        inventory.setItem(9, Methods.getBackgroundGlass(true));
        inventory.setItem(10, Methods.getBackgroundGlass(false));
        inventory.setItem(16, Methods.getBackgroundGlass(false));
        inventory.setItem(17, Methods.getBackgroundGlass(true));
        inventory.setItem(18, Methods.getBackgroundGlass(true));
        inventory.setItem(19, Methods.getBackgroundGlass(true));
        inventory.setItem(20, Methods.getBackgroundGlass(false));
        inventory.setItem(24, Methods.getBackgroundGlass(false));
        inventory.setItem(25, Methods.getBackgroundGlass(true));
        inventory.setItem(26, Methods.getBackgroundGlass(true));

        hopper.setLastPlayer(player.getUniqueId());
    }

    private void runTask() {
        task = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, this::constructGUI, 5L, 5L);
    }

    @Override
    protected void registerClickables() {
        registerClickable(11, ((player, inventory, cursor, slot, type) -> {
            if (!player.hasPermission("EpicHoppers.Upgrade.XP")
                    || hopper.getLevel().getCostExperience() == -1) return;
            hopper.upgrade(player, CostType.EXPERIENCE);
            this.hopper.overview(player);
        }));

        registerClickable(15, ((player, inventory, cursor, slot, type) -> {
            if (!player.hasPermission("EpicHoppers.Upgrade.ECO")
                    || hopper.getLevel().getCostExperience() == -1) return;
            hopper.upgrade(player, CostType.ECONOMY);
            this.hopper.overview(player);
        }));

        registerClickable(3, 23, ((player, inventory, cursor, slot, type) -> {
            if (inventory.getItem(slot).getItemMeta()
                    .getDisplayName().equals(plugin.getLocale().getMessage("interface.hopper.selltitle"))) {
                if (hopper.getAutoSellTimer() == -9999) {
                    hopper.setAutoSellTimer(0);
                } else {
                    hopper.setAutoSellTimer(-9999);
                }
            } else if (inventory.getItem(slot).getItemMeta()
                    .getDisplayName().equals(plugin.getLocale().getMessage("interface.hopper.craftingtitle"))) {
                new GUICrafting(plugin, hopper, player);
            } else if (inventory.getItem(slot).getItemMeta()
                    .getDisplayName().equals(plugin.getLocale().getMessage("interface.hopper.filtertitle"))) {
                new GUIFilter(plugin, hopper, player);
            } else if (inventory.getItem(slot).getItemMeta()
                    .getDisplayName().equals(plugin.getLocale().getMessage("interface.hopper.perltitle"))) {
                if (type == ClickType.LEFT) {
                    if (hopper.getLinkedBlocks() != null) {
                        plugin.getTeleportHandler().tpPlayer(player, hopper);
                        player.closeInventory();
                    }
                } else {
                    if (hopper.getTeleportTrigger() == TeleportTrigger.DISABLED) {
                        hopper.setTeleportTrigger(TeleportTrigger.SNEAK);
                    } else if (hopper.getTeleportTrigger() == TeleportTrigger.SNEAK) {
                        hopper.setTeleportTrigger(TeleportTrigger.WALK_ON);
                    } else if (hopper.getTeleportTrigger() == TeleportTrigger.WALK_ON) {
                        hopper.setTeleportTrigger(TeleportTrigger.DISABLED);
                    }
                    constructGUI();
                }
            } else if (inventory.getItem(slot).getItemMeta()
                    .getDisplayName().equals(plugin.getLocale().getMessage("interface.hopper.synchopper"))) {
                if (type == ClickType.RIGHT) {
                    player.sendMessage(plugin.references.getPrefix() + plugin.getLocale().getMessage("event.hopper.desync"));
                    hopper.clearLinkedBlocks();
                } else {
                    if (hopper.getLastPlayer() != null && !hopper.getLastPlayer().equals(player.getUniqueId())) {
                        player.sendMessage(plugin.references.getPrefix() + plugin.getLocale().getMessage("event.hopper.syncdidnotplace"));
                        return;
                    }
                    plugin.getPlayerDataManager().getPlayerData(player).setSyncType(SyncType.REGULAR);
                    hopper.clearLinkedBlocks();
                    player.sendMessage(plugin.references.getPrefix() + plugin.getLocale().getMessage("event.hopper.syncnext"));
                    hopper.timeout(player);
                }
                player.closeInventory();
            }
        }));
    }

    @Override
    protected void registerOnCloses() {
        registerOnClose(((player1, inventory1) -> Bukkit.getScheduler().cancelTask(task)));
    }
}
