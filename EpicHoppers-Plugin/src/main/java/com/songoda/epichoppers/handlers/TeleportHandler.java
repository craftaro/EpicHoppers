package com.songoda.epichoppers.handlers;

import com.songoda.epichoppers.EpicHoppersPlugin;
import com.songoda.epichoppers.api.hopper.Hopper;
import com.songoda.epichoppers.api.hopper.TeleportTrigger;
import com.songoda.epichoppers.player.PlayerData;
import com.songoda.epichoppers.utils.Debugger;
import com.songoda.epichoppers.utils.Methods;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class TeleportHandler {

    //Teleport from - teleport 2
    private final Map<Location, Location> teleportFrom = new HashMap<>();

    private EpicHoppersPlugin instance;

    public TeleportHandler(EpicHoppersPlugin instance) {
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

            if (hopper.getTeleportTrigger() != TeleportTrigger.WALK_ON) continue;

            PlayerData playerData = instance.getPlayerDataManager().getPlayerData(player);

            if (playerData.getLastTeleport() != null) {
                long duration = (new Date()).getTime() - playerData.getLastTeleport().getTime();
                if (duration <= 5 * 1000) {
                    continue;
                }
            }

            tpPlayer(player, hopper);
            playerData.setLastTeleport(new Date());
        }
    }

    public void tpPlayer(Player player, Hopper hopper) {
        try {
            EpicHoppersPlugin instance = EpicHoppersPlugin.getInstance();
            Block next = hopper.getLocation().getBlock();
            int num = 1;
            while (instance.getHopperManager().isHopper(next.getLocation()) && instance.getHopperManager().getHopper(next.getLocation()).getSyncedBlock() != null && num != 15) {
                Hopper nextHopper = instance.getHopperManager().getHopper(next);
                if (nextHopper.getSyncedBlock() != null) {
                    next = nextHopper.getSyncedBlock();
                }
                if (nextHopper == hopper) break;
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

                if (instance.getConfig().getBoolean("Main.Sounds Enabled"))
                    player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 10,10);
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

                if (instance.getConfig().getBoolean("Main.Sounds Enabled"))
                    player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 10,10);
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
