package com.songoda.epichoppers.hopper.levels.modules;

import com.songoda.core.compatibility.CompatibleMaterial;
import com.songoda.epichoppers.EpicHoppers;
import com.songoda.epichoppers.gui.GUICrafting;
import com.songoda.epichoppers.hopper.Hopper;
import com.songoda.epichoppers.settings.Settings;
import com.songoda.epichoppers.utils.Methods;
import com.songoda.epichoppers.utils.StorageContainerCache;
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

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

public class ModuleAutoCrafting extends Module {

    private static final Map<ItemStack, Recipes> cachedRecipes = new ConcurrentHashMap<>();
    private static final Map<Hopper, ItemStack> cachedCrafting = new ConcurrentHashMap<>();
    static final ItemStack noCraft = new ItemStack(Material.AIR);
    boolean crafterEjection;

    public ModuleAutoCrafting(EpicHoppers plugin) {
        super(plugin);
        crafterEjection = Settings.AUTOCRAFT_JAM_EJECT.getBoolean();
    }

    @Override
    public String getName() {
        return "AutoCrafting";
    }

    @Override
    public void run(Hopper hopper, StorageContainerCache.Cache hopperCache) {
        final ItemStack toCraft;
        if (hopper == null || (toCraft = getAutoCrafting(hopper)) == null || toCraft.getType() == Material.AIR)
            return;

        // jam check: is this hopper gummed up?
        if (crafterEjection) {
            // TODO: Recode. Why specifically eject ingredients (allMaterials).
            //       And why not eject when we checked crafting is possible
            final List<Material> allMaterials = getRecipes(toCraft).getAllMaterials();
            if (Stream.of(hopperCache.cachedInventory)
                    .allMatch(item -> item != null && allMaterials.stream().anyMatch(mat -> mat == item.getType()))) {
                // Crafter can't function if there's nowhere to put the output
                // ¯\_(ツ)_/¯
                // forcibly open the last slot
                ItemStack last = hopperCache.cachedInventory[4];
                hopperCache.setItem(4, null);
                // and yeet into space!
                hopper.getWorld().dropItemNaturally(hopper.getLocation(), last);
            }
        }

        synchronized (hopperCache) {    //TODO: Check if this is required
            ItemStack[] items = hopperCache.cachedInventory;

            for (SimpleRecipe recipe : getRecipes(toCraft).recipes) {
                // key=indexForItemsArray, value=amountAfterCrafting
                Map<Integer, Integer> slotsToAlter = new HashMap<>();

                for (SimpleRecipe.SimpleIngredient ingredient : recipe.ingredients) {
                    int amount = ingredient.item.getAmount() + ingredient.getAdditionalAmount();

                    for (int i = 0; i < items.length; i++) {
                        ItemStack item = items[i];

                        if (item == null) continue;
                        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) continue;

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
                            if (!sameMaterial) continue;
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
                    if (amount != 0) continue;

                    for (Map.Entry<Integer, Integer> entry : slotsToAlter.entrySet()) {
                        if (entry.getValue() <= 0) {
                            items[entry.getKey()] = null;
                        } else {
                            items[entry.getKey()].setAmount(entry.getValue());
                        }
                    }

                    // Add the resulting item into the inventory - Just making sure there actually is enough space
                    boolean success = false;
                    for (int i = 0; i < items.length; i++) {
                        if (items[i] == null ||
                                (items[i].isSimilar(recipe.result)
                                        && items[i].getAmount() + recipe.result.getAmount() <= items[i].getMaxStackSize())) {
                            if (items[i] == null) {
                                items[i] = recipe.result.clone();
                            } else {
                                items[i].setAmount(items[i].getAmount() + recipe.result.getAmount());
                            }

                            success = true;
                            break;
                        }
                    }

                    if (success) {
                        hopperCache.setContents(items);
                    }
                }
            }
        }
    }

    @Override
    public ItemStack getGUIButton(Hopper hopper) {
        ItemStack crafting = CompatibleMaterial.CRAFTING_TABLE.getItem();
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
        EpicHoppers.getInstance().getGuiManager().showGUI(player, new GUICrafting(this, hopper, player));
    }

