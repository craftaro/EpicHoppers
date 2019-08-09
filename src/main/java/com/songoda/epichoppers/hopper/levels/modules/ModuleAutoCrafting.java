package com.songoda.epichoppers.hopper.levels.modules;

import com.songoda.epichoppers.EpicHoppers;
import com.songoda.epichoppers.gui.GUICrafting;
import com.songoda.epichoppers.hopper.Hopper;
import com.songoda.epichoppers.utils.Methods;
import com.songoda.epichoppers.utils.ServerVersion;
import com.songoda.epichoppers.utils.StorageContainerCache;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ModuleAutoCrafting extends Module {

    private static final Map<ItemStack, Recipes> cachedRecipes = new ConcurrentHashMap<>();
    private static final Map<Hopper, ItemStack> cachedCrafting = new ConcurrentHashMap<>();
    static final ItemStack noCraft = new ItemStack(Material.AIR);

    public ModuleAutoCrafting(EpicHoppers plugin) {
        super(plugin);
    }

    @Override
    public String getName() {
        return "AutoCrafting";
    }

    @Override
    public void run(Hopper hopper, StorageContainerCache.Cache hopperCache) {
        final ItemStack toCraft;
        if (hopper == null
                || (toCraft = getAutoCrafting(hopper)) == null
                || cachedRecipes.get(toCraft) == null
                /*|| !canMove(hopperInventory, toCraft)*/)
            return;

        top:
        for (List<ItemStack> recipe : cachedRecipes.get(toCraft).getRecipes()) {

            // Do we have enough to craft this recipe?
            for(ItemStack item : recipe) {
                int amountHave = 0;
                for (ItemStack hopperItem : hopperCache.cachedInventory) {
                    if (hopperItem != null && Methods.isSimilar(hopperItem, item))
                        amountHave += hopperItem.getAmount();
                }
                if (amountHave < item.getAmount()) {
                    // Nope! Try the other recipes, just to be sure.
                    continue top;
                }
            }

            // If we've gotten this far, then we have items to craft!
            // first: can we push this crafted item down the line?
            if (!hopperCache.addItem(toCraft))
                return;

            // We're good! Remove the items used to craft!
            for(ItemStack item : recipe) {
                int amountToRemove = item.getAmount();
                for (int i = 0; amountToRemove > 0 && i < hopperCache.cachedInventory.length; i++) {
                    final ItemStack hopperItem = hopperCache.cachedInventory[i];
                    if(hopperItem != null && Methods.isSimilar(hopperItem, item)) {
                        if(amountToRemove >= hopperItem.getAmount()) {
                            amountToRemove -= hopperItem.getAmount();
                            hopperCache.removeItem(i);
                        } else {
                            hopperItem.setAmount(hopperItem.getAmount() - amountToRemove);
                            break;
                        }
                    }
                }
            }
        }
    }

    @Override
    public ItemStack getGUIButton(Hopper hopper) {
        ItemStack crafting = new ItemStack(EpicHoppers.getInstance().isServerVersionAtLeast(ServerVersion.V1_13) ? Material.CRAFTING_TABLE : Material.valueOf("WORKBENCH"), 1);
        ItemMeta craftingmeta = crafting.getItemMeta();
        craftingmeta.setDisplayName(EpicHoppers.getInstance().getLocale().getMessage("interface.hopper.craftingtitle")
                .getMessage());
        ArrayList<String> lorecrafting = new ArrayList<>();
        String[] parts = EpicHoppers.getInstance().getLocale().getMessage("interface.hopper.craftinglore")
                .getMessage().split("\\|");
        for (String line : parts) {
            lorecrafting.add(Methods.formatText(line));
        }
        craftingmeta.setLore(lorecrafting);
        crafting.setItemMeta(craftingmeta);
        return crafting;
    }

    @Override
    public void runButtonPress(Player player, Hopper hopper, ClickType type) {
        new GUICrafting(EpicHoppers.getInstance(), this, hopper, player);
    }

    @Override
    public List<Material> getBlockedItems(Hopper hopper) {
        ItemStack itemStack = getAutoCrafting(hopper);
        if (itemStack != null && itemStack.getType() != Material.AIR) {

            Recipes recipes = cachedRecipes.get(itemStack);
            if (recipes == null) {
                recipes = new Recipes(Bukkit.getServer().getRecipesFor(itemStack));
                cachedRecipes.put(itemStack, recipes);
            }

            return recipes.getAllMaterials();
        }
        return Collections.EMPTY_LIST;
    }

    @Override
    public String getDescription() {
        return EpicHoppers.getInstance().getLocale().getMessage("interface.hopper.crafting").processPlaceholder("enabled",
                EpicHoppers.getInstance().getLocale().getMessage("general.word.enabled").getMessage()).getMessage();
    }

    @Override
    public void clearData(Hopper hopper) {
        super.clearData(hopper);
        cachedCrafting.remove(hopper);
    }

    public ItemStack getAutoCrafting(Hopper hopper) {
        if (cachedCrafting.containsKey(hopper))
            return cachedCrafting.get(hopper);

        Object autocrafting = getData(hopper, "autocrafting");
        ItemStack toCraft = autocrafting instanceof ItemStack ? (ItemStack) autocrafting : decode((String) autocrafting);
        cachedCrafting.put(hopper, toCraft == null ? noCraft : toCraft);
        return toCraft;
    }

    public void setAutoCrafting(Hopper hopper, Player player, ItemStack autoCrafting) {
        saveData(hopper, "autocrafting", autoCrafting == null ? null : encode(autoCrafting), autoCrafting);
        cachedCrafting.put(hopper, autoCrafting == null ? noCraft : autoCrafting);
        if (autoCrafting == null) return;
        int excess = autoCrafting.getAmount() - 1;
        autoCrafting.setAmount(1);
        if (excess > 0 && player != null) {
            ItemStack item = autoCrafting.clone();
            item.setAmount(excess);
            player.getInventory().addItem(item);
        }
    }

    public String encode(ItemStack item) {
        return item.getType() == Material.AIR ? null : item.getType().name()
                + (item.getDurability() == 0 ? "" : ":" + item.getDurability());
    }

    public ItemStack decode(String string) {
        String autoCraftingStr = string == null ? "AIR" : string;
        String[] autoCraftingParts = autoCraftingStr.split(":");
        return new ItemStack(Material.valueOf(autoCraftingParts[0]),
                1, Short.parseShort(autoCraftingParts.length == 2 ? autoCraftingParts[1] : "0"));
    }

    final class Recipes {

        // we don't actually care about the shape, just the materials used
        private final List<List<ItemStack>> recipes = new ArrayList<>();
        private final List<Material> allTypes = new ArrayList<>();

        public Recipes() {
        }

        public Recipes(Collection<Recipe> recipes) {
             addRecipes(recipes);
        }

        public List<List<ItemStack>> getRecipes() {
            return Collections.unmodifiableList(recipes);
        }

        public List<Material> getAllMaterials() {
            return Collections.unmodifiableList(allTypes);
        }

        public void addRecipe(Recipe recipe) {
            List<ItemStack> ingredientMap;
            if (recipe instanceof ShapelessRecipe) {
                ingredientMap = ((ShapelessRecipe) recipe).getIngredientList();
            } else {
                ingredientMap = new ArrayList<>(((ShapedRecipe) recipe).getIngredientMap().values());
            }
            // consense the recipe into a list of materials and how many of each
            Map<Material, ItemStack> mergedRecipe = new HashMap<>();
            ingredientMap.stream()
                .filter(item -> item != null)
                .forEach(item -> {
                    ItemStack mergedItem = mergedRecipe.get(item.getType());
                    if (mergedItem == null) {
                        mergedRecipe.put(item.getType(), item);
                    } else {
                        mergedItem.setAmount(mergedItem.getAmount() + 1);
                    }
                });
            this.recipes.add(new ArrayList<>(mergedRecipe.values()));
            // Also keep a tally of what materials are possible for this craftable
            mergedRecipe.keySet().stream()
                .filter(itemType -> itemType != null && !allTypes.contains(itemType))
                .forEach(itemType -> {
                    allTypes.add(itemType);
                });
        }

        public void addRecipes(Collection<Recipe> recipes) {
            recipes.forEach(recipe -> this.addRecipe(recipe));
        }

        public boolean hasRecipes() {
            return !recipes.isEmpty();
        }

        public void clearRecipes() {
            recipes.clear();
        }
    }
}
