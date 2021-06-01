package com.songoda.epichoppers.gui;

import com.songoda.core.compatibility.CompatibleMaterial;
import com.songoda.core.compatibility.ServerVersion;
import com.songoda.core.gui.CustomizableGui;
import com.songoda.core.gui.GuiUtils;
import com.songoda.core.utils.TextUtils;
import com.songoda.epichoppers.EpicHoppers;
import com.songoda.epichoppers.boost.BoostData;
import com.songoda.epichoppers.hopper.Hopper;
import com.songoda.epichoppers.hopper.levels.Level;
import com.songoda.epichoppers.hopper.levels.modules.Module;
import com.songoda.epichoppers.player.SyncType;
import com.songoda.epichoppers.settings.Settings;
import com.songoda.epichoppers.utils.CostType;
import com.songoda.epichoppers.utils.Methods;
import com.songoda.epichoppers.hopper.teleport.TeleportTrigger;
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
import java.util.stream.Collectors;

public class GUIOverview extends CustomizableGui {

    private final EpicHoppers plugin;
    private final Hopper hopper;
    private final Player player;

    private int task;

    public GUIOverview(EpicHoppers plugin, Hopper hopper, Player player) {
        super(plugin, "overview");
        this.plugin = plugin;
        this.hopper = hopper;
        this.player = player;

        setRows(3);
        setTitle(Methods.formatName(hopper.getLevel().getLevel()));
        runTask();
        constructGUI();
        this.setOnClose(action -> {
            hopper.setActivePlayer(null);
            Bukkit.getScheduler().cancelTask(task);
        });
    }

