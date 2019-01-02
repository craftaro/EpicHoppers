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
import org.bukkit.inventory.InventoryHolder;

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
            if (hopper == null || !instance.getHopperManager().isHopper(hopper.getLocation())) return;

            EpicHoppersPlugin instance = EpicHoppersPlugin.getInstance();
            Hopper lastHopper = hopper;
            for (int i = 0; i < 15; i ++) {
                boolean empty = lastHopper.getLinkedBlocks().isEmpty();
                if (empty && i == 0) {
                    if (teleportFrom.containsKey(hopper.getLocation()))
                        doTeleport(player, teleportFrom.get(hopper.getLocation()));
                    return;
                }

                if (empty) break;
                Location nextHopper = lastHopper.getLinkedBlocks().get(0);
                if (!(nextHopper.getBlock().getState() instanceof InventoryHolder)) break;
                lastHopper = instance.getHopperManager().getHopper(nextHopper);
            }

            teleportFrom.put(lastHopper.getLocation(), hopper.getLocation());
            doTeleport(player, lastHopper.getLocation());
        } catch (Exception e) {
            Debugger.runReport(e);
        }
    }

    private void doTeleport(Player player, Location location) {
        location.add(.0, 1, .0);
        location.setPitch(player.getLocation().getPitch());
        location.setDirection(player.getLocation().getDirection());
        Methods.doParticles(player, location);
        Methods.doParticles(player, player.getLocation().getBlock().getRelative(BlockFace.DOWN).getLocation());
        player.teleport(location);

        if (instance.getConfig().getBoolean("Main.Sounds Enabled"))
            player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 10,10);
    }
}
