package com.songoda.epichoppers.listeners;

import com.songoda.epichoppers.hopper.levels.modules.ModuleSuction;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerPickupItemEvent;

public class EntityListeners implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerPickup(PlayerPickupItemEvent event) {
        if (ModuleSuction.isBlacklisted(event.getItem().getUniqueId()))
            event.setCancelled(true);
    }
}
