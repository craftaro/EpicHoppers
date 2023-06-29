package com.craftaro.epichoppers.containers.impl;

import com.songoda.epicfarming.EpicFarming;
import com.songoda.epicfarming.core.compatibility.CompatibleMaterial;
import com.songoda.epicfarming.farming.Farm;
import com.craftaro.epichoppers.containers.CustomContainer;
import com.craftaro.epichoppers.containers.IContainer;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;

public class EpicFarmingImpl implements IContainer {
    @Override
    public CustomContainer getCustomContainer(Block block) {
        return new Container(block);
    }

    static class Container extends CustomContainer {
        private final Farm farm;

        public Container(Block block) {
            this.farm = EpicFarming.getInstance().getFarmManager().getFarm(block);
        }

        @Override
        public boolean addToContainer(ItemStack itemToMove) {
            if (!this.farm.willFit(itemToMove)) {
                return false;
            }
            this.farm.addItem(itemToMove);
            return true;
        }

        @Override
        public ItemStack[] getItems() {
            return this.farm.getItems()
                    .stream().filter(item -> CompatibleMaterial.getMaterial(item) != CompatibleMaterial.BONE_MEAL)
                    .toArray(ItemStack[]::new);
        }

        @Override
        public void removeFromContainer(ItemStack itemToMove, int amountToMove) {
            this.farm.removeMaterial(itemToMove.getType(), amountToMove);
        }

        @Override
        public boolean isContainer() {
            return this.farm != null;
        }
    }
}
