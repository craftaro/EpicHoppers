package com.songoda.epichoppers.containers.impl;

import com.songoda.epichoppers.containers.CustomContainer;
import com.songoda.epichoppers.containers.IContainer;
import com.songoda.skyblock.SkyBlock;
import com.songoda.skyblock.core.compatibility.CompatibleMaterial;
import com.songoda.skyblock.stackable.Stackable;
import com.songoda.skyblock.stackable.StackableManager;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;

public class FabledSkyBlockImplementation implements IContainer {

    @Override
    public CustomContainer getCustomContainer(Block block) {
        return new Container(block);
    }

    class Container extends CustomContainer {
        private final Stackable stackable;

        public Container(Block block) {
            super(block);

            StackableManager stackableManager = SkyBlock.getInstance().getStackableManager();
            CompatibleMaterial compatibleMaterial = CompatibleMaterial.getMaterial(block);

            this.stackable = stackableManager.getStack(block.getLocation(), compatibleMaterial);
        }

        @Override
        public boolean addToContainer(ItemStack itemToMove) {
            if (CompatibleMaterial.getMaterial(itemToMove) != stackable.getMaterial()) {
                return false;
            }

            stackable.addOne();
            if (stackable.isMaxSize()) {
                stackable.setSize(stackable.getMaxSize());
                return false;
            }

            return true;
        }

        @Override
        public ItemStack[] getItems() {
            ItemStack[] array = { new ItemStack(stackable.getMaterial().getMaterial(), stackable.getSize()) };
            return array;
        }

        @Override
        public void removeFromContainer(ItemStack itemToMove, int amountToMove) {
            if (CompatibleMaterial.getMaterial(itemToMove) != stackable.getMaterial()) {
                return;
            }

            stackable.setSize(stackable.getSize() - amountToMove);
        }

        @Override
        public boolean isContainer() {
            return stackable != null;
        }
    }
}
