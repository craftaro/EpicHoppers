package com.craftaro.epichoppers.containers.impl;

import com.craftaro.epichoppers.containers.CustomContainer;
import com.craftaro.epichoppers.containers.IContainer;
import com.craftaro.skyblock.SkyBlock;
import com.craftaro.skyblock.core.compatibility.CompatibleMaterial;
import com.craftaro.skyblock.core.third_party.com.cryptomorin.xseries.XMaterial;
import com.craftaro.skyblock.stackable.Stackable;
import com.craftaro.skyblock.stackable.StackableManager;
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
            XMaterial xMaterial = XMaterial.matchXMaterial(block.getType());

            this.stackable = stackableManager.getStack(block.getLocation(), xMaterial);
        }

        @Override
        public boolean addToContainer(ItemStack itemToMove) {
            if (XMaterial.matchXMaterial(itemToMove) != this.stackable.getMaterial()) {
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
            return new ItemStack[]{new ItemStack(this.stackable.getMaterial().parseMaterial(), this.stackable.getSize())};
        }

        @Override
        public void removeFromContainer(ItemStack itemToMove, int amountToMove) {
            if (XMaterial.matchXMaterial(itemToMove) != this.stackable.getMaterial()) {
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
