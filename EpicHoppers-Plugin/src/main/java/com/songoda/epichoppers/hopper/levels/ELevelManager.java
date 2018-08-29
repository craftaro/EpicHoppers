package com.songoda.epichoppers.hopper.levels;

import com.songoda.epichoppers.api.hopper.levels.Level;
import com.songoda.epichoppers.api.hopper.levels.LevelManager;
import com.songoda.epichoppers.api.hopper.levels.modules.Module;

import java.util.*;

public class ELevelManager implements LevelManager {

    private final NavigableMap<Integer, ELevel> registeredLevels = new TreeMap<>();

    @Override
    public void addLevel(int level, int costExperience, int costEconomy, int range, int amount, boolean filter, boolean teleport, ArrayList<Module> modules) {
        registeredLevels.put(level, new ELevel(level, costExperience, costEconomy, range, amount, filter, teleport, modules));
    }

    @Override
    public Level getLevel(int level) {
        return registeredLevels.get(level);
    }

    @Override
    public Level getLowestLevel() {
        return registeredLevels.firstEntry().getValue();
    }

    @Override
    public Level getHighestLevel() {
        return registeredLevels.lastEntry().getValue();
    }

    @Override
    public boolean isLevel(int level) {
        return registeredLevels.containsKey(level);
    }

    @Override
    public Map<Integer, Level> getLevels() {
        return Collections.unmodifiableMap(registeredLevels);
    }

    public void clear() {
        registeredLevels.clear();
    }
}
