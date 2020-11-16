package com.songoda.epichoppers.hopper.levels;

import com.songoda.epichoppers.EpicHoppers;
import com.songoda.epichoppers.hopper.levels.modules.Module;

import java.util.ArrayList;
import java.util.List;

public class Level {

    private final ArrayList<Module> registeredModules;
    private final List<String> description = new ArrayList<>();
    private final int level, costExperience, costEconomy, range, amount, linkAmount;
    private final boolean filter, teleport;

    public Level(int level, int costExperience, int costEconomy, int range, int amount, boolean filter, boolean teleport, int linkAmount, ArrayList<Module> registeredModules) {
        this.level = level;
        this.costExperience = costExperience;
        this.costEconomy = costEconomy;
        this.range = range;
        this.amount = amount;
        this.filter = filter;
        this.teleport = teleport;
        this.linkAmount = linkAmount;
        this.registeredModules = registeredModules;

        buildDescription();

    }

    public void buildDescription() {
        EpicHoppers instance = EpicHoppers.getInstance();

        description.clear();

        description.add(instance.getLocale().getMessage("interface.hopper.range")
                .processPlaceholder("range", range).getMessage());
        description.add(instance.getLocale().getMessage("interface.hopper.amount")
                .processPlaceholder("amount", amount).getMessage());
        if (linkAmount != 1)
            description.add(instance.getLocale().getMessage("interface.hopper.linkamount")
                    .processPlaceholder("amount", linkAmount).getMessage());
        if (filter)
            description.add(instance.getLocale().getMessage("interface.hopper.filter")
                    .processPlaceholder("enabled", EpicHoppers.getInstance().getLocale()
                            .getMessage("general.word.enabled").getMessage()).getMessage());
        if (teleport)
            description.add(instance.getLocale().getMessage("interface.hopper.teleport")
                    .processPlaceholder("enabled", EpicHoppers.getInstance()
                            .getLocale().getMessage("general.word.enabled").getMessage()).getMessage());

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


    public Module getModule(String name) {
        return registeredModules == null ? null :
                registeredModules.stream().filter(module -> module.getName().equals(name)).findFirst().orElse(null);
    }
}

