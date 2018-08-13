package com.songoda.epichoppers.hopper;

import com.songoda.epichoppers.api.hopper.LevelManager;

import java.util.Collections;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

public class ELevelManager implements LevelManager {

    private final NavigableMap<Integer, ELevel> registeredLevels = new TreeMap<>();

    @Override
    public void addLevel(int level, int costExperiance, int costEconomy, int range, int amount, int suction, int blockBreak) {
        registeredLevels.put(level, new ELevel(level, costExperiance, costEconomy, range, amount, suction, blockBreak));
    }

    @Override
    public com.songoda.epichoppers.api.hopper.Level getLevel(int level) {
        return registeredLevels.get(level);
    }

    @Override
    public com.songoda.epichoppers.api.hopper.Level getLowestLevel() {
        return registeredLevels.firstEntry().getValue();
    }

    @Override
    public com.songoda.epichoppers.api.hopper.Level getHighestLevel() {
        return registeredLevels.lastEntry().getValue();
    }

    @Override
    public boolean isLevel(int level) {
        return registeredLevels.containsKey(level);
    }

    @Override
    public Map<Integer, com.songoda.epichoppers.api.hopper.Level> getLevels() {
        return Collections.unmodifiableMap(registeredLevels);
    }

    public void clear() {
        registeredLevels.clear();
    }
}
