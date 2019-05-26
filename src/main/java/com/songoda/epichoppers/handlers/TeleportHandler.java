package com.songoda.epichoppers.handlers;

import com.songoda.epichoppers.EpicHoppers;
import com.songoda.epichoppers.hopper.Hopper;
import com.songoda.epichoppers.utils.Methods;
import com.songoda.epichoppers.utils.ServerVersion;
import com.songoda.epichoppers.utils.TeleportTrigger;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.InventoryHolder;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TeleportHandler {

    //Teleport from - teleport 2
    private final Map<Location, Location> teleportFrom = new HashMap<>();
    private final Map<UUID, Long> lastTeleports = new HashMap<>();

    private EpicHoppers instance;

    public TeleportHandler(EpicHoppers instance) {
            this.instance = instance;
            Bukkit.getScheduler().scheduleSyncRepeatingTask(instance, this::teleportRunner, 0, instance.getConfig().getLong("Main.Amount of Ticks Between Teleport"));
    }

    private void teleportRunner() {
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (!(entity instanceof LivingEntity) ||entity.getType() == EntityType.ARMOR_STAND) continue;

                if (!instance.getConfig().getBoolean("Main.Allow Players To Teleport Through Hoppers")
                        || entity instanceof Player && !((Player)entity).hasPermission("EpicHoppers.Teleport")) {
                    continue;
                }

                Location location = entity.getLocation().getBlock().getRelative(BlockFace.DOWN).getLocation();

                if (!instance.getHopperManager().isHopper(location)) {
                    continue;
                }

                Hopper hopper = instance.getHopperManager().getHopper(location);

                if (hopper.getTeleportTrigger() != TeleportTrigger.WALK_ON) continue;

                if (lastTeleports.containsKey(entity.getUniqueId())) {
                    long duration = (new Date()).getTime() - new Date(lastTeleports.get(entity.getUniqueId())).getTime();
                    if (duration <= 5 * 1000) {
                        continue;
                    }
                }

                tpEntity(entity, hopper);
                lastTeleports.put(entity.getUniqueId(), System.currentTimeMillis());
            }
        }
    }

    public void tpEntity(Entity entity, Hopper hopper) {
            if (hopper == null || !instance.getHopperManager().isHopper(hopper.getLocation())) return;

        EpicHoppers instance = EpicHoppers.getInstance();
            Hopper lastHopper = hopper;
            for (int i = 0; i < 15; i++) {
                boolean empty = lastHopper.getLinkedBlocks().isEmpty();
                if (empty && i == 0) {
                    if (teleportFrom.containsKey(hopper.getLocation()))
                        doTeleport(entity, teleportFrom.get(hopper.getLocation()).clone());
                    return;
                }

                if (empty) break;
                Location nextHopper = lastHopper.getLinkedBlocks().get(0);
                if (!(nextHopper.getBlock().getState() instanceof InventoryHolder)) break;
                lastHopper = instance.getHopperManager().getHopper(nextHopper);
            }

            teleportFrom.put(lastHopper.getLocation(), hopper.getLocation());
            doTeleport(entity, lastHopper.getLocation());
    }

    private void doTeleport(Entity entity, Location location) {
        location.add(.0, 1, .0);
        location.setPitch(entity.getLocation().getPitch());
        location.setDirection(entity.getLocation().getDirection());
        Methods.doParticles(entity, location);
        Methods.doParticles(entity, entity.getLocation().getBlock().getRelative(BlockFace.DOWN).getLocation());
        entity.teleport(location);

        if (instance.isServerVersionAtLeast(ServerVersion.V1_12))
            entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 10, 10);
    }
}