    @Override
    public List<Material> getBlockedItems(Hopper hopper) {
        ItemStack itemStack = getAutoCrafting(hopper);
        if (itemStack != null && itemStack.getType() != Material.AIR) {
            return getRecipes(itemStack).getAllMaterials();
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

    private Recipes getRecipes(ItemStack toCraft) {
        Recipes recipes = cachedRecipes.get(toCraft);
        if (Settings.AUTOCRAFT_BLACKLIST.getStringList().stream()
                .anyMatch(r -> r.equalsIgnoreCase(toCraft.getType().name())))
            return new Recipes();
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
                        if (Methods.isSimilarMaterial(stack, toCraft))
                            recipes.addRecipe(recipe);
                    } catch (Throwable ignored) {
                    }
                }
            }

            cachedRecipes.put(toCraft, recipes);
        }
        return recipes;
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

    final static class Recipes {

        // we don't actually care about the shape, just the materials used
        private final List<SimpleRecipe> recipes = new ArrayList<>();
        private final List<Material> allTypes = new ArrayList<>();

        public Recipes() {
        }

        public Recipes(Collection<Recipe> recipes) {
            addRecipes(recipes);
        }

        public List<SimpleRecipe> getRecipes() {
            return Collections.unmodifiableList(recipes);
        }

        public List<Material> getAllMaterials() {
            return Collections.unmodifiableList(allTypes);
        }

        public void addRecipe(Recipe recipe) {
            SimpleRecipe simpleRecipe = null;

            if (recipe instanceof ShapelessRecipe) {
                simpleRecipe = new SimpleRecipe((ShapelessRecipe) recipe);
            } else if (recipe instanceof ShapedRecipe) {
                simpleRecipe = new SimpleRecipe((ShapedRecipe) recipe);
            }

            // Skip unsupported recipe type
            if (simpleRecipe == null) return;

            this.recipes.add(simpleRecipe);

            // TODO: Find out what allTypes is actually for o.0
            // Also keep a tally of what materials are possible for this craftable
            for (SimpleRecipe.SimpleIngredient ingredient : simpleRecipe.ingredients) {
                if (!allTypes.contains(ingredient.item.getType())) {
                    allTypes.add(ingredient.item.getType());
                }

                for (ItemStack material : ingredient.alternativeTypes) {
                    if (!allTypes.contains(material.getType())) {
                        allTypes.add(material.getType());
                    }
                }
            }
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

    final static class SimpleRecipe {
        final SimpleIngredient[] ingredients;
        final ItemStack result;

        SimpleRecipe(ShapelessRecipe recipe) {
            this.result = recipe.getResult();

            List<SimpleIngredient> ingredients = new ArrayList<>();

            for (int i = 0; i < recipe.getIngredientList().size(); i++) {
                ItemStack item = recipe.getIngredientList().get(i);
                RecipeChoice rChoice = recipe.getChoiceList().get(i);

                processIngredient(ingredients, item, rChoice);
            }

            this.ingredients = ingredients.toArray(new SimpleIngredient[0]);
        }

        SimpleRecipe(ShapedRecipe recipe) {
            this.result = recipe.getResult();

            List<SimpleIngredient> ingredients = new ArrayList<>();

            for (Map.Entry<Character, ItemStack> entry : recipe.getIngredientMap().entrySet()) {
                ItemStack item = entry.getValue();
                RecipeChoice rChoice = recipe.getChoiceMap().get(entry.getKey());

                if (item == null) continue;

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

        static class SimpleIngredient {
            final ItemStack item;
            final ItemStack[] alternativeTypes;

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
                return additionalAmount;
            }

            public void addAdditionalAmount(int amountToAdd) {
                additionalAmount += amountToAdd;
            }

            /**
             * Like {@link #equals(Object)} but ignores {@link #additionalAmount} and {@link ItemStack#getAmount()}
             *
             * @return If two {@link SimpleIngredient} objects are equal
             * while ignoring any item amounts, true otherwise false
             */
            public boolean isSimilar(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;

                SimpleIngredient that = (SimpleIngredient) o;
                return item.isSimilar(that.item) &&
                        Arrays.equals(alternativeTypes, that.alternativeTypes);
            }
        }
    }
}
