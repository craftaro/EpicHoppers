package com.songoda.epichoppers.hopper.levels.modules;

import com.songoda.core.SongodaPlugin;
import com.songoda.core.gui.GuiManager;
import com.songoda.third_party.com.cryptomorin.xseries.XMaterial;
import com.songoda.core.utils.TextUtils;
import com.songoda.epichoppers.EpicHoppers;
import com.songoda.epichoppers.hopper.Hopper;
import com.songoda.epichoppers.hopper.HopperImpl;
import com.songoda.epichoppers.hopper.ItemType;
import com.songoda.epichoppers.settings.Settings;
import com.songoda.epichoppers.utils.Methods;
import com.songoda.epichoppers.gui.GUICrafting;
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
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
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

    // Cache for last output slot to optimize stacking (lag-free with timestamp)
    // Key: Hopper, Value: {slot index, material type, timestamp}
    private static final Map<Hopper, LastOutputSlot> LAST_OUTPUT_SLOT = new ConcurrentHashMap<>();
    private static final long OUTPUT_SLOT_CACHE_TTL = 60000; // 60 seconds timeout

    private final boolean crafterEjection;

    // Helper class to cache last output slot with timestamp
    private static class LastOutputSlot {
        final int slot;
        final Material material;
        final long timestamp;

        LastOutputSlot(int slot, Material material) {
            this.slot = slot;
            this.material = material;
            this.timestamp = System.currentTimeMillis();
        }

        boolean isValid(Material currentMaterial) {
            // Cache is valid if material matches and not too old
            return this.material == currentMaterial &&
                   (System.currentTimeMillis() - this.timestamp) < OUTPUT_SLOT_CACHE_TTL;
        }
    }

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
                if (!freeSlotAfterRemovingIngredients) {
                    // Crafter can't function if there's nowhere to put the output
                    // ¯\_(ツ)_/¯

                    // First try to find a slot that's NOT part of the ingredients
                    for (int i = 0; i < items.length; i++) {
                        if (!slotsToAlter.containsKey(i)) {
                            // and yeet into space!
                            hopper.getLocation().getWorld().dropItemNaturally(hopper.getLocation(), items[i]);
                            items[i] = null;

                            freeSlotAfterRemovingIngredients = true;
                            break;
                        }
                    }

                    // If all slots are ingredients, eject the last slot forcefully to free up space
                    // This is necessary when the hopper is completely full of crafting ingredients
                    if (!freeSlotAfterRemovingIngredients) {
                        int slot = items.length - 1;   // Last slot

                        // Drop only what won't be consumed by crafting
                        Integer amountAfterCraft = slotsToAlter.get(slot);
                        if (amountAfterCraft != null && amountAfterCraft > 0) {
                            // Some items will remain after crafting, drop those
                            ItemStack toDrop = items[slot].clone();
                            toDrop.setAmount(amountAfterCraft);
                            hopper.getLocation().getWorld().dropItemNaturally(hopper.getLocation(), toDrop);

                            // Set the slot to exactly what will be consumed
                            items[slot].setAmount(items[slot].getAmount() - amountAfterCraft);
                            slotsToAlter.put(slot, 0);
                        } else {
                            // All items in this slot will be consumed, but we still drop one to make space
                            ItemStack toDrop = items[slot].clone();
                            toDrop.setAmount(1);
                            hopper.getLocation().getWorld().dropItemNaturally(hopper.getLocation(), toDrop);
                            items[slot].setAmount(items[slot].getAmount() - 1);

                            if (items[slot].getAmount() == 0) {
                                items[slot] = null;
                            }
                        }

                        freeSlotAfterRemovingIngredients = true;
                    }
                }

                if (freeSlotAfterRemovingIngredients) {
                    // Remove ingredients
                    for (Map.Entry<Integer, Integer> entry : slotsToAlter.entrySet()) {
                        if (entry.getValue() <= 0) {
                            items[entry.getKey()] = null;
                        } else {
                            items[entry.getKey()].setAmount(entry.getValue());
                        }
                    }

                    // Add the resulting item into the inventory - Just making sure there actually is enough space
                    boolean outputAdded = false;
                    int outputSlot = -1;

                    // OPTIMIZATION: Check cached slot first (lag-free with timestamp)
                    LastOutputSlot cachedSlot = LAST_OUTPUT_SLOT.get(hopper);
                    if (cachedSlot != null && cachedSlot.isValid(recipe.result.getType())) {
                        int i = cachedSlot.slot;
                        if (i < items.length && items[i] != null &&
                            items[i].isSimilar(recipe.result) &&
                            items[i].getAmount() + recipe.result.getAmount() <= items[i].getMaxStackSize()) {
                            // Cached slot is still valid! Use it directly (no iteration needed)
                            items[i].setAmount(items[i].getAmount() + recipe.result.getAmount());
                            outputAdded = true;
                            outputSlot = i;
                        }
                    }

                    // If cached slot didn't work, do normal search
                    if (!outputAdded) {
                        // First pass: Look for existing stacks of the same item
                        for (int i = 0; i < items.length; i++) {
                            if (items[i] != null &&
                                items[i].isSimilar(recipe.result) &&
                                items[i].getAmount() + recipe.result.getAmount() <= items[i].getMaxStackSize()) {
                                items[i].setAmount(items[i].getAmount() + recipe.result.getAmount());
                                outputAdded = true;
                                outputSlot = i;
                                break;
                            }
                        }

                        // Second pass: Look for empty slots
                        if (!outputAdded) {
                            for (int i = 0; i < items.length; i++) {
                                if (items[i] == null) {
                                    items[i] = recipe.result.clone();
                                    outputAdded = true;
                                    outputSlot = i;
                                    break;
                                }
                            }
                        }
                    }

                    // Update cache with the slot we used
                    if (outputAdded && outputSlot >= 0) {
                        LAST_OUTPUT_SLOT.put(hopper, new LastOutputSlot(outputSlot, recipe.result.getType()));
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
                .toText());
        List<String> loreCrafting = TextUtils.formatLore(
                this.plugin.getLocale().getMessage("interface.hopper.craftinglore")
                        .processPlaceholder("enabled", getAutoCrafting(hopper) != null && 
                                getAutoCrafting(hopper).getType() != Material.AIR ?
                                this.plugin.getLocale().getMessage("general.word.enabled").toText() :
                                this.plugin.getLocale().getMessage("general.word.disabled").toText())
                        .toText()
        );
        craftingMeta.setLore(loreCrafting);
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
                .processPlaceholder("enabled", this.plugin.getLocale().getMessage("general.word.enabled").toText())
                .toText();
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

    /**
     * Load autocrafter item from database
     * Uses complete ItemStack serialization to preserve custom items (name, lore, enchants, NBT)
     */
    private ItemStack loadAutoCraftingFromDB(HopperImpl hopper) {
        try (Connection connection = EpicHoppers.getPlugin(EpicHoppers.class).getDataManager().getDatabaseConnector().getConnection()) {
            String tablePrefix = EpicHoppers.getPlugin(EpicHoppers.class).getDataManager().getTablePrefix();
            String selectItem = "SELECT item FROM " + tablePrefix + "items WHERE hopper_id = ? AND item_type = ? LIMIT 1";

            try (PreparedStatement statement = connection.prepareStatement(selectItem)) {
                statement.setInt(1, hopper.getId());
                statement.setString(2, ItemType.AUTOCRAFTER.name());

                try (ResultSet result = statement.executeQuery()) {
                    if (result.next()) {
                        String itemData = result.getString("item");
                        if (itemData != null) {
                            // Deserialize complete ItemStack from Base64
                            try (BukkitObjectInputStream stream = new BukkitObjectInputStream(
                                    new ByteArrayInputStream(Base64.getDecoder().decode(itemData)))) {
                                return (ItemStack) stream.readObject();
                            } catch (ClassNotFoundException | IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return null;
    }

    /**
     * Save autocrafter item to database
     * Uses complete ItemStack serialization to preserve custom items (name, lore, enchants, NBT)
     */
    private void saveAutoCraftingToDB(HopperImpl hopper, ItemStack item) {
        try (Connection connection = EpicHoppers.getPlugin(EpicHoppers.class).getDataManager().getDatabaseConnector().getConnection()) {
            String tablePrefix = EpicHoppers.getPlugin(EpicHoppers.class).getDataManager().getTablePrefix();

            // Delete existing autocrafter item
            String deleteItem = "DELETE FROM " + tablePrefix + "items WHERE hopper_id = ? AND item_type = ?";
            try (PreparedStatement statement = connection.prepareStatement(deleteItem)) {
                statement.setInt(1, hopper.getId());
                statement.setString(2, ItemType.AUTOCRAFTER.name());
                statement.executeUpdate();
            }

            // Insert new autocrafter item (if not null)
            if (item != null && item.getType() != Material.AIR) {
                String insertItem = "INSERT INTO " + tablePrefix + "items (hopper_id, item_type, item) VALUES (?, ?, ?)";
                try (PreparedStatement statement = connection.prepareStatement(insertItem)) {
                    statement.setInt(1, hopper.getId());
                    statement.setString(2, ItemType.AUTOCRAFTER.name());

                    // Serialize complete ItemStack to Base64
                    try (ByteArrayOutputStream stream = new ByteArrayOutputStream();
                         BukkitObjectOutputStream bukkitStream = new BukkitObjectOutputStream(stream)) {
                        bukkitStream.writeObject(item);
                        statement.setString(3, Base64.getEncoder().encodeToString(stream.toByteArray()));
                    } catch (IOException e) {
                        e.printStackTrace();
                        return;
                    }

                    statement.executeUpdate();
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public ItemStack getAutoCrafting(Hopper hopper) {
        // Check cache first
        if (CACHED_CRAFTING.containsKey(hopper)) {
            return CACHED_CRAFTING.get(hopper);
        }

        if (!(hopper instanceof HopperImpl)) {
            return null;
        }

        HopperImpl hopperImpl = (HopperImpl) hopper;

        // Try loading from database (new format with full ItemStack serialization)
        ItemStack fromDB = loadAutoCraftingFromDB(hopperImpl);
        if (fromDB != null) {
            CACHED_CRAFTING.put(hopper, fromDB);
            return fromDB;
        }

        // Fallback: load from config file (old format - migration path)
        Object autocrafting = getData(hopper, "autocrafting");
        if (autocrafting != null) {
            ItemStack toCraft = autocrafting instanceof ItemStack ? (ItemStack) autocrafting : decode((String) autocrafting);
            if (toCraft != null && toCraft.getType() != Material.AIR) {
                // MIGRATION: Found old format data, migrate it to database
                saveAutoCraftingToDB(hopperImpl, toCraft);
                // Clear from config to avoid confusion
                saveData(hopper, "autocrafting", null, null);
                CACHED_CRAFTING.put(hopper, toCraft);
                return toCraft;
            }
        }

        // No data found
        CACHED_CRAFTING.put(hopper, NO_CRAFT);
        return null;
    }

    public void setAutoCrafting(Hopper hopper, Player player, ItemStack autoCrafting) {
        if (!(hopper instanceof HopperImpl)) {
            return;
        }

        HopperImpl hopperImpl = (HopperImpl) hopper;

        // CRITICAL FIX: Save immediately to database to prevent data loss on server crash
        // Uses complete ItemStack serialization to preserve custom items (name, lore, enchants, NBT)
        saveAutoCraftingToDB(hopperImpl, autoCrafting);

        // Update cache
        CACHED_CRAFTING.put(hopper, autoCrafting == null ? NO_CRAFT : autoCrafting);

        if (autoCrafting == null) {
            return;
        }

        // Return excess items to player
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
