package com.craftaro.epichoppers.api.events;

import com.craftaro.epichoppers.hopper.Hopper;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;

public class HopperBreakEvent extends HopperEvent {
    private static final HandlerList HANDLERS = new HandlerList();

    public HopperBreakEvent(Player who, Hopper hopper) {
        super(who, hopper);
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
