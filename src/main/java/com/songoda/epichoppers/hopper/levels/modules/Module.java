package com.songoda.epichoppers.hopper.levels.modules;

import com.songoda.core.configuration.Config;
import com.songoda.epichoppers.EpicHoppers;
import com.songoda.epichoppers.hopper.Hopper;
import com.songoda.epichoppers.utils.Methods;
import com.songoda.epichoppers.utils.StorageContainerCache;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class Module {

    private static final Map<String, Config> configs = new HashMap<>();

    protected final EpicHoppers plugin;
    private final Config config;

    public Module(EpicHoppers plugin) {
        this.plugin = plugin;
        if (!configs.containsKey(getName())) {
            Config config = new Config(plugin, File.separator + "modules", getName() + ".yml");
            configs.put(getName(), config);
            config.load();

        }
        this.config = configs.get(getName());
    }

    public abstract String getName();

    public abstract void run(Hopper hopper, StorageContainerCache.Cache hopperCache);

    public abstract ItemStack getGUIButton(Hopper hopper);

    public abstract void runButtonPress(Player player, Hopper hopper, ClickType type);

    public abstract List<Material> getBlockedItems(Hopper hopper);

    public abstract String getDescription();

    public void saveData(Hopper hopper, String setting, Object value) {
        saveData(hopper, setting, value, value);
    }

    public void saveData(Hopper hopper, String setting, Object value, Object toCache) {
        config.set("data." + Methods.serializeLocation(hopper.getLocation()) + "." + setting, value);
        modifyDataCache(hopper, setting, toCache);
    }

    public void modifyDataCache(Hopper hopper, String setting, Object value) {
        hopper.addDataToModuleCache(getName() + "." + setting, value);
    }

    protected Object getData(Hopper hopper, String setting) {
        String cacheStr = getName() + "." + setting;
        if (hopper.isDataCachedInModuleCache(cacheStr))
            return hopper.getDataFromModuleCache(cacheStr);

        Object data = config.get("data." + Methods.serializeLocation(hopper.getLocation()) + "." + setting);
        modifyDataCache(hopper, setting, data);
        return data;
    }

    public void clearData(Hopper hopper) {
        config.set("data." + Methods.serializeLocation(hopper.getLocation()), null);
        hopper.clearModuleCache();
    }

    public void saveDataToFile() {
        config.save();
    }
}
