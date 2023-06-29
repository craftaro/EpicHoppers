package com.craftaro.epichoppers.utils;

import com.craftaro.core.SongodaPlugin;
import com.craftaro.core.compatibility.ServerVersion;
import com.craftaro.core.utils.TextUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
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
        String name = getPlugin().getLocale()
                .getMessage("general.nametag.nameformat")
                .processPlaceholder("level", level)
                .getMessage();


        return TextUtils.formatText(name);
    }

    public static void doParticles(Entity entity, Location location) {
        location.setX(location.getX() + .5);
        location.setY(location.getY() + .5);
        location.setZ(location.getZ() + .5);
        entity.getWorld().spawnParticle(org.bukkit.Particle.valueOf(getPlugin().getConfig().getString("Main.Upgrade Particle Type")), location, 200, .5, .5, .5);
    }

    /**
     * @deprecated The class needs refactoring to not even need the plugin.
     * This is just a temporary workaround to get a Minecraft 1.20-beta build ready
     */
    @Deprecated
    private static SongodaPlugin getPlugin() {
        return (SongodaPlugin) Bukkit.getPluginManager().getPlugin("EpicHoppers");
    }
}
