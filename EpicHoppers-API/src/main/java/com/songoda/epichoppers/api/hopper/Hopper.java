package com.songoda.epichoppers.api.hopper;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.UUID;

public interface Hopper {

    /**
     * This will sync this hopper with another hopper.
     *
     * @param toSync the block containing the hopper
     *               that this hopper will be synchronized
     *               with
     * @param filtered whether or not this action is for the
     *                 filtered sync or not
     * @param player the player initializing the synchronization
     */
    void sync(Block toSync, boolean filtered, Player player);

    /**
     * Get location of the hopper.
     *
     * @return location of spawner
     */
    Location getLocation();

    /**
     * Get the X coordinate for the hopper.
     *
     * @return X coordinate.
     */
    int getX();

    /**
     * Get the Y coordinate for the shopper.
     *
     * @return Y coordinate.
     */
    int getY();

    /**
     * Get the Z coordinate for the hopper.
     *
     * @return Z coordinate.
     */
    int getZ();

    /**
     * Get the {@link Level} associated with this hopper.
     *
     * @return the hoppers level
     */
    Level getLevel();

    /**
     * Get the player that placed this hopper.
     *
     * @return the player the placed this hopper.
     */
    UUID getPlacedBy();

    /**
     * Get the player that last used this hopper.
     *
     * @return the last player
     */
    UUID getLastPlayer();

    /**
     * Set the last player to use this hopper.
     *
     * @param uuid the last player
     */
    void setLastPlayer(UUID uuid);

    /**
     * Whether or not walk on teleporting has been
     * enabled for this hopper.
     *
     * @return true if walk on teleporting enabled,
     * false otherwise
     */
    boolean isWalkOnTeleport();

    /**
     * Set the ability to teleport players from this
     * hopper to a remote hopper.
     *
     * @param walkOnTeleport whether or not to enabled
     *                       walk on teleporting
     */
    void setWalkOnTeleport(boolean walkOnTeleport);

    /**
     * Get the Block containing the hopper that is
     * currently synchronised with this hopper.
     *
     * @return the Block in which this hopper is
     * currently synchronized too
     */
    Block getSyncedBlock();

    /**
     * Set the Block containing a hopper in which
     * to synchronize this hopper with.
     *
     * @param syncedBlock block to sync with
     */
    void setSyncedBlock(Block syncedBlock);

    /**
     * Get the filter associated with this hopper
     *
     * @return filter associated with this hopper
     */
    Filter getFilter();
}
