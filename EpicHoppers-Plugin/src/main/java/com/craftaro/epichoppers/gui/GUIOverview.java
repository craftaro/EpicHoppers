package com.craftaro.epichoppers.gui;

import com.craftaro.core.SongodaPlugin;
import com.craftaro.core.compatibility.ServerVersion;
import com.craftaro.core.gui.CustomizableGui;
import com.craftaro.core.gui.GuiUtils;
import com.craftaro.core.third_party.com.cryptomorin.xseries.XMaterial;
import com.craftaro.core.utils.NumberUtils;
import com.craftaro.core.utils.TextUtils;
import com.craftaro.core.utils.TimeUtils;
import com.craftaro.epichoppers.EpicHoppersApi;
import com.craftaro.epichoppers.boost.BoostData;
import com.craftaro.epichoppers.hopper.Hopper;
import com.craftaro.epichoppers.hopper.levels.Level;
import com.craftaro.epichoppers.hopper.levels.modules.Module;
import com.craftaro.epichoppers.hopper.teleport.TeleportTrigger;
import com.craftaro.epichoppers.player.SyncType;
import com.craftaro.epichoppers.settings.Settings;
import com.craftaro.epichoppers.utils.CostType;
import com.craftaro.epichoppers.utils.Methods;
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
    private final SongodaPlugin plugin;
    private final Hopper hopper;
    private final Player player;

    private int task;

    public GUIOverview(SongodaPlugin plugin, Hopper hopper, Player player) {
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
            Bukkit.getScheduler().cancelTask(this.task);
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

        EpicHoppersApi.getApi().getPlayerDataManager().getPlayerData(this.player).setLastHopper(this.hopper);

        Level level = this.hopper.getLevel();

        Level nextLevel = EpicHoppersApi.getApi().getLevelManager().getHighestLevel().getLevel() > level.getLevel() ? EpicHoppersApi.getApi().getLevelManager().getLevel(level.getLevel() + 1) : null;

        ItemStack pearl = new ItemStack(Material.ENDER_PEARL, 1);
        ItemMeta pearlMeta = pearl.getItemMeta();
        pearlMeta.setDisplayName(this.plugin.getLocale().getMessage("interface.hopper.perltitle").getMessage());
        ArrayList<String> lorePearl = new ArrayList<>();
        String[] parts = this.plugin.getLocale().getMessage("interface.hopper.perllore2")
                .processPlaceholder(
                        "type",
                        this.hopper.getTeleportTrigger() == TeleportTrigger.DISABLED
                                ? this.plugin.getLocale().getMessage("general.word.disabled").getMessage()
                                : this.hopper.getTeleportTrigger().name()
                )
                .getMessage()
                .split("\\|");
        for (String line : parts) {
            lorePearl.add(TextUtils.formatText(line));
        }
        pearlMeta.setLore(lorePearl);
        pearl.setItemMeta(pearlMeta);

        ItemStack filter = new ItemStack(ServerVersion.isServerVersionAtLeast(ServerVersion.V1_13) ? Material.COMPARATOR : Material.valueOf("REDSTONE_COMPARATOR"), 1);
        ItemMeta filterMeta = filter.getItemMeta();
        filterMeta.setDisplayName(this.plugin.getLocale().getMessage("interface.hopper.filtertitle").getMessage());
        ArrayList<String> loreFilter = new ArrayList<>();
        parts = this.plugin.getLocale().getMessage("interface.hopper.filterlore").getMessage().split("\\|");
        for (String line : parts) {
            loreFilter.add(TextUtils.formatText(line));
        }
        filterMeta.setLore(loreFilter);
        filter.setItemMeta(filterMeta);


        ItemStack item = new ItemStack(Material.HOPPER, 1);
        ItemMeta itemmeta = item.getItemMeta();
        itemmeta.setDisplayName(this.plugin.getLocale().getMessage("interface.hopper.currentlevel").processPlaceholder("level", level.getLevel()).getMessage());
        List<String> lore = level.getDescription();
        if (this.plugin.getConfig().getBoolean("Main.Allow hopper Upgrading")) {
            lore.add("");
            if (nextLevel == null) {
                lore.add(this.plugin.getLocale().getMessage("interface.hopper.alreadymaxed").getMessage());
            } else {
                lore.add(this.plugin.getLocale().getMessage("interface.hopper.nextlevel").processPlaceholder("level", nextLevel.getLevel()).getMessage());
                lore.addAll(nextLevel.getDescription());
            }
        }

        BoostData boostData = EpicHoppersApi.getApi().getBoostManager().getBoost(this.hopper.getPlacedBy());
        if (boostData != null) {
            parts = this.plugin.getLocale().getMessage("interface.hopper.boostedstats")
                    .processPlaceholder("amount", Integer.toString(boostData.getMultiplier()))
                    .processPlaceholder("time", TimeUtils.makeReadable(boostData.getEndTime() - System.currentTimeMillis()))
                    .getMessage().split("\\|");
            lore.add("");
            for (String line : parts) {
                lore.add(TextUtils.formatText(line));
            }
        }

        itemmeta.setLore(lore);
        item.setItemMeta(itemmeta);

        ItemStack hook = new ItemStack(Material.TRIPWIRE_HOOK, 1);
        ItemMeta hookMeta = hook.getItemMeta();
        hookMeta.setDisplayName(this.plugin.getLocale().getMessage("interface.hopper.synchopper").getMessage());
        ArrayList<String> loreHook = new ArrayList<>();
        parts = this.plugin.getLocale().getMessage("interface.hopper.synclore")
                .processPlaceholder("amount", this.hopper.getLinkedBlocks().stream().distinct().count())
                .getMessage().split("\\|");
        for (String line : parts) {
            loreHook.add(TextUtils.formatText(line));
        }
        hookMeta.setLore(loreHook);
        hook.setItemMeta(hookMeta);

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

        boolean canFilter = level.isFilter() || this.player.hasPermission("EpicHoppers.Filter");
        boolean canTeleport = level.isTeleport() || this.player.hasPermission("EpicHoppers.Teleport");
        if (canFilter) {
            amount++;
        }
        if (canTeleport) {
            amount++;
        }

        List<Module> modules = level.getRegisteredModules()
                .stream()
                .filter(module -> module.getGUIButton(this.hopper) != null)
                .collect(Collectors.toList());

        amount += modules.size();

        Integer[] layout = layouts.get(amount);

        for (int ii = 0; ii < amount; ii++) {
            int slot = layout[ii];

            if (ii == 0) {
                setButton("sync", slot, hook,
                        (event) -> {
                            if (this.hopper.getLastPlayerOpened() != null && !this.hopper.getLastPlayerOpened().equals(this.player.getUniqueId())) {
                                this.plugin.getLocale().getMessage("event.hopper.syncdidnotplace").sendPrefixedMessage(this.player);
                                return;
                            }
                            this.hopper.clearLinkedBlocks();
                            EpicHoppersApi.getApi().getDataManager().deleteLinks(this.hopper);
                            if (event.clickType == ClickType.RIGHT) {
                                this.plugin.getLocale().getMessage("event.hopper.desync").sendPrefixedMessage(this.player);
                                constructGUI();
                                return;
                            } else {
                                EpicHoppersApi.getApi().getPlayerDataManager().getPlayerData(this.player).setSyncType(SyncType.REGULAR);
                                this.plugin.getLocale().getMessage("event.hopper.syncnext").sendPrefixedMessage(this.player);

                                if (level.getLinkAmount() > 1) {
                                    this.plugin.getLocale().getMessage("event.hopper.syncstart")
                                            .processPlaceholder("amount", level.getLinkAmount())
                                            .sendPrefixedMessage(this.player);
                                }

                                this.hopper.timeout(this.player);
                            }
                            this.player.closeInventory();
                        });
            } else if (canTeleport) {
                setButton("teleport", slot, pearl,
                        (event) -> {
                            if (event.clickType == ClickType.LEFT) {
                                if (this.hopper.getLinkedBlocks() != null) {
                                    EpicHoppersApi.getApi().getTeleportHandler().tpEntity(this.player, this.hopper);
                                    this.player.closeInventory();
                                }
                            } else {
                                if (this.hopper.getTeleportTrigger() == TeleportTrigger.DISABLED) {
                                    this.hopper.setTeleportTrigger(TeleportTrigger.SNEAK);
                                } else if (this.hopper.getTeleportTrigger() == TeleportTrigger.SNEAK) {
                                    this.hopper.setTeleportTrigger(TeleportTrigger.WALK_ON);
                                } else if (this.hopper.getTeleportTrigger() == TeleportTrigger.WALK_ON) {
                                    this.hopper.setTeleportTrigger(TeleportTrigger.DISABLED);
                                }
                                EpicHoppersApi.getApi().getDataManager().updateHopper(this.hopper);
                                constructGUI();
                            }
                        });
                canTeleport = false;
            } else if (canFilter) {
                setButton("filter", slot, filter, (event) -> {
                    this.hopper.setActivePlayer(this.player);
                    this.guiManager.showGUI(this.player, new GUIFilter(this.plugin, this.hopper, this.player));
                });
                canFilter = false;
            } else {
                if (modules.isEmpty()) {
                    break;
                }
                Module module = modules.get(0);
                modules.remove(module);
                setButton(module.getName().toLowerCase().replace(" ", "_"), slot, module.getGUIButton(this.hopper),
                        (event) -> module.runButtonPress(this.player, this.hopper, event.clickType));
            }
        }

        if (Settings.HOPPER_UPGRADING.getBoolean()) {
            if (Settings.UPGRADE_WITH_XP.getBoolean()
                    && level.getCostExperience() != -1
                    && this.player.hasPermission("EpicHoppers.Upgrade.XP")) {
                setButton("upgrade_xp", 1, 2, GuiUtils.createButtonItem(
                                Settings.XP_ICON.getMaterial(XMaterial.EXPERIENCE_BOTTLE),
                                this.plugin.getLocale().getMessage("interface.hopper.upgradewithxp").getMessage(),
                                nextLevel != null
                                        ? this.plugin.getLocale().getMessage("interface.hopper.upgradewithxplore")
                                        .processPlaceholder("cost", nextLevel.getCostExperience()).getMessage()
                                        : this.plugin.getLocale().getMessage("interface.hopper.alreadymaxed").getMessage()),
                        (event) -> {
                            this.hopper.upgrade(this.player, CostType.EXPERIENCE);
                            if (this.hopper.prepareForOpeningOverviewGui(this.player)) {
                                this.guiManager.showGUI(event.player, new GUIOverview(this.plugin, this.hopper, event.player));
                            }
                        });
            }
            if (Settings.UPGRADE_WITH_ECONOMY.getBoolean()
                    && level.getCostEconomy() != -1
                    && this.player.hasPermission("EpicHoppers.Upgrade.ECO")) {
                setButton("upgrade_economy", 1, 6, GuiUtils.createButtonItem(
                                Settings.ECO_ICON.getMaterial(XMaterial.SUNFLOWER),
                                this.plugin.getLocale().getMessage("interface.hopper.upgradewitheconomy").getMessage(),
                                nextLevel != null
                                        ? this.plugin.getLocale().getMessage("interface.hopper.upgradewitheconomylore")
                                        .processPlaceholder("cost", NumberUtils.formatNumber(nextLevel.getCostEconomy())).getMessage()
                                        : this.plugin.getLocale().getMessage("interface.hopper.alreadymaxed").getMessage()),
                        (event) -> {
                            this.hopper.upgrade(this.player, CostType.ECONOMY);
                            if (this.hopper.prepareForOpeningOverviewGui(this.player)) {
                                this.guiManager.showGUI(this.player, new GUIOverview(this.plugin, this.hopper, this.player));
                            }
                        });
            }
        }

        setItem("hopper", 13, item);

        this.hopper.setLastPlayerOpened(this.player.getUniqueId());
    }

    private void runTask() {
        this.task = Bukkit.getScheduler().scheduleSyncRepeatingTask(this.plugin, () -> {
            if (!this.inventory.getViewers().isEmpty()) {
                this.constructGUI();
            }
        }, 5L, 5L);
    }
}
