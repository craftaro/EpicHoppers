package com.songoda.epichoppers.api.hopper;

import java.util.Map;

public interface LevelManager {

    /**
     * This will add a level to the level manager. These levels can be
     * used when upgrading hoppers or giving hoppers with the built in
     * "give" command.
     *
     * @param level The level of the hopper
     * @param costExperiance  The cost in experience to upgrade the hopper
     * @param costEconomy The cost in economy to upgrade the hopper
     * @param range The range in which this hopper will need to be in order to sync with another hopper
     * @param amount The amount of items this hopper will transfer at a single time
     * @param suction The distance in which this hopper will suck items into it
     * @param blockBreak The tick frequency in which this hopper will break blocks placed directly above it.
     */
    void addLevel(int level, int costExperiance, int costEconomy, int range, int amount, int suction, int blockBreak);

    Level getLevel(int level);

    Level getLowestLevel();

    Level getHighestLevel();

    boolean isLevel(int level);

    Map<Integer, Level> getLevels();
}
