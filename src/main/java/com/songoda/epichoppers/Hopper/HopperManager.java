package com.songoda.epichoppers.Hopper;

import com.songoda.epichoppers.EpicHoppers;
import org.bukkit.Location;
import org.bukkit.block.Block;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class HopperManager {

    private final Map<Location, Hopper> registeredHoppers = new HashMap<>();

    public void addHopper(Location location, Hopper hopper) {
        registeredHoppers.put(roundLocation(location), hopper);
    }

    public Hopper removeHopper(Location location) {
        return registeredHoppers.remove(location);
    }

    public Hopper getHopper(Location location) {
        if (!registeredHoppers.containsKey(roundLocation(location))) {
            addHopper(location, new Hopper(location, EpicHoppers.getInstance().getLevelManager().getLowestLevel(), null, null, new Filter(), false));
        }
        return registeredHoppers.get(roundLocation(location));
    }

    public Hopper getHopper(Block block) {
        return getHopper(block.getLocation());
    }

    public boolean isHopper(Location location) {
        return registeredHoppers.containsKey(roundLocation(location));
    }

    public Map<Location, Hopper> getHoppers() {
        return Collections.unmodifiableMap(registeredHoppers);
    }

    private Location roundLocation(Location location) {
        location = location.clone();
        location.setX(location.getBlockX());
        location.setY(location.getBlockY());
        location.setZ(location.getBlockZ());
        return location;
    }
}
