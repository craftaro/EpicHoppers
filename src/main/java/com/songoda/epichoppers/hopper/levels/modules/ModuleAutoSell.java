package com.songoda.epichoppers.hopper.levels.modules;

import com.songoda.epichoppers.EpicHoppers;
import com.songoda.epichoppers.hopper.Hopper;
import com.songoda.epichoppers.tasks.HopTask;
import com.songoda.epichoppers.utils.Methods;
import com.songoda.epichoppers.utils.ServerVersion;
import com.songoda.epichoppers.utils.settings.Setting;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class ModuleAutoSell extends Module {

    private int timeOut;
    private int hopperTickRate;

    public ModuleAutoSell(EpicHoppers plugin, int timeOut) {
        super(plugin);
        EpicHoppers instance = EpicHoppers.getInstance();
        this.timeOut = timeOut * 20;
        this.hopperTickRate = (int) instance.getConfig().getLong("Main.Amount of Ticks Between Hops");
    }

    @Override
    public String getName() {
        return "AutoSell";
    }


    @Override
    public void run(Hopper hopper, Inventory hopperInventory) {
        if (hopperInventory == null) return;

        int currentTime = getTime(hopper);

        if (currentTime == -9999) return;

        if (currentTime <= 0) {
            int amountSold = 0;
            double totalValue = 0;

            if (plugin.getEconomy() == null) return;

            boolean updateComparators = false;

            List<String> list = plugin.getConfig().getStringList("Main.AutoSell Prices");

            OfflinePlayer player = Bukkit.getOfflinePlayer(hopper.getPlacedBy());

            for (ItemStack itemStack : hopperInventory.getContents()) {
                if (itemStack == null) continue;

                double value;
                if (Setting.AUTOSELL_SHOPGUIPLUS.getBoolean() && player.isOnline()) {
                    try {
                        ItemStack clone = itemStack.clone();
                        clone.setAmount(1);
                        value = net.brcdev.shopgui.ShopGuiPlusApi.getItemStackPriceSell(player.getPlayer(), clone);
                    } catch (Exception e) {
                        value = 0;
                    }
                } else
                    value = list.stream().filter(line -> Material.valueOf(line.split(",")[0])
                            == itemStack.getType()).findFirst().map(s -> Double.valueOf(s.split(",")[1])).orElse(0.0);

                if (value == 0) continue;

                double sellingFor = value * itemStack.getAmount();

                plugin.getEconomy().deposit(player, sellingFor);
                totalValue += sellingFor;
                amountSold += itemStack.getAmount();
                hopperInventory.removeItem(itemStack);

                updateComparators = true;
            }
            modifyDataCache(hopper, "time", timeOut);

            if (updateComparators)
                HopTask.updateAdjacentComparators(hopper.getLocation());

            if (totalValue != 0 && player.isOnline()) {
                player.getPlayer().sendMessage(plugin.references.getPrefix() + plugin.getLocale().getMessage("event.hopper.autosell", amountSold, Methods.formatEconomy(totalValue)));
            }
        }
        modifyDataCache(hopper, "time", getTime(hopper) - hopperTickRate);
    }

    @Override
    public ItemStack getGUIButton(Hopper hopper) {
        ItemStack sell = new ItemStack(EpicHoppers.getInstance().isServerVersionAtLeast(ServerVersion.V1_13) ? Material.SUNFLOWER : Material.valueOf("DOUBLE_PLANT"), 1);
        ItemMeta sellmeta = sell.getItemMeta();
        sellmeta.setDisplayName(EpicHoppers.getInstance().getLocale().getMessage("interface.hopper.selltitle"));
        ArrayList<String> loreSell = new ArrayList<>();
        String[] parts = EpicHoppers.getInstance().getLocale().getMessage("interface.hopper.selllore", getTime(hopper) == -9999 ? "\u221E" : (int) Math.floor(getTime(hopper) / 20), isNotifying(hopper)).split("\\|");
        for (String line : parts) {
            loreSell.add(Methods.formatText(line));
        }
        sellmeta.setLore(loreSell);
        sell.setItemMeta(sellmeta);
        return sell;
    }

    @Override
    public void runButtonPress(Player player, Hopper hopper, ClickType type) {
        if (type == ClickType.LEFT) {
            if (getTime(hopper) == -9999) {
                saveData(hopper, "time", timeOut);
            } else {
                saveData(hopper, "time", -9999);
            }
        } else if (type == ClickType.RIGHT) {
            saveData(hopper,"notifications", !isNotifying(hopper));
        }
    }

    @Override
    public List<Material> getBlockedItems(Hopper hopper) {
        return null;
    }

    @Override
    public String getDescription() {
        return EpicHoppers.getInstance().getLocale().getMessage("interface.hopper.autosell", (int) Math.floor(timeOut / 20));
    }

    private boolean isNotifying(Hopper hopper) {
        Object notifications = getData(hopper, "notifications");
        if (notifications == null) return false;
        return (boolean) notifications;
    }

    private int getTime(Hopper hopper) {
        Object time = getData(hopper, "time");
        if (time == null) return -9999;
        return (int) time;
    }
}
