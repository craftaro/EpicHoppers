package com.songoda.epichoppers.api.hopper;

import com.songoda.epichoppers.api.hopper.levels.Level;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.UUID;

public interface Hopper {

    org.bukkit.block.Hopper getHopper();

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
     * Get the item being automatically crafted.
     *
     * @return item being crafted
     */
    Material getAutoCrafting();

    /**
     * Set the Item being automatically crafted.
     *
     * @param autoCrafting item to craft
     */
    void setAutoCrafting(Material autoCrafting);

    /**
     * Get the teleport trigger is currently enabled.
     *
     * @return TeleportTrigger
     */
    TeleportTrigger getTeleportTrigger();

    /**
     * Set which teleport trigger is currently enabled.
     *
     * @param teleportTrigger TeleportTrigger
     */
    void setTeleportTrigger(TeleportTrigger teleportTrigger);

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
