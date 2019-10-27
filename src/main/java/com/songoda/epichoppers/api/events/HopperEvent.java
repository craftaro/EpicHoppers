package com.songoda.epichoppers.api.events;

import com.songoda.epichoppers.hopper.Hopper;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerEvent;

/**
 * Represents an abstract {@link Event} given a {@link Player} and {@link Hopper} instance
 */
public abstract class HopperEvent extends PlayerEvent {

    protected final Hopper hopper;

    public HopperEvent(Player who, Hopper hopper) {
        super(who);
        this.hopper = hopper;
    }

    /**
     * Get the {@link Hopper} involved in this event
     *
     * @return the broken spawner
     */
    public Hopper getHopper() {
        return hopper;
    }

}