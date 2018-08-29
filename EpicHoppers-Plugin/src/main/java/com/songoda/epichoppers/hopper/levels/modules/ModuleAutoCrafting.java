package com.songoda.epichoppers.hopper.levels.modules;

import com.songoda.epichoppers.EpicHoppersPlugin;
import com.songoda.epichoppers.api.hopper.Hopper;
import com.songoda.epichoppers.api.hopper.levels.modules.Module;
import com.songoda.epichoppers.utils.Debugger;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ModuleAutoCrafting implements Module {

    @Override
    public String getName() {
        return "AutoCrafting";
    }

    public void run(Hopper hopper) {
        if (hopper.getAutoCrafting() != null && canMove(hopper.getHopper().getInventory(), new ItemStack(hopper.getAutoCrafting()))) {
            org.bukkit.block.Hopper hopperBlock = hopper.getHopper();
            main:
            for (Recipe recipe : Bukkit.getServer().getRecipesFor(new ItemStack(hopper.getAutoCrafting()))) {
                if (!(recipe instanceof ShapedRecipe)) continue;
                Map<Character, ItemStack> ingredientMap = ((ShapedRecipe) recipe).getIngredientMap();
                if (hopperBlock.getInventory().getSize() == 0) continue;
                List<ItemStack> needed = stackItems(new ArrayList<>(ingredientMap.values()));

                for (ItemStack item : needed) {
                    if (!hopperBlock.getInventory().contains(item.getType(), item.getAmount())) continue main;
                }
                for (ItemStack item : needed) {
                    hopperBlock.getInventory().removeItem(item);
                }
                hopperBlock.getInventory().addItem(new ItemStack(hopper.getAutoCrafting()));
            }
        }
    }

    public List<Material> getBlockedItems(Hopper hopper) {
        List<Material> materials = new ArrayList<>();
        if (hopper.getAutoCrafting() != null) {
            for (Recipe recipe : Bukkit.getServer().getRecipesFor(new ItemStack(hopper.getAutoCrafting()))) {
                if (!(recipe instanceof ShapedRecipe)) continue;
                for (ItemStack itemStack : ((ShapedRecipe) recipe).getIngredientMap().values()) {
                    if (itemStack == null) continue;
                    materials.add(itemStack.getType());
                }
            }
        }
        return materials;
    }

    @Override
    public String getDescription() {
        return EpicHoppersPlugin.getInstance().getLocale().getMessage("interface.hopper.crafting", true);
    }

    private List<ItemStack> stackItems(List<ItemStack> items) {
        Map<Material, Integer> materials = new HashMap<>();
        for (ItemStack itemStack : items) {
            if (itemStack == null) continue;
            if (materials.containsKey(itemStack.getType())) {
                materials.put(itemStack.getType(), materials.get(itemStack.getType()) + itemStack.getAmount());
                continue;
            }
            materials.put(itemStack.getType(), itemStack.getAmount());
        }
        List<ItemStack> stacked = new ArrayList<>();
        for (Map.Entry<Material, Integer> entry : materials.entrySet()) {
            stacked.add(new ItemStack(entry.getKey(), entry.getValue()));
        }
        return stacked;
    }

    private boolean canMove(Inventory inventory, ItemStack item) {
        try {
            if (inventory.firstEmpty() != -1) return true;

            for (ItemStack stack : inventory.getContents()) {
                if (stack.isSimilar(item) && stack.getAmount() < stack.getMaxStackSize()) {
                    return true;
                }
            }
        } catch (Exception e) {
            Debugger.runReport(e);
        }
        return false;
    }
}