    private void constructGUI() {
        ItemStack glass1 = GuiUtils.getBorderItem(Settings.GLASS_TYPE_1.getMaterial());
        ItemStack glass2 = GuiUtils.getBorderItem(Settings.GLASS_TYPE_2.getMaterial());
        ItemStack glass3 = GuiUtils.getBorderItem(Settings.GLASS_TYPE_3.getMaterial());

        setDefaultItem(glass1);

        mirrorFill("mirrorfill_1", 0, 0, true, true, glass2);
        mirrorFill("mirrorfill_2", 0, 1, true, true, glass2);
        mirrorFill("mirrorfill_3", 0, 2, true, true, glass3);
        mirrorFill("mirrorfill_4", 1, 0, false, true, glass2);
        mirrorFill("mirrorfill_5", 1, 1, false, true, glass3);

        plugin.getPlayerDataManager().getPlayerData(player).setLastHopper(hopper);

        Level level = hopper.getLevel();

        Level nextLevel = plugin.getLevelManager().getHighestLevel().getLevel() > level.getLevel() ? plugin.getLevelManager().getLevel(level.getLevel() + 1) : null;

        ItemStack perl = new ItemStack(Material.ENDER_PEARL, 1);
        ItemMeta perlmeta = perl.getItemMeta();
        perlmeta.setDisplayName(plugin.getLocale().getMessage("interface.hopper.perltitle").getMessage());
        ArrayList<String> loreperl = new ArrayList<>();
        String[] parts = plugin.getLocale().getMessage("interface.hopper.perllore2")
                .processPlaceholder("type", hopper.getTeleportTrigger() == TeleportTrigger.DISABLED
                        ? plugin.getLocale().getMessage("general.word.disabled").getMessage()
                        : hopper.getTeleportTrigger().name()).getMessage().split("\\|");
        for (String line : parts) {
            loreperl.add(TextUtils.formatText(line));
        }
        perlmeta.setLore(loreperl);
        perl.setItemMeta(perlmeta);

        ItemStack filter = new ItemStack(ServerVersion.isServerVersionAtLeast(ServerVersion.V1_13) ? Material.COMPARATOR : Material.valueOf("REDSTONE_COMPARATOR"), 1);
        ItemMeta filtermeta = filter.getItemMeta();
        filtermeta.setDisplayName(plugin.getLocale().getMessage("interface.hopper.filtertitle").getMessage());
        ArrayList<String> lorefilter = new ArrayList<>();
        parts = plugin.getLocale().getMessage("interface.hopper.filterlore").getMessage().split("\\|");
        for (String line : parts) {
            lorefilter.add(TextUtils.formatText(line));
        }
        filtermeta.setLore(lorefilter);
        filter.setItemMeta(filtermeta);


        ItemStack item = new ItemStack(Material.HOPPER, 1);
        ItemMeta itemmeta = item.getItemMeta();
        itemmeta.setDisplayName(plugin.getLocale().getMessage("interface.hopper.currentlevel").processPlaceholder("level", level.getLevel()).getMessage());
        List<String> lore = level.getDescription();
        if (plugin.getConfig().getBoolean("Main.Allow hopper Upgrading")) {
            lore.add("");
            if (nextLevel == null)
                lore.add(plugin.getLocale().getMessage("interface.hopper.alreadymaxed").getMessage());
            else {
                lore.add(plugin.getLocale().getMessage("interface.hopper.nextlevel").processPlaceholder("level", nextLevel.getLevel()).getMessage());
                lore.addAll(nextLevel.getDescription());
            }
        }

        BoostData boostData = plugin.getBoostManager().getBoost(hopper.getPlacedBy());
        if (boostData != null) {
            parts = plugin.getLocale().getMessage("interface.hopper.boostedstats")
                    .processPlaceholder("amount", Integer.toString(boostData.getMultiplier()))
                    .processPlaceholder("time", Methods.makeReadable(boostData.getEndTime() - System.currentTimeMillis()))
                    .getMessage().split("\\|");
            lore.add("");
            for (String line : parts)
                lore.add(TextUtils.formatText(line));
        }

        itemmeta.setLore(lore);
        item.setItemMeta(itemmeta);

        ItemStack hook = new ItemStack(Material.TRIPWIRE_HOOK, 1);
        ItemMeta hookmeta = hook.getItemMeta();
        hookmeta.setDisplayName(plugin.getLocale().getMessage("interface.hopper.synchopper").getMessage());
        ArrayList<String> lorehook = new ArrayList<>();
        parts = plugin.getLocale().getMessage("interface.hopper.synclore")
                .processPlaceholder("amount", hopper.getLinkedBlocks().stream().distinct().count())
                .getMessage().split("\\|");
        for (String line : parts) {
            lorehook.add(TextUtils.formatText(line));
        }
        hookmeta.setLore(lorehook);
        hook.setItemMeta(hookmeta);

        Map<Integer, Integer[]> layouts = new HashMap<>();
        layouts.put(1, new Integer[]{22});
        layouts.put(2, new Integer[]{22, 4});
        layouts.put(3, new Integer[]{22, 3, 5});
        layouts.put(4, new Integer[]{23, 3, 5, 21});
        layouts.put(5, new Integer[]{23, 3, 5, 21, 22});
        layouts.put(6, new Integer[]{23, 3, 4, 5, 21, 22});
        layouts.put(7, new Integer[]{23, 3, 4, 5, 21, 22, 12});
        layouts.put(8, new Integer[]{23, 3, 4, 5, 21, 22, 12, 14});
        layouts.put(9, new Integer[]{23, 3, 4, 5, 21, 22, 12, 14, 20});
        layouts.put(10, new Integer[]{23, 3, 4, 5, 21, 22, 12, 14, 20, 24});

        int amount = 1;

        boolean canFilter = level.isFilter() || player.hasPermission("EpicHoppers.Filter");
        boolean canTeleport = level.isTeleport() || player.hasPermission("EpicHoppers.Teleport");
        if (canFilter) amount++;
        if (canTeleport) amount++;

        List<Module> modules = level.getRegisteredModules().stream().filter(module ->
                module.getGUIButton(hopper) != null).collect(Collectors.toList());

        amount += modules.size();

        Integer[] layout = layouts.get(amount);

        for (int ii = 0; ii < amount; ii++) {
            int slot = layout[ii];

            if (ii == 0) {
                setButton("sync", slot, hook,
                        (event) -> {
                            if (hopper.getLastPlayerOpened() != null && !hopper.getLastPlayerOpened().equals(player.getUniqueId())) {
                                plugin.getLocale().getMessage("event.hopper.syncdidnotplace").sendPrefixedMessage(player);
                                return;
                            }
                            hopper.clearLinkedBlocks();
                            plugin.getDataManager().deleteLinks(hopper);
                            if (event.clickType == ClickType.RIGHT) {
                                plugin.getLocale().getMessage("event.hopper.desync").sendPrefixedMessage(player);
                                constructGUI();
                                return;
                            } else {
                                plugin.getPlayerDataManager().getPlayerData(player).setSyncType(SyncType.REGULAR);
                                plugin.getLocale().getMessage("event.hopper.syncnext").sendPrefixedMessage(player);

                                if (level.getLinkAmount() > 1)
                                    plugin.getLocale().getMessage("event.hopper.syncstart")
                                            .processPlaceholder("amount", level.getLinkAmount())
                                            .sendPrefixedMessage(player);

                                hopper.timeout(player);
                            }
                            player.closeInventory();
                        });
            } else if (canTeleport) {
                setButton("teleport", slot, perl,
                        (event) -> {
                            if (event.clickType == ClickType.LEFT) {
                                if (hopper.getLinkedBlocks() != null) {
                                    plugin.getTeleportHandler().tpEntity(player, hopper);
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
                                plugin.getDataManager().updateHopper(hopper);
                                constructGUI();
                            }
                        });
                canTeleport = false;
            } else if (canFilter) {
                setButton("filter", slot, filter, (event) -> {
                    hopper.setActivePlayer(player);
                    guiManager.showGUI(player, new GUIFilter(plugin, hopper, player));
                });
                canFilter = false;
            } else {
                if (modules.isEmpty()) break;
                Module module = modules.get(0);
                modules.remove(module);
                setButton(module.getName().toLowerCase().replace(" ", "_"), slot, module.getGUIButton(hopper),
                        (event) -> module.runButtonPress(player, hopper, event.clickType));
            }
        }

        if (Settings.HOPPER_UPGRADING.getBoolean()) {
            if (Settings.UPGRADE_WITH_XP.getBoolean()
                    && level.getCostExperience() != -1
                    && player.hasPermission("EpicHoppers.Upgrade.XP")) {
                setButton("upgrade_xp", 1, 2, GuiUtils.createButtonItem(
                        Settings.XP_ICON.getMaterial(CompatibleMaterial.EXPERIENCE_BOTTLE),
                        plugin.getLocale().getMessage("interface.hopper.upgradewithxp").getMessage(),
                        nextLevel != null
                                ? plugin.getLocale().getMessage("interface.hopper.upgradewithxplore")
                                .processPlaceholder("cost", nextLevel.getCostExperience()).getMessage()
                                : plugin.getLocale().getMessage("interface.hopper.alreadymaxed").getMessage()),
                        (event) -> {
                            hopper.upgrade(player, CostType.EXPERIENCE);
                            hopper.overview(guiManager, player);
                        });
            }
            if (Settings.UPGRADE_WITH_ECONOMY.getBoolean()
                    && level.getCostEconomy() != -1
                    && player.hasPermission("EpicHoppers.Upgrade.ECO")) {
                setButton("upgrade_economy", 1, 6, GuiUtils.createButtonItem(
                        Settings.ECO_ICON.getMaterial(CompatibleMaterial.SUNFLOWER),
                        plugin.getLocale().getMessage("interface.hopper.upgradewitheconomy").getMessage(),
                        nextLevel != null
                                ? plugin.getLocale().getMessage("interface.hopper.upgradewitheconomylore")
                                .processPlaceholder("cost", Methods.formatEconomy(nextLevel.getCostEconomy())).getMessage()
                                : plugin.getLocale().getMessage("interface.hopper.alreadymaxed").getMessage()),
                        (event) -> {
                            hopper.upgrade(player, CostType.ECONOMY);
                            hopper.overview(guiManager, player);
                        });
            }
        }

        setItem("hopper", 13, item);

        hopper.setLastPlayerOpened(player.getUniqueId());
    }

    private void runTask() {
        task = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            if (!inventory.getViewers().isEmpty())
                this.constructGUI();
        }, 5L, 5L);
    }
}
