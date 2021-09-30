package com.songoda.epichoppers.utils;

import com.songoda.core.compatibility.CompatibleMaterial;
import com.songoda.core.compatibility.ServerVersion;
import com.songoda.core.utils.BlockUtils;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Chest;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

/**
 * Persistent storage intended for streamlining read/write for storage
 * containers in large batches
 */
public class StorageContainerCache {

    private static final Map<Block, Cache> inventoryCache = new HashMap<>();

    // need to get the topmost inventory for a double chest, and save as that block
    public static Cache getCachedInventory(Block b) {
        Cache cache = inventoryCache.get(b);
        if (cache == null) {
            Material type = b.getType();
            if (type == Material.CHEST || type == Material.TRAPPED_CHEST) {
                Block b2 = findAdjacentDoubleChest(b);
                //System.out.println("Adjacent to " + b + " = " + b2);
                if (b2 != null && (cache = inventoryCache.get(b2)) != null) {
                    return cache;
                }
            }
            BlockState blockState = b.getState();
            if (blockState instanceof InventoryHolder) {
                //System.out.println("Load " + b.getLocation());
                inventoryCache.put(b, cache = new Cache(b, ((InventoryHolder) blockState).getInventory().getContents()));
            }
        }
        return cache;
    }

    /**
     * Look for a double chest adjacent to a chest
     *
     * @param block
     * @return
     */
    public static Block findAdjacentDoubleChest(Block block) {
        if (ServerVersion.isServerVersionAtLeast(ServerVersion.V1_13)) {
            final BlockData d = block.getBlockData();
            if (d instanceof Chest) {
                final Chest c = (Chest) d;
                if (c.getType() != Chest.Type.SINGLE) {
                    // this is a double chest - check the other chest for registration data
                    Block other = null;
                    switch (c.getFacing()) {
                        case SOUTH:
                            other = block.getRelative(c.getType() != Chest.Type.RIGHT ? BlockFace.WEST : BlockFace.EAST);
                            break;
                        case NORTH:
                            other = block.getRelative(c.getType() != Chest.Type.RIGHT ? BlockFace.EAST : BlockFace.WEST);
                            break;
                        case EAST:
                            other = block.getRelative(c.getType() != Chest.Type.RIGHT ? BlockFace.SOUTH : BlockFace.NORTH);
                            break;
                        case WEST:
                            other = block.getRelative(c.getType() != Chest.Type.RIGHT ? BlockFace.NORTH : BlockFace.SOUTH);
                            break;
                        default:
                            break;
                    }
                    // double-check
                    if (other != null && other.getType() == block.getType()) {
                        return other;
                    }
                }
            }
        } else {
            // legacy check
            Material material = block.getType();
            BlockFace[] faces = new BlockFace[] {BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST};
            for (BlockFace face : faces) {
                Block adjacentBlock = block.getRelative(face);

                if (adjacentBlock.getType() == material) {
                    return adjacentBlock;
                }
            }
        }
        return null;
    }

    public static void update() {
        inventoryCache.entrySet().stream()
                .filter(e -> e.getValue().dirty)
                .forEach(e -> {
                    //System.out.println("Update " + e.getKey().getLocation());
                    // setContents makes a copy of every item whether it's needed or not
                    //((InventoryHolder) e.getKey().getState()).getInventory().setContents(e.getValue().cachedInventory);
                    // so let's only update what needs to be updated.
                    final ItemStack[] cachedInventory = e.getValue().cachedInventory;
                    final boolean[] cacheChanged = e.getValue().cacheChanged;
                    Inventory inventory = ((InventoryHolder) e.getKey().getState()).getInventory();//.setContents();
                    for (int i = 0; i < cachedInventory.length; i++) {
                        if (cacheChanged[i]) {
                            inventory.setItem(i, cachedInventory[i]);
                        }
                    }
                    BlockUtils.updateAdjacentComparators(e.getKey().getLocation());
                });
        inventoryCache.clear();
    }

