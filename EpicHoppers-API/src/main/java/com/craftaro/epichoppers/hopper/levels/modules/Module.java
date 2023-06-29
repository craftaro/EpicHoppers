package com.craftaro.epichoppers.hopper.levels.modules;

import com.craftaro.core.SongodaPlugin;
import com.craftaro.core.configuration.Config;
import com.craftaro.core.gui.GuiManager;
import com.craftaro.epichoppers.utils.StorageContainerCache;
import com.craftaro.epichoppers.hopper.Hopper;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class Module {
    private static final Map<String, Config> CONFIGS = new HashMap<>();

    protected final SongodaPlugin plugin;
    protected final GuiManager guiManager;
    private final Config config;

    public Module(SongodaPlugin plugin, GuiManager guiManager) {
        this.plugin = plugin;
        this.guiManager = guiManager;

        if (!CONFIGS.containsKey(getName())) {
            Config config = new Config(plugin, File.separator + "modules", getName() + ".yml");
            CONFIGS.put(getName(), config);
            config.load();

        }
        this.config = CONFIGS.get(getName());
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
        this.config.set("data." + serializeLocation(hopper.getLocation()) + "." + setting, value);
        modifyDataCache(hopper, setting, toCache);
    }

    public void modifyDataCache(Hopper hopper, String setting, Object value) {
        hopper.addDataToModuleCache(getName() + "." + setting, value);
    }

    protected Object getData(Hopper hopper, String setting) {
        String cacheStr = getName() + "." + setting;
        if (hopper.isDataCachedInModuleCache(cacheStr)) {
            return hopper.getDataFromModuleCache(cacheStr);
        }

        Object data = this.config.get("data." + serializeLocation(hopper.getLocation()) + "." + setting);
        modifyDataCache(hopper, setting, data);
        return data;
    }

    public void clearData(Hopper hopper) {
        this.config.set("data." + serializeLocation(hopper.getLocation()), null);
        hopper.clearModuleCache();
    }

    public void saveDataToFile() {
        this.config.save();
    }

    private static String serializeLocation(Location location) {
        if (location == null || location.getWorld() == null) {
            return "";
        }
        String w = location.getWorld().getName();
        double x = location.getX();
        double y = location.getY();
        double z = location.getZ();
        String str = w + ":" + x + ":" + y + ":" + z;
        str = str.replace(".0", "").replace(".", "/");
        return str;
    }
}
