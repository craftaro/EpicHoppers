package com.craftaro.epichoppers.hopper.levels.modules;

import com.craftaro.core.SongodaPlugin;
import com.craftaro.core.gui.GuiManager;
import com.craftaro.core.third_party.com.cryptomorin.xseries.XMaterial;
import com.craftaro.core.utils.TextUtils;
import com.craftaro.epichoppers.settings.Settings;
import com.craftaro.epichoppers.utils.Methods;
import com.craftaro.epichoppers.gui.GUICrafting;
import com.craftaro.epichoppers.hopper.Hopper;
import com.craftaro.epichoppers.utils.StorageContainerCache;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class ModuleAutoCrafting extends Module {
    private static final Map<ItemStack, Recipes> CACHED_RECIPES = new ConcurrentHashMap<>();
    private static final Map<Hopper, ItemStack> CACHED_CRAFTING = new ConcurrentHashMap<>();
    private static final ItemStack NO_CRAFT = new ItemStack(Material.AIR);

    private final boolean crafterEjection;

    public ModuleAutoCrafting(SongodaPlugin plugin, GuiManager guiManager) {
        super(plugin, guiManager);
        this.crafterEjection = Settings.AUTOCRAFT_JAM_EJECT.getBoolean();
    }

    @Override
    public String getName() {
        return "AutoCrafting";
    }

    @Override
    public void run(Hopper hopper, StorageContainerCache.Cache hopperCache) {
        final ItemStack toCraft;
        if (hopper == null || (toCraft = getAutoCrafting(hopper)) == null || toCraft.getType() == Material.AIR) {
            return;
        }

        synchronized (hopperCache) {    //TODO: Check if this is required
            ItemStack[] items = hopperCache.cachedInventory;

            recipeLoop:
            for (SimpleRecipe recipe : getRecipes(toCraft).recipes) {
                // key=indexForItemsArray, value=amountAfterCrafting
                Map<Integer, Integer> slotsToAlter = new HashMap<>();

                for (SimpleRecipe.SimpleIngredient ingredient : recipe.ingredients) {
                    int amount = ingredient.item.getAmount() + ingredient.getAdditionalAmount();

                    for (int i = 0; i < items.length; i++) {
                        ItemStack item = items[i];

                        if (item == null) {
                            continue;
                        }
                        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
                            continue;
                        }

                        boolean sameMaterial = Methods.isSimilarMaterial(item, ingredient.item);

                        // Check if any alternative Material matches
                        if (!sameMaterial) {
                            for (ItemStack alternativeType : ingredient.alternativeTypes) {
                                if (Methods.isSimilarMaterial(item, alternativeType)) {
                                    sameMaterial = true;
                                    break;
                                }
                            }

                            // Still doesn't not match --> Skip this item
                            if (!sameMaterial) {
                                continue;
                            }
                        }

                        if (item.getAmount() >= amount) {
                            slotsToAlter.put(i, item.getAmount() - amount);
                            amount = 0;
                        } else {
                            slotsToAlter.put(i, 0);
                            amount -= item.getAmount();
                        }
                    }

                    // Not enough ingredients for this recipe
                    if (amount != 0) {
                        continue recipeLoop;
                    }
                }

                boolean freeSlotAfterRemovingIngredients =
                        slotsToAlter.values().stream().anyMatch(iAmount -> iAmount <= 0) ||
                                Arrays.stream(items).anyMatch(item -> item == null ||
                                        (item.getAmount() + recipe.result.getAmount() <= item.getMaxStackSize() &&
                                                recipe.result.isSimilar(item)));

                // jam check: is this hopper gummed up?
                if (this.crafterEjection && !freeSlotAfterRemovingIngredients) {
                    // Crafter can't function if there's nowhere to put the output
                    // ¯\_(ツ)_/¯

                    for (int i = 0; i < items.length; i++) {
                        if (!slotsToAlter.containsKey(i)) {
                            // and yeet into space!
                            hopper.getWorld().dropItemNaturally(hopper.getLocation(), items[i]);
                            items[i] = null;

                            freeSlotAfterRemovingIngredients = true;
                            break;
                        }
                    }

                    // FIXME: In theory the code below should work. But if the last item is of the same type as the
                    //        resulting item, the inventory won't update correctly
                    //        (item is set correctly but reset to MaxStackSize)
                    //        (CachedInventory doesn't like it's array to be edited?)
                        /*
                        // None of the slots can safely be freed. So we drop some leftover ingredients
                        if (!freeSlotAfterRemovingIngredients) {

                            int slot = items.length - 1;   // Last slot

                            slotsToAlter.computeIfPresent(slot, (key, value) -> {
                                items[slot].setAmount(value);

                                return null;
                            });

                            // and yeet into space!
                            items[slot].setAmount(slotsToAlter.getOrDefault(slot, items[slot].getAmount()));
                            hopper.getWorld().dropItemNaturally(hopper.getLocation(), items[slot]);
                            items[slot] = null;

                            freeSlotAfterRemovingIngredients = true;
                        }
                        */
                }

                if (freeSlotAfterRemovingIngredients) {
                    for (Map.Entry<Integer, Integer> entry : slotsToAlter.entrySet()) {
                        if (entry.getValue() <= 0) {
                            items[entry.getKey()] = null;
                        } else {
                            items[entry.getKey()].setAmount(entry.getValue());
                        }
                    }

                    // Add the resulting item into the inventory - Just making sure there actually is enough space
                    for (int i = 0; i < items.length; i++) {
                        if (items[i] == null ||
                                (items[i].isSimilar(recipe.result)
                                        && items[i].getAmount() + recipe.result.getAmount() <= items[i].getMaxStackSize())) {
                            if (items[i] == null) {
                                items[i] = recipe.result.clone();
                            } else {
                                items[i].setAmount(items[i].getAmount() + recipe.result.getAmount());
                            }

                            break;
                        }
                    }

                    hopperCache.setContents(items);
                }
            }
        }
    }

    @Override
    public ItemStack getGUIButton(Hopper hopper) {
        ItemStack crafting = XMaterial.CRAFTING_TABLE.parseItem();
        ItemMeta craftingMeta = crafting.getItemMeta();
        craftingMeta.setDisplayName(this.plugin.getLocale().getMessage("interface.hopper.craftingtitle")
                .getMessage());
        ArrayList<String> lorecrafting = new ArrayList<>();
        String[] parts = this.plugin.getLocale().getMessage("interface.hopper.craftinglore")
                .getMessage().split("\\|");
        for (String line : parts) {
            lorecrafting.add(TextUtils.formatText(line));
        }
        craftingMeta.setLore(lorecrafting);
        crafting.setItemMeta(craftingMeta);
        return crafting;
    }

    @Override
    public void runButtonPress(Player player, Hopper hopper, ClickType type) {
        hopper.setActivePlayer(player);
        this.guiManager.showGUI(player, new GUICrafting(this, this.plugin, hopper, player));
    }

    @Override
    public List<Material> getBlockedItems(Hopper hopper) {
        ItemStack itemStack = getAutoCrafting(hopper);
        if (itemStack != null && itemStack.getType() != Material.AIR) {
            return getRecipes(itemStack).getPossibleIngredientTypes();
        }
        return Collections.emptyList();
    }

    @Override
    public String getDescription() {
        return this.plugin.getLocale()
                .getMessage("interface.hopper.crafting")
                .processPlaceholder("enabled", this.plugin.getLocale().getMessage("general.word.enabled").getMessage())
                .getMessage();
    }

    @Override
    public void clearData(Hopper hopper) {
        super.clearData(hopper);
        CACHED_CRAFTING.remove(hopper);
    }

    private Recipes getRecipes(ItemStack toCraft) {
        Recipes recipes = CACHED_RECIPES.get(toCraft);
        if (Settings.AUTOCRAFT_BLACKLIST.getStringList().stream()
                .anyMatch(r -> r.equalsIgnoreCase(toCraft.getType().name()))) {
            return new Recipes();
        }

        if (recipes == null) {
            try {
                recipes = new Recipes(Bukkit.getServer().getRecipesFor(toCraft));
            } catch (Throwable t) {
                // extremely rare, but y'know - some plugins are dumb
                recipes = new Recipes();
                // how's about we try this manually?
                java.util.Iterator<Recipe> recipeIterator = Bukkit.getServer().recipeIterator();
                while (recipeIterator.hasNext()) {
                    try {
                        Recipe recipe = recipeIterator.next();

                        ItemStack stack = recipe.getResult();
                        if (Methods.isSimilarMaterial(stack, toCraft)) {
                            recipes.addRecipe(recipe);
                        }
                    } catch (Throwable ignored) {
                    }
                }
            }

            CACHED_RECIPES.put(toCraft, recipes);
        }

        return recipes;
    }

    public ItemStack getAutoCrafting(Hopper hopper) {
        if (CACHED_CRAFTING.containsKey(hopper)) {
            return CACHED_CRAFTING.get(hopper);
        }

        Object autocrafting = getData(hopper, "autocrafting");
        ItemStack toCraft = autocrafting instanceof ItemStack ? (ItemStack) autocrafting : decode((String) autocrafting);
        CACHED_CRAFTING.put(hopper, toCraft == null ? NO_CRAFT : toCraft);
        return toCraft;
    }

    public void setAutoCrafting(Hopper hopper, Player player, ItemStack autoCrafting) {
        saveData(hopper, "autocrafting", autoCrafting == null ? null : encode(autoCrafting), autoCrafting);
        CACHED_CRAFTING.put(hopper, autoCrafting == null ? NO_CRAFT : autoCrafting);
        if (autoCrafting == null) {
            return;
        }
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

    private static final class Recipes {
        private final List<SimpleRecipe> recipes = new ArrayList<>();
        // Used for the blacklist to ensure that items are not going to get transferred
        private final List<Material> possibleIngredientTypes = new ArrayList<>();

        public Recipes() {
        }

        public Recipes(Collection<Recipe> recipes) {
            addRecipes(recipes);
        }

        public List<SimpleRecipe> getRecipes() {
            return Collections.unmodifiableList(this.recipes);
        }

        public List<Material> getPossibleIngredientTypes() {
            return Collections.unmodifiableList(this.possibleIngredientTypes);
        }

        public void addRecipe(Recipe recipe) {
            SimpleRecipe simpleRecipe = null;

            if (recipe instanceof ShapelessRecipe) {
                simpleRecipe = new SimpleRecipe((ShapelessRecipe) recipe);
            } else if (recipe instanceof ShapedRecipe) {
                simpleRecipe = new SimpleRecipe((ShapedRecipe) recipe);
            }

            // Skip unsupported recipe type
            if (simpleRecipe == null) {
                return;
            }

            this.recipes.add(simpleRecipe);

            // Keep a list of all possible ingredients.
            for (SimpleRecipe.SimpleIngredient ingredient : simpleRecipe.ingredients) {
                if (!this.possibleIngredientTypes.contains(ingredient.item.getType())) {
                    this.possibleIngredientTypes.add(ingredient.item.getType());
                }

                for (ItemStack material : ingredient.alternativeTypes) {
                    if (!this.possibleIngredientTypes.contains(material.getType())) {
                        this.possibleIngredientTypes.add(material.getType());
                    }
                }
            }
        }

        public void addRecipes(Collection<Recipe> recipes) {
            recipes.forEach(this::addRecipe);
        }

        public boolean hasRecipes() {
            return !this.recipes.isEmpty();
        }

        public void clearRecipes() {
            this.recipes.clear();
        }
    }

    private static final class SimpleRecipe {
        private final SimpleIngredient[] ingredients;
        private final ItemStack result;

        public SimpleRecipe(ShapelessRecipe recipe) {
            this.result = recipe.getResult();

            List<SimpleIngredient> ingredients = new ArrayList<>();

            for (int i = 0; i < recipe.getIngredientList().size(); i++) {
                ItemStack item = recipe.getIngredientList().get(i);
                RecipeChoice rChoice = null;

                try {
                    rChoice = recipe.getChoiceList().get(i);
                } catch (NoSuchMethodError ignore) {    // Method missing in Spigot 1.12.2
                }

                processIngredient(ingredients, item, rChoice);
            }

            this.ingredients = ingredients.toArray(new SimpleIngredient[0]);
        }

        public SimpleRecipe(ShapedRecipe recipe) {
            this.result = recipe.getResult();

            List<SimpleIngredient> ingredients = new ArrayList<>();

            for (Map.Entry<Character, ItemStack> entry : recipe.getIngredientMap().entrySet()) {
                ItemStack item = entry.getValue();
                RecipeChoice rChoice = null;

                try {
                    rChoice = recipe.getChoiceMap().get(entry.getKey());
                } catch (NoSuchMethodError ignore) {    // Method missing in Spigot 1.12.2
                }

                if (item == null) {
                    continue;
                }

                processIngredient(ingredients, item, rChoice);
            }

            this.ingredients = ingredients.toArray(new SimpleIngredient[0]);
        }

        private void processIngredient(List<SimpleIngredient> ingredients, ItemStack item, RecipeChoice rChoice) {
            List<Material> alternativeTypes = new LinkedList<>();

            if (rChoice instanceof RecipeChoice.MaterialChoice) {
                for (Material possType : ((RecipeChoice.MaterialChoice) rChoice).getChoices()) {
                    if (item.getType() != possType) {
                        alternativeTypes.add(possType);
                    }
                }
            }

            SimpleIngredient simpleIngredient = new SimpleIngredient(item, alternativeTypes);

            // Search for existing ingredients
            for (SimpleIngredient ingredient : ingredients) {
                if (ingredient.isSimilar(simpleIngredient)) {
                    ingredient.addAdditionalAmount(item.getAmount());
                    simpleIngredient = null;
                    break;
                }
            }

            // No existing ingredient found?
            if (simpleIngredient != null) {
                ingredients.add(simpleIngredient);
            }
        }

        private static class SimpleIngredient {
            private final ItemStack item;
            private final ItemStack[] alternativeTypes;

            /**
             * <b>Ignored by {@link #isSimilar(Object)}!</b><br>
             * This amount should be added to {@link #item} when crafting,
             * to consider the complete item costs
             */
            private int additionalAmount = 0;

            /**
             * @throws NullPointerException If any of the parameters is null
             */
            SimpleIngredient(ItemStack item, List<Material> alternativeTypes) {
                Objects.requireNonNull(item);
                Objects.requireNonNull(alternativeTypes);

                this.item = item;

                this.alternativeTypes = new ItemStack[alternativeTypes.size()];

                for (int i = 0; i < alternativeTypes.size(); i++) {
                    this.alternativeTypes[i] = this.item.clone();
                    this.alternativeTypes[i].setType(alternativeTypes.get(i));
                }
            }

            public int getAdditionalAmount() {
                return this.additionalAmount;
            }

            public void addAdditionalAmount(int amountToAdd) {
                this.additionalAmount += amountToAdd;
            }

            /**
             * Like {@link #equals(Object)} but ignores {@link #additionalAmount} and {@link ItemStack#getAmount()}
             *
             * @return If two {@link SimpleIngredient} objects are equal
             * while ignoring any item amounts, true otherwise false
             */
            public boolean isSimilar(Object o) {
                if (this == o) {
                    return true;
                }
                if (o == null || getClass() != o.getClass()) {
                    return false;
                }

                SimpleIngredient that = (SimpleIngredient) o;
                return this.item.isSimilar(that.item) && Arrays.equals(this.alternativeTypes, that.alternativeTypes);
            }
        }
    }
}