    public static class Cache {

        public final Material type;
        public final Block block;
        public ItemStack[] cachedInventory;
        public boolean[] cacheChanged;
        public int[] cacheAdded;
        public boolean dirty;

        public Cache(Material type, ItemStack[] cachedInventory) {
            this.block = null;
            this.type = type;
            this.cachedInventory = cachedInventory;
            this.cacheChanged = new boolean[cachedInventory.length];
            this.cacheAdded = new int[cachedInventory.length];
        }

        public Cache(Block b, ItemStack[] cachedInventory) {
            this.block = b;
            this.type = b.getType();
            this.cachedInventory = cachedInventory;
            this.cacheChanged = new boolean[cachedInventory.length];
            this.cacheAdded = new int[cachedInventory.length];
        }

        public void setDirty(boolean dirty) {
            this.dirty = dirty;
        }

        public boolean isDirty() {
            return this.dirty;
        }

        public void setContents(ItemStack[] items) {
            if (cachedInventory == null || items.length == cachedInventory.length) {
                cachedInventory = items;
                for (int i = 0; i < cachedInventory.length; i++) {
                    cacheChanged[i] = true;
                }
                dirty = true;
            }
        }

        public void setItem(int item, ItemStack itemStack) {
            if (cachedInventory != null) {
                cachedInventory[item] = itemStack;
                cacheChanged[item] = true;
                dirty = true;
            }
        }

        public void removeItem(int item) {
            if (cachedInventory != null) {
                cachedInventory[item] = null;
                cacheChanged[item] = true;
                dirty = true;
            }
        }

        public void removeItems(ItemStack item) {
            if (cachedInventory != null && item != null) {
                int toRemove = item.getAmount();
                for (int i = 0; toRemove > 0 && i < cachedInventory.length; i++) {
                    final ItemStack cacheItem = cachedInventory[i];
                    if (cacheItem != null && cacheItem.getAmount() != 0 && item.isSimilar(cacheItem)) {
                        int have = cacheItem.getAmount();
                        if (have > toRemove) {
                            cachedInventory[i].setAmount(have - toRemove);
                            cacheChanged[i] = true;
                            toRemove = 0;
                        } else {
                            cachedInventory[i] = null;
                            cacheChanged[i] = true;
                            toRemove -= have;
                        }
                    }
                }
                dirty = dirty | (toRemove != item.getAmount());
            }
        }

        /**
         * Add a number of items to this container's inventory later.
         *
         * @param item        item to add
         * @param amountToAdd how many of this item to attempt to add
         * @return how many items were added
         */
        public int addAny(ItemStack item, int amountToAdd) {

            // Don't transfer shulker boxes into other shulker boxes, that's a bad idea.
            if (type.name().contains("SHULKER_BOX") && item.getType().name().contains("SHULKER_BOX"))
                return 0;

            int totalAdded = 0;
            if (cachedInventory != null && item != null) {
                final int maxStack = item.getMaxStackSize();
                for (int i = 0; amountToAdd > 0 && i < cachedInventory.length; i++) {
                    final ItemStack cacheItem = cachedInventory[i];
                    if (cacheItem == null || cacheItem.getAmount() == 0) {
                        // free slot!
                        int toAdd = Math.min(maxStack, amountToAdd);
                        cachedInventory[i] = item.clone();
                        cachedInventory[i].setAmount(toAdd);
                        cacheChanged[i] = true;
                        cacheAdded[i] = toAdd;
                        totalAdded += toAdd;
                        amountToAdd -= toAdd;
                    } else if (maxStack > cacheItem.getAmount() && item.isSimilar(cacheItem)) {
                        // free space!
                        int toAdd = Math.min(maxStack - cacheItem.getAmount(), amountToAdd);
                        cachedInventory[i].setAmount(toAdd + cacheItem.getAmount());
                        cacheChanged[i] = true;
                        cacheAdded[i] += toAdd;
                        totalAdded += toAdd;
                        amountToAdd -= toAdd;
                    }
                }
                if (totalAdded != 0) {
                    dirty = true;
                }
            }
            return totalAdded;
        }

