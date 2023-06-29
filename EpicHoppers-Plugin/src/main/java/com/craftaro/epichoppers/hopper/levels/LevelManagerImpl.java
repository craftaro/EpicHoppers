package com.craftaro.epichoppers.hopper.levels;

import com.craftaro.core.nms.NmsManager;
import com.craftaro.core.nms.nbt.NBTCore;
import com.craftaro.core.third_party.de.tr7zw.nbtapi.NBTItem;
import com.craftaro.epichoppers.hopper.levels.modules.Module;
import org.bukkit.ChatColor;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

public class LevelManagerImpl implements LevelManager {
    private final NavigableMap<Integer, Level> registeredLevels = new TreeMap<>();

    @Override
    public void addLevel(int level, int costExperience, int costEconomy, int range, int amount, boolean filter, boolean teleport, int linkAmount, ArrayList<Module> modules) {
        this.registeredLevels.put(level, new Level(level, costExperience, costEconomy, range, amount, filter, teleport, linkAmount, modules));
    }

    @Override
    public Level getLevel(int level) {
        return this.registeredLevels.get(level);
    }

    @Override
    public Level getLevel(ItemStack item) {
        NBTItem nbtItem = new NBTItem(item);

        if (nbtItem.hasTag("level")) {
            return getLevel(nbtItem.getInteger("level"));
        }

        // Legacy trash.
        if (item.hasItemMeta() && item.getItemMeta().getDisplayName().contains(":")) {
            String[] arr = item.getItemMeta().getDisplayName().replace(String.valueOf(ChatColor.COLOR_CHAR), "").split(":");
            return getLevel(Integer.parseInt(arr[0]));
        }

        return getLowestLevel();
    }

    @Override
    public boolean isEpicHopper(ItemStack item) {
        NBTCore nbt = NmsManager.getNbt();

        if (nbt.of(item).has("level")) {
            return true;
        }

        return item.hasItemMeta()
                // Legacy Trash.
                && item.getItemMeta().getDisplayName().contains(":");
    }


    @Override
    public Level getLowestLevel() {
        return this.registeredLevels.firstEntry().getValue();
    }


    @Override
    public Level getHighestLevel() {
        return this.registeredLevels.lastEntry().getValue();
    }


    @Override
    public boolean isLevel(int level) {
        return this.registeredLevels.containsKey(level);
    }


    @Override
    public Map<Integer, Level> getLevels() {
        return Collections.unmodifiableMap(this.registeredLevels);
    }

    @Override
    public void clear() {
        this.registeredLevels.clear();
    }
}
