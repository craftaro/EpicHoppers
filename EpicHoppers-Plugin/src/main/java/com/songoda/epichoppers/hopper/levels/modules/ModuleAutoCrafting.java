package com.songoda.epichoppers.hopper.levels.modules;

import com.songoda.epichoppers.EpicHoppersPlugin;
import com.songoda.epichoppers.api.hopper.Hopper;
import com.songoda.epichoppers.api.hopper.levels.modules.Module;
import com.songoda.epichoppers.utils.Debugger;
import jdk.nashorn.internal.ir.Block;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ModuleAutoCrafting implements Module {

    private final Map<Material, Recipe> cachedRecipes = new HashMap<>();
    private final Map<Hopper, Material> lastMaterial = new HashMap<>();

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
        org.bukkit.block.Hopper hopperBlock = hopper.getHopper();
        if (hopper.getAutoCrafting() != null && canMove(hopperBlock.getInventory(), new ItemStack(hopper.getAutoCrafting()))) {

            Recipe recipe = cachedRecipes.get(hopper.getAutoCrafting());
                if (!(recipe instanceof ShapedRecipe) && !(recipe instanceof ShapelessRecipe)) return;
                List<ItemStack> ingredientMap = null;
                if (recipe instanceof ShapelessRecipe) ingredientMap = ((ShapelessRecipe) recipe).getIngredientList();
                if (recipe instanceof ShapedRecipe)
                    ingredientMap = new ArrayList<>(((ShapedRecipe) recipe).getIngredientMap().values());
                if (hopperBlock.getInventory().getSize() == 0) return;

                Map<Material, Integer> items = new HashMap<>();
                for (ItemStack item : ingredientMap) {
                    if (!items.containsKey(item.getType())) {
                        items.put(item.getType(), item.getAmount());
                    } else {
                        items.put(item.getType(), items.get(item.getType()) + 1);
                    }
                }

                for (Material material : items.keySet()) {
                    int amt = 0;
                    ItemStack item = new ItemStack(material, items.get(material));
                    for (ItemStack i : hopperBlock.getInventory().getContents()) {
                        if (i == null) continue;
                        if (i.getType() != material) continue;
                        amt += i.getAmount();
                    }

                    if (amt < item.getAmount()) {
                        return;
                    }
                }
                main2:
                for (Material material : items.keySet()) {
                    int amtRemoved = 0;
                    ItemStack toRemove = new ItemStack(material, items.get(material));
                    for (ItemStack i : hopperBlock.getInventory().getContents()) {
                        if (i ==  null || i.getType() != material) continue;
                        if (toRemove.getAmount() - amtRemoved <= i.getAmount()) {
                            toRemove.setAmount(toRemove.getAmount() - amtRemoved);
                            hopperBlock.getInventory().removeItem(toRemove);
                            continue main2;
                        } else {
                            amtRemoved += i.getAmount();
                            hopperBlock.getInventory().removeItem(i);
                        }
                    }
                }
                hopperBlock.getInventory().addItem(recipe.getResult());
            }

    }

    public List<Material> getBlockedItems(Hopper hopper) {
        List<Material> materials = new ArrayList<>();
        if (hopper.getAutoCrafting() != null) {

            Material material = hopper.getAutoCrafting();

            if (material == Material.AIR) return materials;

            if (lastMaterial.get(hopper) != material) {
                lastMaterial.put(hopper, material);
                cachedRecipes.remove(hopper);
            }

            if (!cachedRecipes.containsKey(material)) {
                for (Recipe recipe : Bukkit.getServer().getRecipesFor(new ItemStack(material))) {
                    cachedRecipes.put(material, recipe);
                }
            } else {
                Recipe recipe = cachedRecipes.get(material);
                if (recipe instanceof ShapedRecipe) {
                    for (ItemStack itemStack : ((ShapedRecipe) recipe).getIngredientMap().values()) {
                        if (itemStack == null) continue;
                        materials.add(itemStack.getType());
                    }
                } else if (recipe instanceof ShapelessRecipe) {
                    for (ItemStack itemStack : ((ShapelessRecipe) recipe).getIngredientList()) {
                        if (itemStack == null) continue;
                        materials.add(itemStack.getType());
                    }
                }
            }
        }
        return materials;
    }

    @Override
    public String getDescription() {
        return EpicHoppersPlugin.getInstance().getLocale().getMessage("interface.hopper.crafting", true);
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
