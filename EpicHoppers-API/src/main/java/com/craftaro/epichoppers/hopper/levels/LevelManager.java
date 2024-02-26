package com.craftaro.epichoppers.hopper.levels;

import com.craftaro.epichoppers.hopper.levels.modules.Module;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Map;

public interface LevelManager {
    void addLevel(int level, int costExperience, int costEconomy, int range, int amount, boolean filter, boolean teleport, int linkAmount, ArrayList<Module> modules);

    Level getLevel(int level);

    Level getLevel(ItemStack item);

    boolean isEpicHopper(ItemStack item);

    Level getLowestLevel();

    Level getHighestLevel();

    boolean isLevel(int level);

    Map<Integer, Level> getLevels();

    void clear();
}
