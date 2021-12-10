package com.songoda.epichoppers.containers.impl;

import com.songoda.epicfarming.EpicFarming;
import com.songoda.epicfarming.core.compatibility.CompatibleMaterial;
import com.songoda.epicfarming.farming.Farm;
import com.songoda.epichoppers.containers.CustomContainer;
import com.songoda.epichoppers.containers.IContainer;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;

public class EpicFarmingImplementation implements IContainer {

    @Override
    public CustomContainer getCustomContainer(Block block) {
        return new Container(block);
    }

    class Container extends CustomContainer {
        private final Farm farm;

        public Container(Block block) {
            super(block);
            this.farm = EpicFarming.getInstance().getFarmManager().getFarm(block);
        }

        @Override
        public boolean addToContainer(ItemStack itemToMove) {
            if (!farm.willFit(itemToMove)) {
                return false;
            }
            farm.addItem(itemToMove);
            return true;
        }

        @Override
        public ItemStack[] getItems() {
            return farm.getItems()
                    .stream().filter(i -> CompatibleMaterial.getMaterial(i) != CompatibleMaterial.BONE_MEAL)
                    .toArray(ItemStack[]::new);
        }

        @Override
        public void removeFromContainer(ItemStack itemToMove, int amountToMove) {
            farm.removeMaterial(itemToMove.getType(), amountToMove);
        }

        @Override
        public boolean isContainer() {
            return farm != null;
        }
    }
}
