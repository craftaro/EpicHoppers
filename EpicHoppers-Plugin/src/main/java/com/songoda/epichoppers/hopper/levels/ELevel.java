package com.songoda.epichoppers.hopper.levels;

import com.songoda.epichoppers.EpicHoppersPlugin;
import com.songoda.epichoppers.api.hopper.levels.Level;
import com.songoda.epichoppers.api.hopper.levels.modules.Module;

import java.util.ArrayList;
import java.util.List;

public class ELevel implements Level {

    private final ArrayList<Module> registeredModules;

    private int level, costExperience, costEconomy, range, amount;

    private boolean filter, teleport;

    private final List<String> description = new ArrayList<>();

    ELevel(int level, int costExperience, int costEconomy, int range, int amount, boolean filter, boolean teleport, ArrayList<Module> registeredModules) {
        this.level = level;
        this.costExperience = costExperience;
        this.costEconomy = costEconomy;
        this.range = range;
        this.amount = amount;
        this.filter = filter;
        this.teleport = teleport;
        this.registeredModules = registeredModules;

        EpicHoppersPlugin instance = EpicHoppersPlugin.getInstance();

        description.add(instance.getLocale().getMessage("interface.hopper.range", range));
        description.add(instance.getLocale().getMessage("interface.hopper.amount", amount));
        if (filter) description.add(instance.getLocale().getMessage("interface.hopper.filter", true));
        if (teleport) description.add(instance.getLocale().getMessage("interface.hopper.teleport", true));

        for (Module module : registeredModules) {
            description.add(module.getDescription());
        }

    }

    @Override
    public int getLevel() {
        return level;
    }

    @Override
    public int getRange() {
        return range;
    }

    @Override
    public int getAmount() {
        return amount;
    }

    @Override
    public boolean isFilter() {
        return filter;
    }

    @Override
    public boolean isTeleport() {
        return teleport;
    }

    @Override
    public int getCostExperience() {
        return costExperience;
    }

    @Override
    public int getCostEconomy() {
        return costEconomy;
    }

    @Override
    public List<String> getDescription() {
        return new ArrayList<>(description);
    }

    @Override
    public ArrayList<Module> getRegisteredModules() {
        return new ArrayList<>(registeredModules);
    }

    @Override
    public void addModule(Module module) {
        registeredModules.add(module);
    }

}

