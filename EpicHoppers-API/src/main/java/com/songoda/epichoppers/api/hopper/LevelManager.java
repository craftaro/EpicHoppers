package com.songoda.epichoppers.api.hopper;

import java.util.Map;

public interface LevelManager {
    void addLevel(int level, int costExperiance, int costEconomy, int range, int amount, int suction, int blockBreak);

    Level getLevel(int level);

    Level getLowestLevel();

    Level getHighestLevel();

    Map<Integer, Level> getLevels();
}
