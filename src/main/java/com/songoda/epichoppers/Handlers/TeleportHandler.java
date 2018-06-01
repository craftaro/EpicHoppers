package com.songoda.epichoppers.Handlers;

import com.songoda.epichoppers.EpicHoppers;
import com.songoda.epichoppers.Hopper.Hopper;
import com.songoda.epichoppers.Utils.Debugger;
import com.songoda.epichoppers.Utils.Methods;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class TeleportHandler {

    //Teleport from - teleport 2
    private final Map<Location, Location> teleportFrom = new HashMap<>();

    private EpicHoppers instance;

    public TeleportHandler(EpicHoppers instance) {
        try {
            this.instance = instance;
            Bukkit.getScheduler().scheduleSyncRepeatingTask(instance, this::teleportRunner, 0, instance.getConfig().getLong("Main.Amount of Ticks Between Teleport"));
        } catch (Exception e) {
            Debugger.runReport(e);
        }
    }

    private void teleportRunner() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!instance.getConfig().getBoolean("Main.Allow Players To Teleport Through Hoppers") || !player.hasPermission("EpicHoppers.Teleport")) {
                continue;
            }

            Location location = player.getLocation().getBlock().getRelative(BlockFace.DOWN).getLocation();

            if (!instance.getHopperManager().isHopper(location)) {
                continue;
            }

            Hopper hopper = instance.getHopperManager().getHopper(location);

            if (!hopper.isWalkOnTeleport()) continue;

            if (instance.lastTp.containsKey(player)) {
                long duration = (new Date()).getTime() - instance.lastTp.get(player).getTime();
                if (duration <= 5 * 1000) {
                    continue;
                }
            }

            tpPlayer(player, hopper);
            instance.lastTp.put(player, new Date());
        }
    }

    public void tpPlayer(Player player, Hopper hopper) {
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
            if (num == 1 && teleportFrom.containsKey(hopper.getLocation())) {
                Location location = teleportFrom.get(hopper.getLocation());
                location.setX(location.getX() + 0.5);
                location.setZ(location.getZ() + 0.5);
                location.setY(location.getY() + 1);
                location.setPitch(player.getLocation().getPitch());
                location.setDirection(player.getLocation().getDirection());
                player.teleport(location);
                next = player.getLocation().subtract(0, 0.5, 0).getBlock();
                num ++;

            }
            if (num != 1) {
                teleportFrom.put(next.getLocation(), hopper.getLocation());
                Methods.doParticles(player, hopper.getLocation());
                Methods.doParticles(player, next.getLocation());
            }
        } catch (Exception e) {
            Debugger.runReport(e);
        }
    }
}
