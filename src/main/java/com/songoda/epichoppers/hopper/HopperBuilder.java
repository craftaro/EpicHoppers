package com.songoda.epichoppers.hopper;

import com.songoda.epichoppers.hopper.levels.Level;
import com.songoda.epichoppers.utils.TeleportTrigger;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class HopperBuilder {

    private final Hopper hopper;

    public HopperBuilder(Location location) {
        this.hopper = new Hopper(location);
    }

    public HopperBuilder(Block block) {
        this(block.getLocation());
    }

    public HopperBuilder setLevel(Level level) {
        this.hopper.setLevel(level);
        return this;
    }

    public HopperBuilder addLinkedBlocks(Location... linkedBlocks) {
        for (Location location : linkedBlocks)
            hopper.addLinkedBlock(location);
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

    public HopperBuilder setAutoCrafting(ItemStack autoCrafting) {
        this.hopper.setAutoCrafting(autoCrafting);
        return this;
    }

    public HopperBuilder setAutoSelling(boolean autoSelling) {
        this.hopper.setAutoSellTimer(autoSelling ? 0 : -9999);
        return this;
    }

    public HopperBuilder setAutoBreaking(boolean autoBreaking) {
        this.hopper.setAutoBreaking(autoBreaking);
        return this;
    }

    public Hopper build() {
        return this.hopper;
    }
}
