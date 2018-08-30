package com.songoda.epichoppers.api;

import com.songoda.epichoppers.api.hopper.HopperManager;
import com.songoda.epichoppers.api.hopper.levels.Level;
import com.songoda.epichoppers.api.hopper.levels.LevelManager;
import com.songoda.epichoppers.api.utils.ProtectionPluginHook;
import org.bukkit.inventory.ItemStack;

import java.util.function.Supplier;

/**
 * The main API class for the EpicHoppers plugin. This class will provide various
 * methods to access important features of the plugin's API. For static method
 * wrappers to all methods in this interface, see the {@link EpicHoppersAPI} class
 */
public interface EpicHoppers {

    Level getLevelFromItem(ItemStack item);

    ItemStack newHopperItem(Level level);

    /**
     * Get an instance of the {@link LevelManager}
     *
     * @return the level manager
     */
    LevelManager getLevelManager();


    /**
     * Get an instance of the {@link HopperManager}
     *
     * @return the hopper manager
     */
    HopperManager getHopperManager();

    void register(Supplier<ProtectionPluginHook> hookSupplier);

    /**
     * Register a new {@link ProtectionPluginHook} implementation
     * in order for EpicSpawners to support plugins that protect
     * blocks from being interacted with
     *
     * @param hook the hook to register
     */
    void registerProtectionHook(ProtectionPluginHook hook);
}
