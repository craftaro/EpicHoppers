package com.songoda.epichoppers.hopper.levels.modules;

import com.songoda.epichoppers.EpicHoppers;
import com.songoda.epichoppers.hopper.Hopper;
import com.songoda.epichoppers.tasks.HopTask;
import com.songoda.epichoppers.utils.Methods;
import com.songoda.epichoppers.utils.ServerVersion;
import com.songoda.epichoppers.utils.settings.Setting;
import net.brcdev.shopgui.ShopGuiPlusApi;
import net.brcdev.shopgui.api.exception.PlayerDataNotLoadedException;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ModuleAutoSell implements Module {

    private int timeOut;
    private int hopperTickRate;

    public ModuleAutoSell(int timeOut) {
        EpicHoppers instance = EpicHoppers.getInstance();
        this.timeOut = timeOut * 20;
        this.hopperTickRate = (int) instance.getConfig().getLong("Main.Amount of Ticks Between Hops");
    }


    public String getName() {
        return "AutoSell";
    }


    public void run(Hopper hopper, Inventory hopperInventory) {
        if (hopperInventory == null) return;

        if (hopper.getAutoSellTimer() == -9999) return;

        if (hopper.getAutoSellTimer() <= 0) {
            EpicHoppers instance = EpicHoppers.getInstance();

            if (instance.getEconomy() == null) return;

            boolean updateComparators = false;

            List<String> list = instance.getConfig().getStringList("Main.AutoSell Prices");

            OfflinePlayer player = Bukkit.getOfflinePlayer(hopper.getPlacedBy());

            for (ItemStack itemStack : hopperInventory.getContents()) {
                if (itemStack == null) continue;

                double value;
                if (Setting.AUTOSELL_SHOPGUIPLUS.getBoolean() && player.isOnline()) {
                    try {
                        value = ShopGuiPlusApi.getItemStackPriceSell(player.getPlayer(), itemStack);
                    } catch (PlayerDataNotLoadedException e){
                        value = 0;
                    }
                } else
                    value = list.stream().filter(line -> Material.valueOf(line.split(",")[0])
                            == itemStack.getType()).findFirst().map(s -> Double.valueOf(s.split(",")[1])).orElse(0.0);

                if (value == 0) continue;

                    instance.getEconomy().deposit(player, value * itemStack.getAmount());
                hopperInventory.removeItem(itemStack);

                updateComparators = true;
            }
            hopper.setAutoSellTimer(timeOut);

            if (updateComparators)
                HopTask.updateAdjacentComparators(hopper.getLocation());
        }
        hopper.setAutoSellTimer(hopper.getAutoSellTimer() - hopperTickRate);
    }


    public ItemStack getGUIButton(Hopper hopper) {
        Hopper eHopper = hopper;
        ItemStack sell = new ItemStack(EpicHoppers.getInstance().isServerVersionAtLeast(ServerVersion.V1_13) ? Material.SUNFLOWER : Material.valueOf("DOUBLE_PLANT"), 1);
        ItemMeta sellmeta = sell.getItemMeta();
        sellmeta.setDisplayName(EpicHoppers.getInstance().getLocale().getMessage("interface.hopper.selltitle"));
        ArrayList<String> loresell = new ArrayList<>();
        String[] parts = EpicHoppers.getInstance().getLocale().getMessage("interface.hopper.selllore", eHopper.getAutoSellTimer() == -9999 ? "\u221E" : (int) Math.floor(eHopper.getAutoSellTimer() / 20)).split("\\|");
        for (String line : parts) {
            loresell.add(Methods.formatText(line));
        }
        sellmeta.setLore(loresell);
        sell.setItemMeta(sellmeta);
        return sell;
    }


    public void runButtonPress(Player player, Hopper hopper) {
        Hopper eHopper = hopper;
        if (eHopper.getAutoSellTimer() == -9999) {
            eHopper.setAutoSellTimer(0);
        } else {
            eHopper.setAutoSellTimer(-9999);
        }
    }


    public List<Material> getBlockedItems(Hopper hopper) {
        return null;
    }


    public String getDescription() {
        return EpicHoppers.getInstance().getLocale().getMessage("interface.hopper.autosell", (int) Math.floor(timeOut / 20));
    }
}
