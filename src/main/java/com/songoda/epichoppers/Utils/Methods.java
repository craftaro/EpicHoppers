package com.songoda.epichoppers.Utils;

import com.songoda.arconix.plugin.Arconix;
import com.songoda.epichoppers.EpicHoppers;
import com.songoda.epichoppers.Hopper.Hopper;
import net.minecraft.server.v1_7_R4.AxisAlignedBB;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_7_R4.CraftWorld;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Created by songoda on 2/24/2017.
 */
public class Methods {

    public static boolean isSync(Player p) {
        try {
            if (p.getItemInHand().hasItemMeta()
                    && p.getItemInHand().getType() != Material.AIR
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
            EpicHoppers instance = EpicHoppers.getInstance();
            return Arconix.pl().getApi().getGUI().getGlass(instance.getConfig().getBoolean("Interfaces.Replace Glass Type 1 With Rainbow Glass"), instance.getConfig().getInt("Interfaces.Glass Type 1"));
        } catch (Exception e) {
            Debugger.runReport(e);
        }
        return null;
    }

    public static ItemStack getBackgroundGlass(boolean type) {
        try {
            EpicHoppers instance = EpicHoppers.getInstance();
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
            EpicHoppers instance = EpicHoppers.getInstance();
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


    public static void tpPlayer(Player player, Hopper hopper) {
        try {
            EpicHoppers instance = EpicHoppers.getInstance();
            Block next = hopper.getLocation().getBlock();
            int num = 1;
            while (instance.getHopperManager().isHopper(next.getLocation()) && instance.getHopperManager().getHopper(next.getLocation()).getSyncedBlock() != null && num != 15) {
                Hopper nextHopper = instance.getHopperManager().getHopper(next);
                if (nextHopper.getSyncedBlock() != null) {
                    next = nextHopper.getSyncedBlock();
                }
                if (!next.getType().equals(Material.HOPPER)) {
                    instance.getHopperManager().removeHopper(nextHopper.getLocation());
                    break;
                }

                Location location = next.getLocation();
                location.setX(location.getX() + 0.5);
                location.setZ(location.getZ() + 0.5);
                location.setY(location.getY() + 1);
                location.setPitch(player.getLocation().getPitch());
                location.setDirection(player.getLocation().getDirection());
                player.teleport(location);
                next = player.getLocation().subtract(0, 0.5, 0).getBlock();

                num++;
            }
            if (num != 1) {
                Methods.doParticles(player, hopper.getLocation());
                Methods.doParticles(player, next.getLocation());
            }
        } catch (Exception e) {
            Debugger.runReport(e);
        }
    }

    public static void doParticles(Player p, Location location) {
        try {
            EpicHoppers instance = EpicHoppers.getInstance();
            location.setX(location.getX() + .5);
            location.setY(location.getY() + .5);
            location.setZ(location.getZ() + .5);
            if (!instance.v1_8 && !instance.v1_7) {
                p.getWorld().spawnParticle(org.bukkit.Particle.valueOf(instance.getConfig().getString("Main.Upgrade Particle Type")), location, 200, .5, .5, .5);
            } else {
                p.getWorld().playEffect(location, org.bukkit.Effect.valueOf(instance.getConfig().getString("Main.Upgrade Particle Type")), 1, 0);
                //Does not resolve --Nova
                //p.getWorld().spigot().playEffect(location, org.bukkit.Effect.valueOf(instance.getConfig().getString("Main.Upgrade Particle Type")), 1, 0, (float) 1, (float) 1, (float) 1, 1, 200, 10);
            }
        } catch (Exception e) {
            Debugger.runReport(e);
        }
    }

    @SuppressWarnings("unchecked")
    public static Collection<Entity> getNearbyEntities(Location location, double x, double y, double z) {
        if (location == null) return Collections.emptyList();

        World world = location.getWorld();
        AxisAlignedBB aabb = AxisAlignedBB.a(location.getX() - x, location.getY() - y, location.getZ() - z, location.getX() + x, location.getY() + y, location.getZ() + z);
        List<net.minecraft.server.v1_7_R4.Entity> entityList = ((CraftWorld) world).getHandle().getEntities(null, aabb, null);
        List<Entity> bukkitEntityList = new ArrayList<>();

        for (Object entity : entityList) {
            bukkitEntityList.add(((net.minecraft.server.v1_7_R4.Entity) entity).getBukkitEntity());
        }

        return bukkitEntityList;
    }


}
