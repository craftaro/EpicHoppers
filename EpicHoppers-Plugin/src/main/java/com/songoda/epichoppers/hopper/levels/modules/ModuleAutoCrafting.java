package com.songoda.epichoppers.hopper.levels.modules;

import com.songoda.epichoppers.EpicHoppersPlugin;
import com.songoda.epichoppers.api.hopper.Hopper;
import com.songoda.epichoppers.api.hopper.levels.modules.Module;
import com.songoda.epichoppers.utils.Debugger;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ModuleAutoCrafting implements Module {

    private final Map<Material, Recipe> cachedRecipes = new HashMap<>();

    @Override
    public String getName() {
        return "AutoCrafting";
    }

    public void run(Hopper hopper) {
        if (hopper.getAutoCrafting() != null && canMove(hopper.getHopper().getInventory(), new ItemStack(hopper.getAutoCrafting()))) {
            org.bukkit.block.Hopper hopperBlock = hopper.getHopper();
            main:
            for (Recipe recipe : Bukkit.getServer().getRecipesFor(new ItemStack(hopper.getAutoCrafting()))) {
                if (!(recipe instanceof ShapedRecipe)&&!(recipe instanceof ShapelessRecipe)) continue;
                List<ItemStack> ingredientMap = null;
                if(recipe instanceof ShapelessRecipe)ingredientMap = ((ShapelessRecipe) recipe).getIngredientList();
                if(recipe instanceof ShapedRecipe)ingredientMap = (List<ItemStack>) ((ShapedRecipe) recipe).getIngredientMap().values();
                if (hopperBlock.getInventory().getSize() == 0) continue;

                for (ItemStack item : ingredientMap) {
                    if (!hopperBlock.getInventory().contains(item.getType(), item.getAmount())) continue main;
                }
                for (ItemStack item : ingredientMap) {
                    hopperBlock.getInventory().removeItem(item);
                }
                hopperBlock.getInventory().addItem(new ItemStack(hopper.getAutoCrafting()));
            }
        }
    }

    public List<Material> getBlockedItems(Hopper hopper) {
        List<Material> materials = new ArrayList<>();
        if (hopper.getAutoCrafting() != null) {

            Material material = hopper.getAutoCrafting();

            if (!cachedRecipes.containsKey(material)) {
                for (Recipe recipe : Bukkit.getServer().getRecipesFor(new ItemStack(material))) {
                    if (!(recipe instanceof ShapedRecipe)) continue;
                    cachedRecipes.put(material, recipe);
                }
            } else {
                for (ItemStack itemStack : ((ShapedRecipe) cachedRecipes.get(material)).getIngredientMap().values()) {
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
                if (stack.isSimilar(item) && (stack.getAmount() + item.getAmount()) < stack.getMaxStackSize()) {
                    return true;
                }
            }
        } catch (Exception e) {
            Debugger.runReport(e);
        }
        return false;
    }
}
