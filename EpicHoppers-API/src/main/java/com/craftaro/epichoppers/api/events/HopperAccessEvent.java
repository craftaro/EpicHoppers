package com.craftaro.epichoppers.api.events;

import com.craftaro.epichoppers.hopper.Hopper;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;

/**
 * Called when a hopper is accessed by a player.
 */
public class HopperAccessEvent extends HopperEvent implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();

    private boolean canceled = false;

    public HopperAccessEvent(Player who, Hopper hopper) {
        super(who, hopper);
    }

    @Override
    public boolean isCancelled() {
        return this.canceled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.canceled = cancel;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
