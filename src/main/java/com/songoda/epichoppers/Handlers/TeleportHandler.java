package com.songoda.epichoppers.Handlers;

import com.songoda.epichoppers.EpicHoppers;
import com.songoda.epichoppers.Hopper.Hopper;
import com.songoda.epichoppers.Utils.Debugger;
import com.songoda.epichoppers.Utils.Methods;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;

import java.util.Date;

public class TeleportHandler {

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

            Methods.tpPlayer(player, hopper);
            instance.lastTp.put(player, new Date());
        }


    }
}
