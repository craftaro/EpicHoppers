package com.songoda.epichoppers.utils;

import com.songoda.arconix.plugin.Arconix;
import com.songoda.epichoppers.EpicHoppersPlugin;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Created by songoda on 2/24/2017.
 */
public class Methods {

    public static boolean isSync(Player p) {
        try {
            if (p.getItemInHand().hasItemMeta()
                    && p.getItemInHand().getType() != Material.AIR
                    && p.getItemInHand().getType() != Material.ENCHANTED_BOOK
                    && p.getItemInHand().getItemMeta().hasLore()) {
                for (String str : p.getItemInHand().getItemMeta().getLore()) {
                    if (str.equals(Arconix.pl().getApi().format().formatText("&7Sync Touch")) || str.equals(Arconix.pl().getApi().format().formatText("&aSync Touch"))) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            Debugger.runReport(e);
        }
        return false;
    }

    public static ItemStack getGlass() {
        try {
            EpicHoppersPlugin instance = EpicHoppersPlugin.getInstance();
            return Arconix.pl().getApi().getGUI().getGlass(instance.getConfig().getBoolean("Interfaces.Replace Glass Type 1 With Rainbow Glass"), instance.getConfig().getInt("Interfaces.Glass Type 1"));
        } catch (Exception e) {
            Debugger.runReport(e);
        }
        return null;
    }

    public static ItemStack getBackgroundGlass(boolean type) {
        try {
            EpicHoppersPlugin instance = EpicHoppersPlugin.getInstance();
            if (type)
                return Arconix.pl().getApi().getGUI().getGlass(false, instance.getConfig().getInt("Interfaces.Glass Type 2"));
            else
                return Arconix.pl().getApi().getGUI().getGlass(false, instance.getConfig().getInt("Interfaces.Glass Type 3"));
        } catch (Exception e) {
            Debugger.runReport(e);
        }
        return null;
    }

    public static String formatName(int level, boolean full) {
        try {
            EpicHoppersPlugin instance = EpicHoppersPlugin.getInstance();
            String name = instance.getLocale().getMessage("general.nametag.nameformat", level);

            String info = "";
            if (full) {
                info += Arconix.pl().getApi().format().convertToInvisibleString(level + ":");
            }

            return info + Arconix.pl().getApi().format().formatText(name);
        } catch (Exception e) {
            Debugger.runReport(e);
        }
        return null;
    }

    public static void doParticles(Player p, Location location) {
        try {
            EpicHoppersPlugin instance = EpicHoppersPlugin.getInstance();
            location.setX(location.getX() + .5);
            location.setY(location.getY() + .5);
            location.setZ(location.getZ() + .5);
                p.getWorld().spawnParticle(org.bukkit.Particle.valueOf(instance.getConfig().getString("Main.Upgrade Particle Type")), location, 200, .5, .5, .5);
        } catch (Exception e) {
            Debugger.runReport(e);
        }
    }
}
