package com.craftaro.epichoppers.gui;

import com.craftaro.core.SongodaPlugin;
import com.craftaro.core.gui.CustomizableGui;
import com.craftaro.core.gui.GuiUtils;
import com.craftaro.core.third_party.com.cryptomorin.xseries.XMaterial;
import com.craftaro.core.utils.TextUtils;
import com.craftaro.epichoppers.EpicHoppersApi;
import com.craftaro.epichoppers.hopper.Filter;
import com.craftaro.epichoppers.hopper.Hopper;
import com.craftaro.epichoppers.hopper.ItemType;
import com.craftaro.epichoppers.settings.Settings;
import com.craftaro.epichoppers.utils.Methods;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class GUIAutoSellFilter extends CustomizableGui {
    private static final List<GUIAutoSellFilter> OPEN_INVENTORIES = new ArrayList<>();

    private final Hopper hopper;

    private final int[] whiteListSlots = {9, 10, 11, 18, 19, 20, 27, 28, 29, 36, 37, 38};
    private final int[] blackListSlots = {12, 13, 14, 21, 22, 23, 30, 31, 32, 39, 40, 41};

    public GUIAutoSellFilter(SongodaPlugin plugin, Hopper hopper) {
        super(plugin, "autosell");
        this.hopper = hopper;

        setRows(6);
        setTitle(TextUtils.formatText(Methods.formatName(hopper.getLevel().getLevel()) + " &8-&f AutoSell Filter"));
        setDefaultItem(null);
        setAcceptsItems(true);

        setOnOpen((event) -> GUIAutoSellFilter.OPEN_INVENTORIES.add(this));

        setOnClose((event) -> {
            GUIAutoSellFilter.OPEN_INVENTORIES.remove(this);
            hopper.setActivePlayer(null);
            compile();
        });

        Filter filter = hopper.getFilter();

        // Fill
        ItemStack glass1 = GuiUtils.getBorderItem(Settings.GLASS_TYPE_1.getMaterial());
        ItemStack glass2 = GuiUtils.getBorderItem(Settings.GLASS_TYPE_2.getMaterial());

        mirrorFill("mirrorfill_1", 0, 6, true, false, glass2);
        mirrorFill("mirrorfill_2", 0, 7, true, false, glass2);
        mirrorFill("mirrorfill_3", 0, 8, true, false, glass2);
        mirrorFill("mirrorfill_4", 1, 6, true, false, glass2);
        mirrorFill("mirrorfill_5", 1, 8, true, false, glass2);
        mirrorFill("mirrorfill_6", 2, 6, true, false, glass2);
        mirrorFill("mirrorfill_7", 2, 7, true, false, glass1);
        mirrorFill("mirrorfill_8", 2, 8, true, false, glass2);
        mirrorFill("mirrorfill_9", 4, 7, false, false, glass1);

        setButton("back", 8, GuiUtils.createButtonItem(XMaterial.ARROW.parseItem(),
                        plugin.getLocale().getMessage("general.nametag.back").getMessage()),
                (event) -> {
                    if (hopper.prepareForOpeningOverviewGui(event.player)) {
                        this.guiManager.showGUI(event.player, new GUIOverview(plugin, hopper, event.player));
                    }
                    compile();
                });

        // Whitelist
        ItemStack indicatorItem = XMaterial.WHITE_STAINED_GLASS_PANE.parseItem();
        ItemMeta indicatorMeta = indicatorItem.getItemMeta();
        indicatorMeta.setDisplayName(plugin.getLocale().getMessage("interface.autosell-filter.whitelist").getMessage());
        indicatorItem.setItemMeta(indicatorMeta);

        int[] whiteSlots = {0, 1, 2, 45, 46, 47};
        for (int nu : whiteSlots) {
            setItem("whitelist", nu, indicatorItem);
        }

        int num = 0;
        for (ItemStack m : filter.getAutoSellWhiteList()) {
            if (num >= filter.getAutoSellWhiteList().size()) {
                break;
            }
            setItem(this.whiteListSlots[num], new ItemStack(m));
            num++;
        }

        // Blacklist
        indicatorItem = XMaterial.BLACK_STAINED_GLASS_PANE.parseItem();
        indicatorMeta = indicatorItem.getItemMeta();
        indicatorMeta.setDisplayName(plugin.getLocale().getMessage("interface.autosell-filter.blacklist").getMessage());
        indicatorItem.setItemMeta(indicatorMeta);

        int[] blackSlots = {3, 4, 5, 48, 49, 50};
        for (int nu : blackSlots) {
            setItem("blacklist", nu, indicatorItem);
        }

        num = 0;
        for (ItemStack m : filter.getAutoSellBlackList()) {
            if (num >= filter.getAutoSellBlackList().size()) {
                break;
            }
            setItem(this.blackListSlots[num], new ItemStack(m));
            num++;
        }

        // Info item
        indicatorItem = XMaterial.PAPER.parseItem();
        indicatorMeta = indicatorItem.getItemMeta();

        indicatorMeta.setDisplayName(plugin.getLocale().getMessage("interface.autosell-filter.infotitle").getMessage());
        ArrayList<String> loreInfo = new ArrayList<>();
        String[] parts = plugin
                .getLocale()
                .getMessage("interface.autosell-filter.infolore")
                .getMessage()
                .split("\\|");

        for (String line : parts) {
            loreInfo.add(TextUtils.formatText(line));
        }

        indicatorMeta.setLore(loreInfo);
        indicatorItem.setItemMeta(indicatorMeta);

        setItem("info", 16, indicatorItem);

        setUnlockedRange(9, 14);
        setUnlockedRange(18, 23);
        setUnlockedRange(27, 32);
        setUnlockedRange(36, 41);
    }

    private void compile() {
        ItemStack[] items = this.inventory.getContents();

        Filter filter = this.hopper.getFilter();

        List<ItemStack> whiteListItems = new ArrayList<>();
        List<ItemStack> blackListItems = new ArrayList<>();

        for (int i = 0; i < items.length; i++) {
            for (int slot : this.whiteListSlots) {
                if (slot != i) {
                    continue;
                }

                if (items[i] != null && !items[i].getType().isAir()) {
                    ItemStack item = items[i];
                    if (item.getAmount() != 1) {
                        item.setAmount(item.getAmount() - 1);
                        Bukkit.getPlayer(this.hopper.getLastPlayerOpened()).getInventory().addItem(item);
                        item.setAmount(1);
                    }

                    whiteListItems.add(item);
                }
            }

            for (int slot : this.blackListSlots) {
                if (slot != i) {
                    continue;
                }

                if (items[i] != null && !items[i].getType().isAir()) {
                    ItemStack item = items[i];
                    if (item.getAmount() != 1) {
                        item.setAmount(item.getAmount() - 1);
                        Bukkit.getPlayer(this.hopper.getLastPlayerOpened()).getInventory().addItem(item);
                        item.setAmount(1);
                    }
                    blackListItems.add(item);
                }
            }
        }

        filter.setAutoSellWhiteList(whiteListItems);
        filter.setAutoSellBlackList(blackListItems);
        EpicHoppersApi.getApi().getDataManager().updateItems(this.hopper, ItemType.AUTO_SELL_WHITELIST, whiteListItems);
        EpicHoppersApi.getApi().getDataManager().updateItems(this.hopper, ItemType.AUTO_SELL_BLACKLIST, blackListItems);
    }

    public static void compileOpenAutoSellFilter(Hopper hopper) {
        for (GUIAutoSellFilter autoSellFilter : OPEN_INVENTORIES) {
            if (autoSellFilter.hopper == hopper) {
                autoSellFilter.compile();
            }
        }
    }
}
