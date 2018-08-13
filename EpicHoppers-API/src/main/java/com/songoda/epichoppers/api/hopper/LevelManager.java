package com.songoda.epichoppers.api.hopper;

import java.util.Map;

public interface LevelManager {

    /**
     * This will add a level to the level manager. These levels can be used when upgrading hoppers.
     *
     * @param level
     * @param costExperiance
     * @param costEconomy
     * @param range
     * @param amount
     * @param suction
     * @param blockBreak
     */
    void addLevel(int level, int costExperiance, int costEconomy, int range, int amount, int suction, int blockBreak);

    Level getLevel(int level);

    Level getLowestLevel();

    Level getHighestLevel();

    boolean isLevel(int level);

    Map<Integer, Level> getLevels();
}
