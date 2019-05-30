package com.songoda.epichoppers.hopper.levels;

import com.songoda.epichoppers.EpicHoppers;
import com.songoda.epichoppers.hopper.levels.modules.Module;

import java.util.ArrayList;
import java.util.List;

public class Level {

    private final ArrayList<Module> registeredModules;
    private final List<String> description = new ArrayList<>();
    private int level, costExperience, costEconomy, range, amount, linkAmount, autoSell;
    private boolean filter, teleport;

    Level(int level, int costExperience, int costEconomy, int range, int amount, boolean filter, boolean teleport, int linkAmount, int autoSell, ArrayList<Module> registeredModules) {
        this.level = level;
        this.costExperience = costExperience;
        this.costEconomy = costEconomy;
        this.range = range;
        this.amount = amount;
        this.filter = filter;
        this.teleport = teleport;
        this.linkAmount = linkAmount;
        this.autoSell = autoSell;
        this.registeredModules = registeredModules;

        buildDescription();

    }

    public void buildDescription() {
        EpicHoppers instance = EpicHoppers.getInstance();

        description.clear();

        description.add(instance.getLocale().getMessage("interface.hopper.range", range));
        description.add(instance.getLocale().getMessage("interface.hopper.amount", amount));
        if (linkAmount != 1)
            description.add(instance.getLocale().getMessage("interface.hopper.linkamount", linkAmount));
        if (filter)
            description.add(instance.getLocale().getMessage("interface.hopper.filter", EpicHoppers.getInstance().getLocale().getMessage("general.word.enabled")));
        if (teleport)
            description.add(instance.getLocale().getMessage("interface.hopper.teleport", EpicHoppers.getInstance().getLocale().getMessage("general.word.enabled")));

        for (Module module : registeredModules) {
            description.add(module.getDescription());
        }
    }


    public int getLevel() {
        return level;
    }


    public int getRange() {
        return range;
    }


    public int getAmount() {
        return amount;
    }


    public boolean isFilter() {
        return filter;
    }


    public boolean isTeleport() {
        return teleport;
    }


    public int getAutoSell() {
        return autoSell;
    }


    public int getLinkAmount() {
        return linkAmount;
    }


    public int getCostExperience() {
        return costExperience;
    }


    public int getCostEconomy() {
        return costEconomy;
    }


    public List<String> getDescription() {
        return new ArrayList<>(description);
    }


    public ArrayList<Module> getRegisteredModules() {
        return new ArrayList<>(registeredModules);
    }


    public void addModule(Module module) {
        registeredModules.add(module);
        buildDescription();
    }

}

