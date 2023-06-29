package com.craftaro.epichoppers.hopper.levels;

import com.craftaro.core.SongodaPlugin;
import com.craftaro.epichoppers.hopper.levels.modules.Module;
import org.bukkit.Bukkit;

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
        this.description.clear();

        this.description.add(getPlugin().getLocale().getMessage("interface.hopper.range")
                .processPlaceholder("range", this.range).getMessage());
        this.description.add(getPlugin().getLocale().getMessage("interface.hopper.amount")
                .processPlaceholder("amount", this.amount).getMessage());
        if (this.linkAmount != 1) {
            this.description.add(getPlugin().getLocale().getMessage("interface.hopper.linkamount")
                    .processPlaceholder("amount", this.linkAmount).getMessage());
        }
        if (this.filter) {
            this.description.add(getPlugin().getLocale().getMessage("interface.hopper.filter")
                    .processPlaceholder("enabled", getPlugin().getLocale()
                            .getMessage("general.word.enabled").getMessage()).getMessage());
        }
        if (this.teleport) {
            this.description.add(getPlugin()
                    .getLocale()
                    .getMessage("interface.hopper.teleport")
                    .processPlaceholder(
                            "enabled",
                            getPlugin()
                                    .getLocale()
                                    .getMessage("general.word.enabled")
                                    .getMessage())
                    .getMessage());
        }

        for (Module module : this.registeredModules) {
            this.description.add(module.getDescription());
        }
    }


    public int getLevel() {
        return this.level;
    }


    public int getRange() {
        return this.range;
    }


    public int getAmount() {
        return this.amount;
    }


    public boolean isFilter() {
        return this.filter;
    }


    public boolean isTeleport() {
        return this.teleport;
    }


    public int getLinkAmount() {
        return this.linkAmount;
    }


    public int getCostExperience() {
        return this.costExperience;
    }


    public int getCostEconomy() {
        return this.costEconomy;
    }


    public List<String> getDescription() {
        return new ArrayList<>(this.description);
    }


    public ArrayList<Module> getRegisteredModules() {
        return new ArrayList<>(this.registeredModules);
    }


    public void addModule(Module module) {
        this.registeredModules.add(module);
        buildDescription();
    }


    public Module getModule(String name) {
        if (this.registeredModules == null) {
            return null;
        }

        for (Module module : this.registeredModules) {
            if (module.getName().equals(name)) {
                return module;
            }
        }
        return null;
    }

    /**
     * @deprecated The class needs refactoring to not even need the plugin.
     * This is just a temporary workaround to get a Minecraft 1.20-beta build ready
     */
    @Deprecated
    private SongodaPlugin getPlugin() {
        return (SongodaPlugin) Bukkit.getPluginManager().getPlugin("EpicHoppers");
    }
}
