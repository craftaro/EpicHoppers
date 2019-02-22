package com.songoda.epichoppers.gui;

import com.songoda.epichoppers.EpicHoppersPlugin;
import com.songoda.epichoppers.api.hopper.Filter;
import com.songoda.epichoppers.hopper.EHopper;
import com.songoda.epichoppers.player.SyncType;
import com.songoda.epichoppers.utils.Debugger;
import com.songoda.epichoppers.utils.Methods;
import com.songoda.epichoppers.utils.gui.AbstractGUI;
import com.songoda.epichoppers.utils.gui.Range;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class GUIFilter extends AbstractGUI {

    private final EpicHoppersPlugin plugin;
    private final EHopper hopper;

    public GUIFilter(EpicHoppersPlugin plugin, EHopper hopper, Player player) {
        super(player);
        this.plugin = plugin;
        this.hopper = hopper;

        init(Methods.formatText(Methods.formatName(hopper.getLevel().getLevel(), false) + " &8-&f Filter"), 54);
    }

    @Override
    protected void constructGUI() {

        Filter filter = hopper.getFilter();

        inventory.setItem(6, Methods.getBackgroundGlass(true));
        inventory.setItem(7, Methods.getBackgroundGlass(true));
        inventory.setItem(8, Methods.getBackgroundGlass(true));
        inventory.setItem(15, Methods.getBackgroundGlass(true));
        inventory.setItem(17, Methods.getBackgroundGlass(true));
        inventory.setItem(24, Methods.getBackgroundGlass(true));
        inventory.setItem(25, Methods.getGlass());
        inventory.setItem(26, Methods.getBackgroundGlass(true));
        inventory.setItem(33, Methods.getBackgroundGlass(true));
        inventory.setItem(34, Methods.getGlass());
        inventory.setItem(35, Methods.getBackgroundGlass(true));
        inventory.setItem(42, Methods.getBackgroundGlass(true));
        inventory.setItem(44, Methods.getBackgroundGlass(true));
        inventory.setItem(51, Methods.getBackgroundGlass(true));
        inventory.setItem(52, Methods.getBackgroundGlass(true));
        inventory.setItem(53, Methods.getBackgroundGlass(true));

        ItemStack it = new ItemStack(Material.WHITE_STAINED_GLASS_PANE, 1);
        ItemMeta itm = it.getItemMeta();
        itm.setDisplayName(plugin.getLocale().getMessage("interface.filter.whitelist"));
        it.setItemMeta(itm);

        int[] whiteSlots = {0, 1, 45, 46};
        for (int nu : whiteSlots) {
            inventory.setItem(nu, it);
        }

        int[] awhite = {9, 10, 18, 19, 27, 28, 36, 37};
        int num = 0;
        for (ItemStack m : filter.getWhiteList()) {
            if (num >= filter.getWhiteList().size()) break;
            inventory.setItem(awhite[num], new ItemStack(m));
            num++;
        }

        it = new ItemStack(Material.BLACK_STAINED_GLASS_PANE, 1);
        itm = it.getItemMeta();
        itm.setDisplayName(plugin.getLocale().getMessage("interface.filter.blacklist"));
        it.setItemMeta(itm);

        int[] blackSlots = {2, 3, 47, 48};
        for (int nu : blackSlots) {
            inventory.setItem(nu, it);
        }

        int[] ablack = {11, 12, 20, 21, 29, 30, 38, 39};
        num = 0;
        for (ItemStack m : filter.getBlackList()) {
            if (num >= filter.getBlackList().size()) break;
            inventory.setItem(ablack[num], new ItemStack(m));
            num++;
        }

        it = new ItemStack(Material.BARRIER);
        itm = it.getItemMeta();
        itm.setDisplayName(plugin.getLocale().getMessage("interface.filter.void"));
        it.setItemMeta(itm);

        int[] avoid = {4, 5, 49, 50};
        for (int nu : avoid) {
            inventory.setItem(nu, it);
        }

        int[] voidSlots = {13, 14, 22, 23, 31, 32, 40, 41};
        num = 0;
        for (ItemStack m : filter.getVoidList()) {
            if (num >= filter.getVoidList().size()) break;
            inventory.setItem(voidSlots[num], new ItemStack(m));
            num++;
        }

        ItemStack itemInfo = new ItemStack(Material.PAPER, 1);
        ItemMeta itemmetaInfo = itemInfo.getItemMeta();
        itemmetaInfo.setDisplayName(plugin.getLocale().getMessage("interface.filter.infotitle"));
        ArrayList<String> loreInfo = new ArrayList<>();
        String[] parts = plugin.getLocale().getMessage("interface.filter.infolore").split("\\|");
        for (String line : parts) {
            loreInfo.add(Methods.formatText(line));
        }
        itemmetaInfo.setLore(loreInfo);
        itemInfo.setItemMeta(itemmetaInfo);

        inventory.setItem(16, itemInfo);


        ItemStack hook = new ItemStack(Material.TRIPWIRE_HOOK, 1);
        ItemMeta hookmeta = hook.getItemMeta();
        hookmeta.setDisplayName(plugin.getLocale().getMessage("interface.hopper.rejectsync"));
        ArrayList<String> lorehook = new ArrayList<>();
        parts = plugin.getLocale().getMessage("interface.hopper.synclore", filter.getEndPoint() != null ? 1 : 0).split("\\|");
        for (String line : parts) {
            lorehook.add(Methods.formatText(line));
        }
        hookmeta.setLore(lorehook);
        hook.setItemMeta(hookmeta);
        inventory.setItem(43, hook);

        addDraggable(new Range(9, 14, null, false), true);
        addDraggable(new Range(18, 23, null, false), true);
        addDraggable(new Range(27, 32, null, false), true);
        addDraggable(new Range(36, 41, null, false), true);
    }

    @Override
    protected void registerClickables() {
        registerClickable(43, ((player, inventory, cursor, slot, type) -> {
            if (type == ClickType.RIGHT) {
                player.sendMessage(plugin.references.getPrefix() + plugin.getLocale().getMessage("event.hopper.desync"));
                hopper.getFilter().setEndPoint(null);
            } else {
                plugin.getPlayerDataManager().getPlayerData(player).setSyncType(SyncType.FILTERED);
                player.sendMessage(plugin.references.getPrefix() + plugin.getLocale().getMessage("event.hopper.syncnext"));
                hopper.timeout(player);
            }
            player.closeInventory();
        }));
    }


    private void compile(Player p) {
        try {
            ItemStack[] items = p.getOpenInventory().getTopInventory().getContents();

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
                            Bukkit.getPlayer(hopper.getLastPlayer()).getInventory().addItem(item);
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
                            Bukkit.getPlayer(hopper.getLastPlayer()).getInventory().addItem(item);
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
                            Bukkit.getPlayer(hopper.getLastPlayer()).getInventory().addItem(item);
                            item.setAmount(1);
                        }
                        ovoid.add(item);
                    }
                }
            }
            filter.setWhiteList(owhite);
            filter.setBlackList(oblack);
            filter.setVoidList(ovoid);
        } catch (Exception e) {
            Debugger.runReport(e);
        }
    }

    @Override
    protected void registerOnCloses() {
        registerOnClose(((player, inventory) -> compile(player)));
    }
}
