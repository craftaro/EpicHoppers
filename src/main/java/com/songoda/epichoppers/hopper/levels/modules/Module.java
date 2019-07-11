package com.songoda.epichoppers.hopper.levels.modules;

import com.songoda.epichoppers.EpicHoppers;
import com.songoda.epichoppers.hopper.Hopper;
import com.songoda.epichoppers.utils.ConfigWrapper;
import com.songoda.epichoppers.utils.Methods;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class Module {

    protected final EpicHoppers plugin;
    private ConfigWrapper config;

    public Module(EpicHoppers plugin) {
        this.plugin = plugin;
        this.config = new ConfigWrapper(plugin, File.separator + "modules", getName() + ".yml");
    }

    public abstract String getName();

    public abstract void run(Hopper hopper, Inventory hopperInventory);

    public abstract ItemStack getGUIButton(Hopper hopper);

    public abstract void runButtonPress(Player player, Hopper hopper);

    public abstract List<Material> getBlockedItems(Hopper hopper);

    public abstract String getDescription();

    public void saveData(Hopper hopper, String setting, Object value) {
        saveData(hopper, setting, value, value);
    }

    public void saveData(Hopper hopper, String setting, Object value, Object toCache) {
        config.getConfig().set("data." + Methods.serializeLocation(hopper.getLocation()) + "." + setting, value);
        modifyDataCache(hopper, setting, toCache);
    }

    public void modifyDataCache(Hopper hopper, String setting, Object value) {
        hopper.addDataToModuleCache(setting, value);
    }

    protected Object getData(Hopper hopper, String setting) {
        if (hopper.isDataCachedInModuleCache(setting))
            return hopper.getDataFromModuleCache(setting);

        Object data = config.getConfig().get("data." + Methods.serializeLocation(hopper.getLocation()) + "." + setting);
        modifyDataCache(hopper, setting, data);
        return data;
    }

    public void clearData(Hopper hopper) {
        config.getConfig().set("data." + Methods.serializeLocation(hopper.getLocation()), null);
        hopper.clearModuleCache();
    }

    public void saveDataToFile() {
        config.saveConfig();
    }
}
