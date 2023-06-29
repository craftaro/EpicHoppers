package com.craftaro.epichoppers.containers.impl;

import com.craftaro.epichoppers.containers.CustomContainer;
import com.craftaro.epichoppers.containers.IContainer;
import com.songoda.skyblock.SkyBlock;
import com.songoda.skyblock.core.compatibility.CompatibleMaterial;
import com.songoda.skyblock.stackable.Stackable;
import com.songoda.skyblock.stackable.StackableManager;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;

public class FabledSkyBlockImpl implements IContainer {
    @Override
    public CustomContainer getCustomContainer(Block block) {
        return new Container(block);
    }

    static class Container extends CustomContainer {
        private final Stackable stackable;

        public Container(Block block) {
            super();

            StackableManager stackableManager = SkyBlock.getInstance().getStackableManager();
            CompatibleMaterial compatibleMaterial = CompatibleMaterial.getMaterial(block);

            this.stackable = stackableManager.getStack(block.getLocation(), compatibleMaterial);
        }

        @Override
        public boolean addToContainer(ItemStack itemToMove) {
            if (CompatibleMaterial.getMaterial(itemToMove) != this.stackable.getMaterial()) {
                return false;
            }

            this.stackable.addOne();
            if (this.stackable.getMaxSize() > 0 && this.stackable.isMaxSize()) {
                this.stackable.setSize(this.stackable.getMaxSize());
                return false;
            }

            return true;
        }

        @Override
        public ItemStack[] getItems() {
            return new ItemStack[]{new ItemStack(this.stackable.getMaterial().getMaterial(), this.stackable.getSize())};
        }

        @Override
        public void removeFromContainer(ItemStack itemToMove, int amountToMove) {
            if (CompatibleMaterial.getMaterial(itemToMove) != this.stackable.getMaterial()) {
                return;
            }

            this.stackable.setSize(this.stackable.getSize() - amountToMove);
        }

        @Override
        public boolean isContainer() {
            return this.stackable != null;
        }
    }
}
