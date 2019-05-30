package com.songoda.epichoppers.hopper;

import com.songoda.epichoppers.EpicHoppers;
import com.songoda.epichoppers.utils.TeleportTrigger;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.ArrayList;
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
            addHopper(location, new Hopper(location, EpicHoppers.getInstance().getLevelManager().getLowestLevel(), null, null, new ArrayList<>(), new Filter(), TeleportTrigger.DISABLED, null));
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
