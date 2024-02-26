package com.craftaro.epichoppers.containers;

import org.bukkit.inventory.ItemStack;

public abstract class CustomContainer {
    public abstract boolean addToContainer(ItemStack itemToMove);

    public abstract ItemStack[] getItems();

    public abstract void removeFromContainer(ItemStack itemToMove, int amountToMove);

    public abstract boolean isContainer();
}
