package com.songoda.epichoppers.containers;

import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;

public abstract class CustomContainer {

    private final Block block;
    public CustomContainer(Block block) {
        this.block = block;
    }

    public abstract boolean addToContainer(ItemStack itemToMove);
    public abstract ItemStack[] getItems();
    public abstract void removeFromContainer(ItemStack itemToMove, int amountToMove);
    public abstract boolean isContainer();
}
