package com.songoda.epichoppers.api.hopper.levels;

import com.songoda.epichoppers.api.hopper.levels.modules.Module;

import java.util.ArrayList;
import java.util.List;

public interface Level {

    /**
     * Get the current level in numerical format.
     *
     * @return level
     */
    int getLevel();

    /**
     * Get the range that this Level will allow
     * the applied hopper to remotely connect with
     * another hopper.
     *
     * @return range
     */
    int getRange();

    /**
     * Get the amount of items that will transfer
     * between this hopper and the remotely connected
     * hopper at a single time.
     *
     * @return amount
     */
    int getAmount();

    /**
     * Whether or not the filter is enabled with this
     * level.
     *
     * @return true if the filter is enabled, false
     * otherwise
     */
    boolean isFilter();

    /**
     * Whether or not teleporting through hoppers is
     * enabled with this level.
     *
     * @return true if teleporting is enabled false
     * otherwise
     */
    boolean isTeleport();


    /**
     * Get the cost in experience in order to upgrade
     * to this level.
     *
     * @return experience upgrade cost
     */
    int getCostExperience();

    /**
     * Get the cost in economy in order to upgrade
     * to this level.
     *
     * @return economy upgrade cost
     */
    int getCostEconomy();

    List<String> getDescription();

    ArrayList<Module> getRegisteredModules();

    void addModule(Module module);
}
