package com.songoda.epichoppers.api.hopper.levels;

import com.songoda.epichoppers.api.hopper.levels.modules.Module;

import java.util.ArrayList;
import java.util.Map;

public interface LevelManager {

    /**
     * This will add a level to the level manager. These levels can be
     * used when upgrading hoppers or giving hoppers with the built in
     * "give" command.
     *
     * @param level The level of the hopper
     * @param costExperience  The cost in experience to upgrade the hopper
     * @param costEconomy The cost in economy to upgrade the hopper
     * @param range The range in which this hopper will need to be in order to sync with another hopper
     * @param amount The amount of items this hopper will transfer at a single time
     * @param filter Whether or not access to the filter is allowed.
     * @param teleport Whether or not teleporting through hoppers is allowed.
     */
    void addLevel(int level, int costExperience, int costEconomy, int range, int amount, boolean filter, boolean teleport, ArrayList<Module> modules);

    /**
     * Get {@link Level} by corresponding integer value.
     *
     * @param level the integer level
     * @return level object
     */
    Level getLevel(int level);

    /**
     * Get the lowest {@link Level} in the LevelManager.
     *
     * @return lowest level
     */
    Level getLowestLevel();


    /**
     * Get the highest {@link Level} in the LevelManager.
     *
     * @return high level
     */
    Level getHighestLevel();

    /**
     * Whether or not this level exists.
     *
     * @param level the integer level
     * @return true if a level, false otherwise
     */
    boolean isLevel(int level);

    /**
     * Get a unmodifiable map of the current level registered
     * in the manager.
     *
     * @return map of levels.
     */
    Map<Integer, Level> getLevels();
}
