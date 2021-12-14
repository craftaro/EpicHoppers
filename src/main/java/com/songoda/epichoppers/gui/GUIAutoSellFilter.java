package com.songoda.epichoppers.gui;

import com.songoda.core.compatibility.CompatibleMaterial;
import com.songoda.core.gui.CustomizableGui;
import com.songoda.core.gui.GuiUtils;
import com.songoda.core.utils.TextUtils;
import com.songoda.epichoppers.EpicHoppers;
import com.songoda.epichoppers.hopper.Filter;
import com.songoda.epichoppers.hopper.Hopper;
import com.songoda.epichoppers.hopper.ItemType;
import com.songoda.epichoppers.settings.Settings;
import com.songoda.epichoppers.utils.Methods;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class GUIAutoSellFilter extends CustomizableGui {

    private final EpicHoppers plugin;
    private final Hopper hopper;

    private final int[] whiteListSlots = {9, 10, 11, 18, 19, 20, 27, 28, 29, 36, 37, 38};
    private final int[] blackListSlots = {12, 13, 14, 21, 22, 23, 30, 31, 32, 39, 40, 41};

    public GUIAutoSellFilter(EpicHoppers plugin, Hopper hopper) {
        super(plugin, "autosell");
        this.plugin = plugin;
        this.hopper = hopper;

        setRows(6);
        setTitle(TextUtils.formatText(Methods.formatName(hopper.getLevel().getLevel()) + " &8-&f AutoSell Filter"));
        setDefaultItem(null);
        setAcceptsItems(true);

        setOnClose((event) -> {
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

        setButton("back", 8, GuiUtils.createButtonItem(CompatibleMaterial.ARROW.getItem(),
                        plugin.getLocale().getMessage("general.nametag.back").getMessage()),
                (event) -> {
                    hopper.overview(guiManager, event.player);
                    compile();
                });

        // Whitelist
        ItemStack indicatorItem = CompatibleMaterial.WHITE_STAINED_GLASS_PANE.getItem();
        ItemMeta indicatorMeta = indicatorItem.getItemMeta();
        indicatorMeta.setDisplayName(plugin.getLocale().getMessage("interface.autosell-filter.whitelist").getMessage());
        indicatorItem.setItemMeta(indicatorMeta);

        int[] whiteSlots = {0, 1, 2, 45, 46, 47};
        for (int nu : whiteSlots) {
            setItem("whitelist", nu, indicatorItem);
        }

        int num = 0;
        for (ItemStack m : filter.getAutoSellWhiteList()) {
            if (num >= filter.getAutoSellWhiteList().size()) break;
            setItem(whiteListSlots[num], new ItemStack(m));
            num++;
        }

        // Blacklist
        indicatorItem = CompatibleMaterial.BLACK_STAINED_GLASS_PANE.getItem();
        indicatorMeta = indicatorItem.getItemMeta();
        indicatorMeta.setDisplayName(plugin.getLocale().getMessage("interface.autosell-filter.blacklist").getMessage());
        indicatorItem.setItemMeta(indicatorMeta);

        int[] blackSlots = {3, 4, 5, 48, 49, 50};
        for (int nu : blackSlots) {
            setItem("blacklist", nu, indicatorItem);
        }

        num = 0;
        for (ItemStack m : filter.getAutoSellBlackList()) {
            if (num >= filter.getAutoSellBlackList().size()) break;
            setItem(blackListSlots[num], new ItemStack(m));
            num++;
        }

        // Info item
        indicatorItem = new ItemStack(CompatibleMaterial.PAPER.getMaterial());
        indicatorMeta = indicatorItem.getItemMeta();

        indicatorMeta.setDisplayName(plugin.getLocale().getMessage("interface.autosell-filter.infotitle").getMessage());
        ArrayList<String> loreInfo = new ArrayList<>();
        String[] parts = plugin.getLocale().getMessage("interface.autosell-filter.infolore").getMessage().split("\\|");

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
        ItemStack[] items = inventory.getContents();

        Filter filter = hopper.getFilter();

        List<ItemStack> whiteListItems = new ArrayList<>();
        List<ItemStack> blackListItems = new ArrayList<>();

        for (int i = 0; i < items.length; i++) {
            for (int slot : whiteListSlots) {
                if (slot != i) continue;

                if (items[i] != null && !items[i].getType().isAir()) {
                    ItemStack item = items[i];
                    if (item.getAmount() != 1) {
                        item.setAmount(item.getAmount() - 1);
                        Bukkit.getPlayer(hopper.getLastPlayerOpened()).getInventory().addItem(item);
                        item.setAmount(1);
                    }

                    whiteListItems.add(item);
                }
            }

            for (int slot : blackListSlots) {
                if (slot != i) continue;

                if (items[i] != null && !items[i].getType().isAir()) {
                    ItemStack item = items[i];
                    if (item.getAmount() != 1) {
                        item.setAmount(item.getAmount() - 1);
                        Bukkit.getPlayer(hopper.getLastPlayerOpened()).getInventory().addItem(item);
                        item.setAmount(1);
                    }
                    blackListItems.add(item);
                }
            }
        }

        filter.setAutoSellWhiteList(whiteListItems);
        filter.setAutoSellBlackList(blackListItems);
        plugin.getDataManager().updateItems(hopper, ItemType.WHITELIST, whiteListItems);
        plugin.getDataManager().updateItems(hopper, ItemType.BLACKLIST, blackListItems);
    }
}