        /**
         * Add an item to this container's inventory later.
         *
         * @param item item to add
         * @return true if the item was added
         */
        public boolean addItem(ItemStack item) {
            if (cachedInventory == null || item == null || item.getAmount() <= 0)
                return false;

            // Don't transfer shulker boxes into other shulker boxes, that's a bad idea.
            if (type.name().contains("SHULKER_BOX") && item.getType().name().contains("SHULKER_BOX"))
                return false;

            // grab the amount to move and the max item stack size
            int toAdd = item.getAmount();
            final int maxStack = item.getMaxStackSize();
            boolean[] check = null;

            // some destination containers have special conditions
            switch (type.name()) {
                case "BREWING_STAND": {

                    // first compile a list of what slots to check
                    check = new boolean[5];
                    String typeStr = item.getType().name().toUpperCase();
                    if (typeStr.contains("POTION") || typeStr.contains("BOTTLE")) {
                        // potion bottles are the first three slots
                        check[0] = check[1] = check[2] = true;
                    }
                    // fuel in 5th position, input in 4th
                    if (item.getType() == Material.BLAZE_POWDER)
                        check[4] = true;
                    else if (CompatibleMaterial.getMaterial(item).isBrewingStandIngredient())
                        check[3] = true;

                    break;
                }
                case "SMOKER":
                case "BLAST_FURNACE":
                case "BURNING_FURNACE":
                case "FURNACE": {

                    check = new boolean[3];

                    boolean isFuel = !item.getType().name().contains("LOG") && CompatibleMaterial.getMaterial(item.getType()).isFuel();
                    // fuel is 2nd slot, input is first
                    if (isFuel)
                        check[1] = true;
                    else
                        check[0] = true;

                    break;
                }
                default:
                    break;
            }

            // we can reduce calls to ItemStack.isSimilar() by caching what cells to look at
            if (check == null) {
                check = new boolean[cachedInventory.length];
                for (int i = 0; toAdd > 0 && i < check.length; i++)
                    check[i] = true;
            }

            // first verify that we can add this item
            for (int i = 0; toAdd > 0 && i < cachedInventory.length; i++) {
                if (check[i]) {
                    final ItemStack cacheItem = cachedInventory[i];
                    if (cacheItem == null || cacheItem.getAmount() == 0) {
                        // free slot!
                        toAdd -= Math.min(maxStack, toAdd);
                        check[i] = true;
                    } else if (maxStack > cacheItem.getAmount() && item.isSimilar(cacheItem)) {
                        // free space!
                        toAdd -= Math.min(maxStack - cacheItem.getAmount(), toAdd);
                        check[i] = true;
                    } else
                        check[i] = false;
                }
            }
            if (toAdd <= 0) {
                // all good to add!
                toAdd = item.getAmount();
                for (int i = 0; toAdd > 0 && i < cachedInventory.length; i++) {
                    if (!check[i])
                        continue;
                    final ItemStack cacheItem = cachedInventory[i];
                    if (cacheItem == null || cacheItem.getAmount() == 0) {
                        // free slot!
                        int adding = Math.min(maxStack, toAdd);
                        cachedInventory[i] = item.clone();
                        cachedInventory[i].setAmount(adding);
                        cacheChanged[i] = true;
                        cacheAdded[i] = adding;
                        toAdd -= adding;
                    } else if (maxStack > cacheItem.getAmount()) {
                        // free space!
                        // (no need to check item.isSimilar(cacheItem), since we have that cached in check[])
                        int adding = Math.min(maxStack - cacheItem.getAmount(), toAdd);
                        cachedInventory[i].setAmount(adding + cacheItem.getAmount());
                        cacheChanged[i] = true;
                        cacheAdded[i] += adding;
                        toAdd -= adding;
                    }
                }
                dirty = true;
                return true;
            }
            return false;
        }
    }
}
