package com.songoda.epichoppers.hopper;

import com.songoda.epichoppers.EpicHoppers;

import java.util.ArrayList;
import java.util.List;

public class Level {

    private int level, costExperience, costEconomy, range, amount, blockBreak, suction;

    private List<String> description = new ArrayList<>();

    public Level(int level, int costExperience, int costEconomy, int range, int amount, int suction, int blockBreak) {
        this.level = level;
        this.costExperience = costExperience;
        this.costEconomy = costEconomy;
        this.range = range;
        this.amount = amount;
        this.blockBreak = blockBreak;
        this.suction = suction;

        EpicHoppers instance = EpicHoppers.getInstance();

        description.add(instance.getLocale().getMessage("interface.hopper.range", range));
        description.add(instance.getLocale().getMessage("interface.hopper.amount", amount));
        if (suction != 0) description.add(instance.getLocale().getMessage("interface.hopper.suction", suction));
        if (blockBreak != 0) description.add(instance.getLocale().getMessage("interface.hopper.blockbreak", blockBreak));
    }

    public List<String> getDescription() {
        return new ArrayList<>(description);
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

    public int getBlockBreak() {
        return blockBreak;
    }

    public int getSuction() {
        return suction;
    }

    public int getCostExperience() {
        return costExperience;
    }

    public int getCostEconomy() {
        return costEconomy;
    }
}

