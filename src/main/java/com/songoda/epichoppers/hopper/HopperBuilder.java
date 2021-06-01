package com.songoda.epichoppers.hopper;

import com.songoda.epichoppers.hopper.levels.Level;
import com.songoda.epichoppers.hopper.teleport.TeleportTrigger;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;

import java.util.UUID;

public class HopperBuilder {

    private final Hopper hopper;

    public HopperBuilder(Location location) {
        this.hopper = new Hopper(location);
    }

    public HopperBuilder(Block block) {
        this(block.getLocation());
    }

    public HopperBuilder setId(int id) {
        this.hopper.setId(id);
        return this;
    }

    public HopperBuilder setLevel(Level level) {
        this.hopper.setLevel(level);
        return this;
    }

    public HopperBuilder addLinkedBlocks(LinkType type, Location... linkedBlocks) {
        for (Location location : linkedBlocks)
            hopper.addLinkedBlock(location, type);
        return this;
    }

    public HopperBuilder setFilter(Filter filter) {
        this.hopper.setFilter(filter);
        return this;
    }

    public HopperBuilder setLastPlayerOpened(UUID uuid) {
        this.hopper.setLastPlayerOpened(uuid);
        return this;
    }

    public HopperBuilder setLastPlayerOpened(OfflinePlayer player) {
        return setLastPlayerOpened(player.getUniqueId());
    }

    public HopperBuilder setPlacedBy(UUID uuid) {
        this.hopper.setPlacedBy(uuid);
        return this;
    }

    public HopperBuilder setPlacedBy(OfflinePlayer player) {
        this.hopper.setPlacedBy(player.getUniqueId());
        return this;
    }

    public HopperBuilder setTeleportTrigger(TeleportTrigger teleportTrigger) {
        this.hopper.setTeleportTrigger(teleportTrigger);
        return this;
    }

    public Hopper build() {
        return this.hopper;
    }
}
