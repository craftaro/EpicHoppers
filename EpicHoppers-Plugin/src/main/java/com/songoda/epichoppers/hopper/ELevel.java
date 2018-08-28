package com.songoda.epichoppers.hopper;

import com.songoda.epichoppers.EpicHoppersPlugin;
import com.songoda.epichoppers.api.hopper.Level;

import java.util.ArrayList;
import java.util.List;

public class ELevel implements Level {

    private int level, costExperience, costEconomy, range, amount, blockBreak, suction;

    private boolean filter, teleport, crafting;

    private List<String> description = new ArrayList<>();

    public ELevel(int level, int costExperience, int costEconomy, int range, int amount, int suction, int blockBreak, boolean filter, boolean teleport, boolean crafting) {
        this.level = level;
        this.costExperience = costExperience;
        this.costEconomy = costEconomy;
        this.range = range;
        this.amount = amount;
        this.blockBreak = blockBreak;
        this.suction = suction;
        this.filter = filter;
        this.teleport = teleport;
        this.crafting = crafting;

        EpicHoppersPlugin instance = EpicHoppersPlugin.getInstance();

        description.add(instance.getLocale().getMessage("interface.hopper.range", range));
        description.add(instance.getLocale().getMessage("interface.hopper.amount", amount));
        if (suction != 0) description.add(instance.getLocale().getMessage("interface.hopper.suction", suction));
        if (blockBreak != 0) description.add(instance.getLocale().getMessage("interface.hopper.blockbreak", blockBreak));
        if (filter) description.add(instance.getLocale().getMessage("interface.hopper.filter", true));
        if (teleport) description.add(instance.getLocale().getMessage("interface.hopper.teleport", true));
        if (crafting) description.add(instance.getLocale().getMessage("interface.hopper.crafting", true));
    }

    @Override
    public List<String> getDescription() {
        return new ArrayList<>(description);
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
    public int getBlockBreak() {
        return blockBreak;
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
    public boolean isCrafting() {
        return crafting;
    }

    @Override
    public int getSuction() {
        return suction;
    }

    @Override
    public int getCostExperience() {
        return costExperience;
    }

    @Override
    public int getCostEconomy() {
        return costEconomy;
    }
}

