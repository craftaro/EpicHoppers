package com.songoda.epichoppers.gui;

import com.songoda.core.compatibility.CompatibleMaterial;
import com.songoda.core.gui.CustomizableGui;
import com.songoda.core.gui.GuiUtils;
import com.songoda.core.utils.TextUtils;
import com.songoda.epichoppers.EpicHoppers;
import com.songoda.epichoppers.hopper.Filter;
import com.songoda.epichoppers.hopper.Hopper;
import com.songoda.epichoppers.hopper.ItemType;
import com.songoda.epichoppers.player.SyncType;
import com.songoda.epichoppers.settings.Settings;
import com.songoda.epichoppers.utils.Methods;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class GUIFilter extends CustomizableGui {

    private final EpicHoppers plugin;

    private final Hopper hopper;

    public GUIFilter(EpicHoppers plugin, Hopper hopper, Player player) {
        super(plugin, "filter");
        this.plugin = plugin;
        this.hopper = hopper;

        setRows(6);
        setTitle(TextUtils.formatText(Methods.formatName(hopper.getLevel().getLevel()) + " &8-&f Filter"));
        setDefaultItem(null);
        setAcceptsItems(true);

        setOnClose((event) -> {
            hopper.setActivePlayer(null);
            compile();
        });

        Filter filter = hopper.getFilter();

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

        ItemStack it = CompatibleMaterial.WHITE_STAINED_GLASS_PANE.getItem();
        ItemMeta itm = it.getItemMeta();
        itm.setDisplayName(plugin.getLocale().getMessage("interface.filter.whitelist").getMessage());
        it.setItemMeta(itm);

        setButton("back", 8, GuiUtils.createButtonItem(CompatibleMaterial.ARROW.getItem(),
                plugin.getLocale().getMessage("general.nametag.back").getMessage()),
                (event) -> {
                    hopper.overview(guiManager, event.player);
                    compile();
                });

        int[] whiteSlots = {0, 1, 45, 46};
        for (int nu : whiteSlots) {
            setItem(nu, it);
        }

        int[] awhite = {9, 10, 18, 19, 27, 28, 36, 37};
        int num = 0;
        for (ItemStack m : filter.getWhiteList()) {
            if (num >= filter.getWhiteList().size()) break;
            setItem(awhite[num], new ItemStack(m));
            num++;
        }

        it = CompatibleMaterial.BLACK_STAINED_GLASS_PANE.getItem();
        itm = it.getItemMeta();
        itm.setDisplayName(plugin.getLocale().getMessage("interface.filter.blacklist").getMessage());
        it.setItemMeta(itm);

        int[] blackSlots = {2, 3, 47, 48};
        for (int nu : blackSlots) {
            setItem("blacklist", nu, it);
        }

        int[] ablack = {11, 12, 20, 21, 29, 30, 38, 39};
        num = 0;
        for (ItemStack m : filter.getBlackList()) {
            if (num >= filter.getBlackList().size()) break;
            setItem(ablack[num], new ItemStack(m));
            num++;
        }

        it = new ItemStack(CompatibleMaterial.BARRIER.getMaterial());
        itm = it.getItemMeta();
        itm.setDisplayName(plugin.getLocale().getMessage("interface.filter.void").getMessage());
        it.setItemMeta(itm);

        int[] avoid = {4, 5, 49, 50};
        for (int nu : avoid) {
            setItem("void", nu, it);
        }

        int[] voidSlots = {13, 14, 22, 23, 31, 32, 40, 41};
        num = 0;
        for (ItemStack m : filter.getVoidList()) {
            if (num >= filter.getVoidList().size()) break;
            setItem(voidSlots[num], new ItemStack(m));
            num++;
        }

        ItemStack itemInfo = new ItemStack(CompatibleMaterial.PAPER.getMaterial());
        ItemMeta itemmetaInfo = itemInfo.getItemMeta();
        itemmetaInfo.setDisplayName(plugin.getLocale().getMessage("interface.filter.infotitle").getMessage());
        ArrayList<String> loreInfo = new ArrayList<>();
        String[] parts = plugin.getLocale().getMessage("interface.filter.infolore").getMessage().split("\\|");
        for (String line : parts) {
            loreInfo.add(TextUtils.formatText(line));
        }
        itemmetaInfo.setLore(loreInfo);
        itemInfo.setItemMeta(itemmetaInfo);

        setItem("info", 16, itemInfo);


        ItemStack hook = new ItemStack(CompatibleMaterial.TRIPWIRE_HOOK.getMaterial());
        ItemMeta hookmeta = hook.getItemMeta();
        hookmeta.setDisplayName(plugin.getLocale().getMessage("interface.hopper.rejectsync").getMessage());
        ArrayList<String> lorehook = new ArrayList<>();
        parts = plugin.getLocale().getMessage("interface.hopper.synclore")
                .processPlaceholder("amount", filter.getEndPoint() != null ? 1 : 0)
                .getMessage().split("\\|");
        for (String line : parts) {
            lorehook.add(TextUtils.formatText(line));
        }
        hookmeta.setLore(lorehook);
        hook.setItemMeta(hookmeta);
        setButton("reject", 43, hook,
                (event) -> {
                    if (event.clickType == ClickType.RIGHT) {
                        plugin.getLocale().getMessage("event.hopper.desync").sendPrefixedMessage(player);
                        hopper.getFilter().setEndPoint(null);
                    } else {
                        plugin.getPlayerDataManager().getPlayerData(player).setSyncType(SyncType.FILTERED);
                        plugin.getLocale().getMessage("event.hopper.syncnext").sendPrefixedMessage(player);
                        hopper.timeout(player);
                    }
                    player.closeInventory();
                });

        setUnlockedRange(9, 14);
        setUnlockedRange(18, 23);
        setUnlockedRange(27, 32);
        setUnlockedRange(36, 41);
    }


    private void compile() {
        ItemStack[] items = inventory.getContents();

        Filter filter = hopper.getFilter();

        List<ItemStack> owhite = new ArrayList<>();
        List<ItemStack> oblack = new ArrayList<>();
        List<ItemStack> ovoid = new ArrayList<>();

        int[] awhite = {9, 10, 18, 19, 27, 28, 36, 37};
        int[] ablack = {11, 12, 20, 21, 29, 30, 38, 39};
        int[] avoid = {13, 14, 22, 23, 31, 32, 40, 41};

        for (int i = 0; i < items.length; i++) {
            for (int aa : awhite) {
                if (aa != i) continue;
                if (items[i] != null && items[i].getType() != Material.AIR) {
                    ItemStack item = items[i];
                    if (item.getAmount() != 1) {
                        item.setAmount(item.getAmount() - 1);
                        Bukkit.getPlayer(hopper.getLastPlayerOpened()).getInventory().addItem(item);
                        item.setAmount(1);
                    }
                    owhite.add(item);
                }
            }
            for (int aa : ablack) {
                if (aa != i) continue;
                if (items[i] != null && items[i].getType() != Material.AIR) {
                    ItemStack item = items[i];
                    if (item.getAmount() != 1) {
                        item.setAmount(item.getAmount() - 1);
                        Bukkit.getPlayer(hopper.getLastPlayerOpened()).getInventory().addItem(item);
                        item.setAmount(1);
                    }
                    oblack.add(item);
                }
            }
            for (int aa : avoid) {
                if (aa != i) continue;
                if (items[i] != null && items[i].getType() != Material.AIR) {
                    ItemStack item = items[i];
                    if (item.getAmount() != 1) {
                        item.setAmount(item.getAmount() - 1);
                        Bukkit.getPlayer(hopper.getLastPlayerOpened()).getInventory().addItem(item);
                        item.setAmount(1);
                    }
                    ovoid.add(item);
                }
            }
        }
        filter.setWhiteList(owhite);
        filter.setBlackList(oblack);
        filter.setVoidList(ovoid);
        plugin.getDataManager().updateItems(hopper, ItemType.WHITELIST, owhite);
        plugin.getDataManager().updateItems(hopper, ItemType.BLACKLIST, oblack);
        plugin.getDataManager().updateItems(hopper, ItemType.VOID, ovoid);
    }
}
