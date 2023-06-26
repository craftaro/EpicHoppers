package com.songoda.epichoppers.utils;

import com.songoda.core.compatibility.ServerVersion;
import com.songoda.core.utils.TextUtils;
import com.songoda.epichoppers.EpicHoppers;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class Methods {
    public static boolean isSimilarMaterial(ItemStack is1, ItemStack is2) {
        if (ServerVersion.isServerVersionAtLeast(ServerVersion.V1_13) ||
                is1.getDurability() == Short.MAX_VALUE || is2.getDurability() == Short.MAX_VALUE) {
            // Durability of Short.MAX_VALUE is used in recipes if the durability should be ignored
            return is1.getType() == is2.getType();
        } else {
            return is1.getType() == is2.getType() && (is1.getDurability() == -1 || is2.getDurability() == -1 || is1.getDurability() == is2.getDurability());
        }
    }

    public static boolean canMove(Inventory inventory, ItemStack item) {
        if (inventory.firstEmpty() != -1) {
            return true;
        }

        final ItemMeta itemMeta = item.getItemMeta();
        for (ItemStack stack : inventory) {
            final ItemMeta stackMeta;
            if (isSimilarMaterial(stack, item) && (stack.getAmount() + item.getAmount()) < stack.getMaxStackSize()
                    && ((itemMeta == null) == ((stackMeta = stack.getItemMeta()) == null))
                    && (itemMeta == null || Bukkit.getItemFactory().equals(itemMeta, stackMeta))) {
                return true;
            }
        }
        return false;
    }

    public static boolean canMoveReserved(Inventory inventory, ItemStack item) {
        if (inventory.firstEmpty() != inventory.getSize() - 1) {
            return true;
        }

        final ItemMeta itemMeta = item.getItemMeta();
        final ItemStack[] contents = inventory.getContents();
        for (int i = 0; i < 4; i++) {
            final ItemStack stack = contents[i];
            final ItemMeta stackMeta;
            if (isSimilarMaterial(stack, item) && (stack.getAmount() + item.getAmount()) < stack.getMaxStackSize()
                    && ((itemMeta == null) == ((stackMeta = stack.getItemMeta()) == null))
                    && (itemMeta == null || Bukkit.getItemFactory().equals(itemMeta, stackMeta))) {
                return true;
            }
        }
        return false;
    }

    public static boolean canMoveReserved(ItemStack[] contents, ItemStack item) {
        final ItemMeta itemMeta = item.getItemMeta();
        for (int i = 0; i < contents.length - 2; i++) {
            final ItemStack stack = contents[i];
            if (stack == null || stack.getAmount() == 0) {
                return true;
            }
            final ItemMeta stackMeta;
            if (isSimilarMaterial(stack, item) && (stack.getAmount() + item.getAmount()) < stack.getMaxStackSize()
                    && ((itemMeta == null) == ((stackMeta = stack.getItemMeta()) == null))
                    && (itemMeta == null || Bukkit.getItemFactory().equals(itemMeta, stackMeta))) {
                return true;
            }
        }
        return false;
    }

    public static String formatName(int level) {
        EpicHoppers instance = EpicHoppers.getPlugin(EpicHoppers.class);
        String name = instance.getLocale()
                .getMessage("general.nametag.nameformat")
                .processPlaceholder("level", level)
                .getMessage();


        return TextUtils.formatText(name);
    }

    public static void doParticles(Entity entity, Location location) {
        EpicHoppers instance = EpicHoppers.getPlugin(EpicHoppers.class);
        location.setX(location.getX() + .5);
        location.setY(location.getY() + .5);
        location.setZ(location.getZ() + .5);
        entity.getWorld().spawnParticle(org.bukkit.Particle.valueOf(instance.getConfig().getString("Main.Upgrade Particle Type")), location, 200, .5, .5, .5);
    }

    /**
     * Serializes the location specified.
     *
     * @param location The location that is to be saved.
     * @return The serialized data.
     */
    public static String serializeLocation(Location location) {
        if (location == null || location.getWorld() == null) {
            return "";
        }
        String w = location.getWorld().getName();
        double x = location.getX();
        double y = location.getY();
        double z = location.getZ();
        String str = w + ":" + x + ":" + y + ":" + z;
        str = str.replace(".0", "").replace(".", "/");
        return str;
    }
}
