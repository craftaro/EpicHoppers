package com.songoda.epichoppers.hopper.levels.modules;

import com.songoda.epichoppers.EpicHoppers;
import com.songoda.epichoppers.hopper.Hopper;
import com.songoda.epichoppers.utils.Methods;
import com.songoda.epichoppers.utils.ServerVersion;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

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

            List<String> list = instance.getConfig().getStringList("Main.AutoSell Prices");

            for (String line : list) {
                try {
                    String[] split = line.split(",");

                    Material material = Material.valueOf(split[0]);
                    double price = Double.valueOf(split[1]);

                    for (ItemStack itemStack : hopperInventory.getContents()) {
                        if (itemStack == null || itemStack.getType() != material) continue;

                        instance.getEconomy().deposit(Bukkit.getOfflinePlayer(hopper.getPlacedBy()), price * itemStack.getAmount());
                        hopperInventory.removeItem(itemStack);
                    }
                } catch (Exception ignored) {
                }
            }
            hopper.setAutoSellTimer(timeOut);
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
