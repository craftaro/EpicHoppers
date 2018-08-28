package com.songoda.epichoppers.hopper;

import com.songoda.epichoppers.EpicHoppersPlugin;
import com.songoda.epichoppers.api.hopper.Hopper;
import com.songoda.epichoppers.api.hopper.HopperManager;
import com.songoda.epichoppers.api.hopper.TeleportTrigger;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class EHopperManager implements HopperManager {

    private final Map<Location, Hopper> registeredHoppers = new HashMap<>();

    @Override
    public void addHopper(Location location, Hopper hopper) {
        registeredHoppers.put(roundLocation(location), hopper);
    }

    @Override
    public Hopper removeHopper(Location location) {
        return registeredHoppers.remove(location);
    }

    @Override
    public Hopper getHopper(Location location) {
        if (!registeredHoppers.containsKey(roundLocation(location))) {
            addHopper(location, new EHopper(location, EpicHoppersPlugin.getInstance().getLevelManager().getLowestLevel(), null, null, null, new EFilter(), TeleportTrigger.DISABLED, null));
        }
        return registeredHoppers.get(roundLocation(location));
    }

    @Override
    public Hopper getHopper(Block block) {
        return getHopper(block.getLocation());
    }

    @Override
    public boolean isHopper(Location location) {
        return registeredHoppers.containsKey(roundLocation(location));
    }

    @Override
    public Map<Location, Hopper> getHoppers() {
        return Collections.unmodifiableMap(registeredHoppers);
    }

    @Override
    public Hopper getHopperFromPlayer(Player player) {
        for (Hopper hopper : registeredHoppers.values()) {
            if (hopper.getLastPlayer() == player.getUniqueId()) {
                return hopper;
            }
        }
        return null;
    }

    private Location roundLocation(Location location) {
        location = location.clone();
        location.setX(location.getBlockX());
        location.setY(location.getBlockY());
        location.setZ(location.getBlockZ());
        return location;
    }
}
