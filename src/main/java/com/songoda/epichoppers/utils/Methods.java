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

import java.text.DecimalFormat;
import java.util.concurrent.TimeUnit;

/**
 * Created by songoda on 2/24/2017.
 */
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
        if (inventory.firstEmpty() != -1) return true;

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
        if (inventory.firstEmpty() != inventory.getSize() - 1) return true;

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
            if (stack == null || stack.getAmount() == 0)
                return true;
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
        EpicHoppers instance = EpicHoppers.getInstance();
        String name = instance.getLocale().getMessage("general.nametag.nameformat")
                .processPlaceholder("level", level).getMessage();


        return TextUtils.formatText(name);
    }

    public static void doParticles(Entity entity, Location location) {
        EpicHoppers instance = EpicHoppers.getInstance();
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
        if (location == null || location.getWorld() == null)
            return "";
        String w = location.getWorld().getName();
        double x = location.getX();
        double y = location.getY();
        double z = location.getZ();
        String str = w + ":" + x + ":" + y + ":" + z;
        str = str.replace(".0", "").replace(".", "/");
        return str;
    }

    /**
     * Makes the specified Unix Epoch time human readable as per the format settings in the Arconix config.
     *
     * @param time The time to convert.
     * @return A human readable string representing to specified time.
     */
    public static String makeReadable(Long time) {
        if (time == null)
            return "";

        StringBuilder sb = new StringBuilder();

        long days = TimeUnit.MILLISECONDS.toDays(time);
        long hours = TimeUnit.MILLISECONDS.toHours(time) - TimeUnit.DAYS.toHours(TimeUnit.MILLISECONDS.toDays(time));
        long minutes = TimeUnit.MILLISECONDS.toMinutes(time) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(time));
        long seconds = TimeUnit.MILLISECONDS.toSeconds(time) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(time));

        if (days != 0L)
            sb.append(" ").append(days).append("d");
        if (hours != 0L)
            sb.append(" ").append(hours).append("h");
        if (minutes != 0L)
            sb.append(" ").append(minutes).append("m");
        if (seconds != 0L)
            sb.append(" ").append(seconds).append("s");
        return sb.toString().trim();
    }

    public static long parseTime(String input) {
        long result = 0;
        StringBuilder number = new StringBuilder();
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (Character.isDigit(c)) {
                number.append(c);
            } else if (Character.isLetter(c) && (number.length() > 0)) {
                result += convert(Integer.parseInt(number.toString()), c);
                number = new StringBuilder();
            }
        }
        return result;
    }

    private static long convert(long value, char unit) {
        switch (unit) {
            case 'd':
                return value * 1000 * 60 * 60 * 24;
            case 'h':
                return value * 1000 * 60 * 60;
            case 'm':
                return value * 1000 * 60;
            case 's':
                return value * 1000;
            default:
                return 0;
        }
    }

    /**
     * Formats the specified double into the Economy format specified in the Arconix config.
     *
     * @param amt The double to format.
     * @return The economy formatted double.
     */
    public static String formatEconomy(double amt) {
        DecimalFormat formatter = new DecimalFormat(amt == Math.ceil(amt) ? "#,###" : "#,###.00");
        return formatter.format(amt);
    }

    public static boolean isInt(String number) {
        if (number == null || number.equals(""))
            return false;
        try {
            Integer.parseInt(number);
        } catch (NumberFormatException e) {
            return false;
        }
        return true;
    }
}
