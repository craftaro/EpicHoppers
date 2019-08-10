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

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TeleportHandler {

    private final Map<UUID, Long> lastTeleports = new HashMap<>();

    private EpicHoppers instance;

    public TeleportHandler(EpicHoppers instance) {
        this.instance = instance;
        Bukkit.getScheduler().scheduleSyncRepeatingTask(instance, this::teleportRunner, 0, instance.getConfig().getLong("Main.Amount of Ticks Between Teleport"));
    }

    private void teleportRunner() {
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (!(entity instanceof LivingEntity) || entity.getType() == EntityType.ARMOR_STAND)
                    continue;

                if (!this.instance.getConfig().getBoolean("Main.Allow Players To Teleport Through Hoppers")
                        || (entity instanceof Player && !entity.hasPermission("EpicHoppers.Teleport")))
                    continue;

                Location location = entity.getLocation().getBlock().getRelative(BlockFace.DOWN).getLocation();

                if (!this.instance.getHopperManager().isHopper(location))
                    continue;

                Hopper hopper = this.instance.getHopperManager().getHopper(location);

                if (hopper.getTeleportTrigger() != TeleportTrigger.WALK_ON)
                    continue;

                if (this.lastTeleports.containsKey(entity.getUniqueId())) {
                    long duration = (new Date()).getTime() - new Date(this.lastTeleports.get(entity.getUniqueId())).getTime();
                    if (duration <= 5 * 1000)
                        continue;
                }

                this.tpEntity(entity, hopper);
                this.lastTeleports.put(entity.getUniqueId(), System.currentTimeMillis());
            }
        }
    }

    public void tpEntity(Entity entity, Hopper hopper) {
        if (hopper == null || !this.instance.getHopperManager().isHopper(hopper.getLocation()))
            return;

        Hopper lastHopper = this.getChain(hopper, 1);
        if (hopper != lastHopper)
            this.doTeleport(entity, lastHopper.getLocation());
    }

    /**
     * Recursively gets the next hopper in the linked hopper chain
     * @param lastHopper The previous hopper found in the chain
     * @param currentChainLength The current length of the chain, used to cap the search length
     * @return The hopper at the end of the chain (or up to 15 in depth)
     */
    private Hopper getChain(Hopper lastHopper, int currentChainLength) {
        if (currentChainLength > 15)
            return lastHopper;

        for (Location nextHopperLocation : lastHopper.getLinkedBlocks()) {
            if (nextHopperLocation.getBlock().getState() instanceof org.bukkit.block.Hopper) {
                Hopper hopper = this.instance.getHopperManager().getHopper(nextHopperLocation);
                if (hopper != null)
                    return this.getChain(hopper, currentChainLength + 1);
            }
        }

        return lastHopper;
    }

    private void doTeleport(Entity entity, Location location) {
        location.add(.0, 1, .0);
        location.setPitch(entity.getLocation().getPitch());
        location.setDirection(entity.getLocation().getDirection());

        if (this.instance.isServerVersionAtLeast(ServerVersion.V1_12)) {
            Methods.doParticles(entity, location);
            Methods.doParticles(entity, entity.getLocation().getBlock().getRelative(BlockFace.DOWN).getLocation());
        }

        entity.teleport(location);

        if (this.instance.isServerVersionAtLeast(ServerVersion.V1_12))
            entity.getWorld().playSound(entity.getLocation(), this.instance.isServerVersion(ServerVersion.V1_12) ? Sound.valueOf("ENTITY_ENDERMEN_TELEPORT") : Sound.ENTITY_ENDERMAN_TELEPORT, 10, 10);
    }
}
