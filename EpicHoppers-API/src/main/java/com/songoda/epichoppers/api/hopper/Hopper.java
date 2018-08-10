package com.songoda.epichoppers.api.hopper;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.UUID;

public interface Hopper {
    void sync(Block toSync, boolean filtered, Player player);

    Location getLocation();

    int getX();

    int getY();

    int getZ();

    Level getLevel();

    UUID getLastPlayer();

    boolean isWalkOnTeleport();

    void setWalkOnTeleport(boolean walkOnTeleport);

    Block getSyncedBlock();

    void setSyncedBlock(Block syncedBlock);

    Filter getFilter();
}
