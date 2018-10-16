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

    public static List<ItemStack> compressItemStack(List<ItemStack> target) {
        HashMap<Material, ItemStack> sortingList = new HashMap<>();
        for (ItemStack item : target) {
            if (sortingList.containsKey(item.getType())) {
                ItemStack existing = sortingList.get(item.getType());
                existing.setAmount(existing.getAmount() + item.getAmount());
                sortingList.put(existing.getType(), existing);
            } else {
                sortingList.put(item.getType(), item);
            }
        }
        List<ItemStack> list = new ArrayList<>(sortingList.values());
        return list;
    }

    @Override
    public String getName() {
        return "AutoCrafting";
    }

    public void run(Hopper hopper) {
        if (hopper.getAutoCrafting() != null && canMove(hopper.getHopper().getInventory(), new ItemStack(hopper.getAutoCrafting()))) {
            org.bukkit.block.Hopper hopperBlock = hopper.getHopper();
            main:
            for (Recipe recipe : Bukkit.getServer().getRecipesFor(new ItemStack(hopper.getAutoCrafting()))) {
                if (!(recipe instanceof ShapedRecipe) && !(recipe instanceof ShapelessRecipe)) continue;
                List<ItemStack> ingredientMap = null;
                if (recipe instanceof ShapelessRecipe) ingredientMap = ((ShapelessRecipe) recipe).getIngredientList();
                if (recipe instanceof ShapedRecipe)
                    ingredientMap = new ArrayList<>(((ShapedRecipe) recipe).getIngredientMap().values());
                if (hopperBlock.getInventory().getSize() == 0) continue;


                Map<Material, Integer> items = new HashMap<>();
                for (ItemStack item : ingredientMap) {
                    if (!items.containsKey(item.getType())) {
                        items.put(item.getType(), item.getAmount());
                    } else {
                        items.put(item.getType(), items.get(item.getType()) + 1);
                    }
                }

                for (Material material : items.keySet()) {
                    ItemStack item = new ItemStack(material, items.get(material));
                    if (!hopperBlock.getInventory().contains(item)) {
                        continue main;
                    }
                }
                for (Material material : items.keySet()) {
                    ItemStack item = new ItemStack(material, items.get(material));
                    hopperBlock.getInventory().removeItem(item);
                }
                hopperBlock.getInventory().addItem(recipe.getResult());
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
