package com.craftaro.epichoppers.boost;

import java.util.Objects;
import java.util.UUID;

public interface BoostData {

    /**
     * Gets the multiplier of the boost
     * @return The multiplier
     */
    int getMultiplier();

    /**
     * Gets the player's uuid who has the boost
     * @return The player's uuid
     */
    public UUID getPlayer();

    /**
     * Gets the end time of the boost
     * @return The end time
     */
    public long getEndTime();
}
