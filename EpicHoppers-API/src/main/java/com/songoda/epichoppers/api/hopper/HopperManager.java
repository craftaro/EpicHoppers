package com.songoda.epichoppers.api.hopper;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.Map;

public interface HopperManager {

    void addHopper(Location location, Hopper hopper);

    Hopper removeHopper(Location location);

    Hopper getHopper(Location location);

    Hopper getHopper(Block block);

    boolean isHopper(Location location);

    Map<Location, Hopper> getHoppers();

    Hopper getHopperFromPlayer(Player player);
}
