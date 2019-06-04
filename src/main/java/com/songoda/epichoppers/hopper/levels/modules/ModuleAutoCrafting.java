package com.songoda.epichoppers.hopper.levels.modules;

import com.songoda.epichoppers.EpicHoppers;
import com.songoda.epichoppers.gui.GUICrafting;
import com.songoda.epichoppers.hopper.Hopper;
import com.songoda.epichoppers.utils.Methods;
import com.songoda.epichoppers.utils.ServerVersion;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ModuleAutoCrafting implements Module {

    private final Map<ItemStack, Recipes> cachedRecipes = new HashMap<>();

    @Override
    public String getName() {
        return "AutoCrafting";
    }

    public void run(Hopper hopper, Inventory hopperInventory) {
        if (hopper.getAutoCrafting() == null
                || hopperInventory == null
                || hopperInventory.getSize() == 0
                || !canMove(hopperInventory, new ItemStack(hopper.getAutoCrafting()))
                || cachedRecipes.get(hopper.getAutoCrafting()) == null)
            return;

        top:
        for (Recipe recipe : cachedRecipes.get(hopper.getAutoCrafting()).getRecipes()) {
            if (!(recipe instanceof ShapedRecipe) && !(recipe instanceof ShapelessRecipe))
                continue;

            List<ItemStack> ingredientMap;
            if (recipe instanceof ShapelessRecipe) {
                ingredientMap = ((ShapelessRecipe) recipe).getIngredientList();
            } else {
                ingredientMap = new ArrayList<>(((ShapedRecipe) recipe).getIngredientMap().values());
            }

            Map<Material, ItemStack> items = new HashMap<>();
            for (ItemStack item : ingredientMap) {
                if (item == null)
                    continue;

                if (!items.containsKey(item.getType())) {
                    items.put(item.getType(), item.clone());
                } else {
                    items.get(item.getType()).setAmount(items.get(item.getType()).getAmount() + 1);
                }
            }

            for (ItemStack item : items.values()) {
                int amt = 0;
                for (ItemStack i : hopperInventory.getContents()) {
                    if (i == null || !isSimilar(i, item))
                        continue;
                    amt += i.getAmount();
                }

                if (amt < item.getAmount()) {
                    continue top;
                }
            }

            main2:
            for (ItemStack toRemove : items.values()) {
                int amtRemoved = 0;
                for (ItemStack i : hopperInventory.getContents()) {
                    if (i == null || !isSimilar(i, toRemove))
                        continue;

                    amtRemoved += Math.min(toRemove.getAmount() - amtRemoved, i.getAmount());
                    if (amtRemoved == i.getAmount())
                        hopperInventory.removeItem(i);
                    else
                        i.setAmount(i.getAmount() - amtRemoved);

                    if (amtRemoved == toRemove.getAmount())
                        continue main2;
                }
            }

            hopperInventory.addItem(recipe.getResult());
        }
    }

    @Override
    public ItemStack getGUIButton(Hopper hopper) {
        ItemStack crafting = new ItemStack(EpicHoppers.getInstance().isServerVersionAtLeast(ServerVersion.V1_13) ? Material.CRAFTING_TABLE : Material.valueOf("WORKBENCH"), 1);
        ItemMeta craftingmeta = crafting.getItemMeta();
        craftingmeta.setDisplayName(EpicHoppers.getInstance().getLocale().getMessage("interface.hopper.craftingtitle"));
        ArrayList<String> lorecrafting = new ArrayList<>();
        String[] parts = EpicHoppers.getInstance().getLocale().getMessage("interface.hopper.craftinglore").split("\\|");
        for (String line : parts) {
            lorecrafting.add(Methods.formatText(line));
        }
        craftingmeta.setLore(lorecrafting);
        crafting.setItemMeta(craftingmeta);
        return crafting;
    }

    @Override
    public void runButtonPress(Player player, Hopper hopper) {
        new GUICrafting(EpicHoppers.getInstance(), hopper, player);
    }

    public List<Material> getBlockedItems(Hopper hopper) {
        List<Material> materials = new ArrayList<>();
        if (hopper.getAutoCrafting() != null) {

            ItemStack itemStack = hopper.getAutoCrafting();

            if (itemStack.getType() == Material.AIR)
                return materials;

            if (cachedRecipes.get(itemStack) == null) {
                Recipes recipes = new Recipes();
                for (Recipe recipe : Bukkit.getServer().getRecipesFor(itemStack)) {
                    recipes.addRecipe(recipe);
                }
                cachedRecipes.put(itemStack, recipes);
            }

            if (cachedRecipes.get(itemStack) != null) {
                Recipes recipes = cachedRecipes.get(itemStack);
                for (Recipe recipe : recipes.getRecipes()) {
                    if (recipe instanceof ShapedRecipe) {
                        for (ItemStack itemStack1 : ((ShapedRecipe) recipe).getIngredientMap().values()) {
                            if (itemStack1 == null)
                                continue;
                            materials.add(itemStack1.getType());
                        }
                    } else if (recipe instanceof ShapelessRecipe) {
                        for (ItemStack itemStack1 : ((ShapelessRecipe) recipe).getIngredientList()) {
                            if (itemStack1 == null)
                                continue;
                            materials.add(itemStack1.getType());
                        }
                    }
                }
            }
        }
        return materials;
    }

    @Override
    public String getDescription() {
        return EpicHoppers.getInstance().getLocale().getMessage("interface.hopper.crafting", EpicHoppers.getInstance().getLocale().getMessage("general.word.enabled"));
    }

    private boolean canMove(Inventory inventory, ItemStack item) {
        if (inventory.firstEmpty() != -1) return true;

        for (ItemStack stack : inventory.getContents()) {
            if (stack.isSimilar(item) && (stack.getAmount() + item.getAmount()) < stack.getMaxStackSize()) {
                return true;
            }
        }
        return false;
    }

    private boolean isSimilar(ItemStack is1, ItemStack is2) {
        if (EpicHoppers.getInstance().isServerVersionAtLeast(ServerVersion.V1_13)) {
            return is1.getType() == is2.getType();
        } else {
            return is1.getType() == is2.getType() || is1.getDurability() == is2.getDurability();
        }
    }

    class Recipes {

        private List<Recipe> recipes = new ArrayList<>();

        public List<Recipe> getRecipes() {
            return new ArrayList<>(recipes);
        }

        public void addRecipe(Recipe recipe) {
            this.recipes.add(recipe);
        }

        public void clearRecipes() {
            recipes.clear();
        }
    }
}
