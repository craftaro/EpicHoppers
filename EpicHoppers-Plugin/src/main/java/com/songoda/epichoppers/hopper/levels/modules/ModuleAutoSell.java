package com.songoda.epichoppers.hopper.levels.modules;

import com.songoda.epichoppers.EpicHoppersPlugin;
import com.songoda.epichoppers.api.EpicHoppers;
import com.songoda.epichoppers.api.hopper.Hopper;
import com.songoda.epichoppers.api.hopper.levels.modules.Module;
import com.songoda.epichoppers.hopper.EHopper;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ModuleAutoSell implements Module {

    private int timeOut;
    private int hopperTickRate;

    public ModuleAutoSell(int timeOut) {
        EpicHoppersPlugin instance = EpicHoppersPlugin.getInstance();
        this.timeOut = timeOut * 20;
        this.hopperTickRate = (int)instance.getConfig().getLong("Main.Amount of Ticks Between Hops");
    }

    @Override
    public String getName() {
        return "AutoSell";
    }

    @Override
    public void run(Hopper hopper) {
        org.bukkit.block.Hopper hopperBlock = hopper.getHopper();
        if (hopperBlock == null || hopperBlock.getInventory() == null) return;

        if (((EHopper)hopper).getAutoSellTimer() == -9999) return;

        if (((EHopper)hopper).getAutoSellTimer() <= 0) {
            EpicHoppersPlugin instance = EpicHoppersPlugin.getInstance();

            if (instance.getServer().getPluginManager().getPlugin("Vault") == null) return;
            RegisteredServiceProvider<Economy> rsp = instance.getServer().getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class);
            net.milkbowl.vault.economy.Economy econ = rsp.getProvider();

            List<String> list = (List<String>)EpicHoppersPlugin.getInstance().getConfig().getList("Main.AutoSell Prices");

            for (String line : list) {
                try {
                    String[] split = line.split(",");

                    Material material = Material.valueOf(split[0]);
                    double price = Double.valueOf(split[1]);

                    for (ItemStack itemStack : hopperBlock.getInventory().getContents()) {
                        if (itemStack.getType() == material) {
                            econ.depositPlayer(Bukkit.getOfflinePlayer(hopper.getPlacedBy()), price * itemStack.getAmount());
                        }
                        hopperBlock.getInventory().removeItem(itemStack);
                    }
                } catch (Exception ignored) {}
            }
            ((EHopper)hopper).setAutoSellTimer(timeOut);
        }
        ((EHopper)hopper).setAutoSellTimer(((EHopper)hopper).getAutoSellTimer() - hopperTickRate);
    }

    @Override
    public List<Material> getBlockedItems(Hopper hopper) {
        return null;
    }

    @Override
    public String getDescription() {
        return EpicHoppersPlugin.getInstance().getLocale().getMessage("interface.hopper.autosell", timeOut);
    }
}
