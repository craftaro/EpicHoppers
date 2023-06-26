package com.songoda.epichoppers.containers;

import com.songoda.epichoppers.containers.impl.AdvancedChestImplementation;
import com.songoda.epichoppers.containers.impl.EpicFarmingImplementation;
import com.songoda.epichoppers.containers.impl.FabledSkyBlockImplementation;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.plugin.PluginManager;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class ContainerManager {
    private final Set<IContainer> customContainers;

    public ContainerManager() {
        this.customContainers = new HashSet<>();

        registerCustomContainerImplementation("AdvancedChests", new AdvancedChestImplementation());
        registerCustomContainerImplementation("EpicFarming", new EpicFarmingImplementation());
        registerCustomContainerImplementation("FabledSkyBlock", new FabledSkyBlockImplementation());
    }

    public Set<IContainer> getCustomContainerImplementations() {
        return Collections.unmodifiableSet(this.customContainers);
    }

    public void registerCustomContainerImplementation(String requiredPlugin, IContainer container) {
        PluginManager pluginManager = Bukkit.getPluginManager();
        if (requiredPlugin != null && pluginManager.isPluginEnabled(requiredPlugin)) {
            this.customContainers.add(container);
        }
    }

    public void registerCustomContainerImplementation(IContainer container) {
        registerCustomContainerImplementation(null, container);
    }

    public CustomContainer getCustomContainer(Block block) {
        for (IContainer container : this.customContainers) {
            CustomContainer customContainer = container.getCustomContainer(block);
            if (customContainer.isContainer()) {
                return customContainer;
            }
        }

        return null;
    }
}
