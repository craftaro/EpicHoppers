package com.craftaro.epichoppers.containers.impl;

import com.craftaro.epichoppers.containers.CustomContainer;
import com.craftaro.epichoppers.containers.IContainer;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;
import us.lynuxcraft.deadsilenceiv.advancedchests.AdvancedChestsAPI;
import us.lynuxcraft.deadsilenceiv.advancedchests.chest.AdvancedChest;

public class AdvancedChestImpl implements IContainer {
    @Override
    public CustomContainer getCustomContainer(Block block) {
        return new Container(block);
    }

    static class Container extends CustomContainer {
        private final AdvancedChest advancedChest;

        public Container(Block block) {
            this.advancedChest = AdvancedChestsAPI.getChestManager().getAdvancedChest(block.getLocation());
        }

        @Override
        public boolean addToContainer(ItemStack itemToMove) {
            return AdvancedChestsAPI.addItemToChest(this.advancedChest, itemToMove);
        }

        @Override
        public ItemStack[] getItems() {
            return this.advancedChest.getAllItems().toArray(new ItemStack[0]);
        }

        @Override
        public void removeFromContainer(ItemStack itemToMove, int amountToMove) {
            for (ItemStack item : this.advancedChest.getAllItems()) {
                if (item == null) {
                    return;
                }

                if (itemToMove.getType() == item.getType()) {
                    item.setAmount(item.getAmount() - amountToMove);

                    if (item.getAmount() <= 0) {
                        this.advancedChest.getAllItems().remove(item);
                    }
                    return;
                }
            }
        }

        @Override
        public boolean isContainer() {
            return this.advancedChest != null;
        }
    }
}
