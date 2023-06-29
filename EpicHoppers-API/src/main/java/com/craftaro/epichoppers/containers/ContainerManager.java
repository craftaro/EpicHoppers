package com.craftaro.epichoppers.containers;

import org.bukkit.block.Block;

import java.util.Set;

public interface ContainerManager {
    Set<IContainer> getCustomContainerImplementations();

    void registerCustomContainerImplementation(String requiredPlugin, IContainer container);

    void registerCustomContainerImplementation(IContainer container);

    CustomContainer getCustomContainer(Block block);
}
