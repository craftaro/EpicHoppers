package com.craftaro.epichoppers.containers.impl;

import com.craftaro.epichoppers.containers.CustomContainer;
import com.craftaro.epichoppers.containers.IContainer;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;
import us.lynuxcraft.deadsilenceiv.advancedchests.AdvancedChestsAPI;
import us.lynuxcraft.deadsilenceiv.advancedchests.chest.AdvancedChest;
import us.lynuxcraft.deadsilenceiv.advancedchests.utils.inventory.InteractiveInventory;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class AdvancedChestImpl implements IContainer {
    @Override
    public CustomContainer getCustomContainer(Block block) {
        return new Container(block);
    }

    static class Container extends CustomContainer {
        private final AdvancedChest<?, ?> advancedChest;

        public Container(Block block) {
            this.advancedChest = AdvancedChestsAPI.getChestManager().getAdvancedChest(block.getLocation());
        }

        @Override
        public boolean addToContainer(ItemStack itemToMove) {
//            return AdvancedChestsAPI.addItemToChest(this.advancedChest, itemToMove);
            if (advancedChest != null) {
                Optional<InteractiveInventory> inv = advancedChest.getSubInventories().stream().filter(subInventory -> subInventory.getBukkitInventory().firstEmpty() != -1).findFirst();
                if (inv.isPresent()) {
                    return inv.get().getBukkitInventory().addItem(itemToMove).isEmpty();
                }
            }
            return false;
        }

        @Override
        public ItemStack[] getItems() {
//            return this.advancedChest.getAllItems().toArray(new ItemStack[0]);
            return this.advancedChest.getSubInventories().stream().map(subInventory -> subInventory.getBukkitInventory().getContents()).collect(Collectors.toList()).stream().flatMap(Arrays::stream).toArray(ItemStack[]::new);
        }

        @Override
        public void removeFromContainer(ItemStack itemToMove, int amountToMove) {
            for (ItemStack item : getItems()) {
                if (item == null) {
                    return;
                }

                if (itemToMove.getType() == item.getType()) {
                    item.setAmount(item.getAmount() - amountToMove);

                    if (item.getAmount() <= 0) {
                        item.setType(Material.AIR);
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
