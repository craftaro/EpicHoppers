package com.craftaro.epichoppers.hopper;

import com.craftaro.epichoppers.EpicHoppers;
import com.craftaro.epichoppers.hopper.levels.Level;
import com.craftaro.epichoppers.hopper.levels.modules.Module;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class HopperManager {
    private final Map<Location, Hopper> registeredHoppers = new HashMap<>();
    private final EpicHoppers plugin;

    protected boolean ready;

    public HopperManager(EpicHoppers plugin) {
        this.plugin = plugin;
    }

    /**
     * Sets {@link #isReady()} to {@code true}.<br>
     * <b>Called by {@link EpicHoppers#onDataLoad()}</b>
     */
    public void setReady() {
        this.ready = true;
    }

    /**
     * @return true, if all the data has been loaded from the DB
     */
    public boolean isReady() {
        return this.ready;
    }

    public Hopper addHopper(Hopper hopper) {
        this.registeredHoppers.put(roundLocation(hopper.getLocation()), hopper);
        return hopper;
    }

    @Deprecated
    public void addHopper(Location location, Hopper hopper) {
        this.registeredHoppers.put(roundLocation(location), hopper);
    }

    public void addHoppers(Collection<Hopper> hoppers) {
        for (Hopper hopper : hoppers) {
            this.registeredHoppers.put(hopper.getLocation(), hopper);
        }
    }

    /**
     * Removes a hopper and unlinks it from any other hoppers
     *
     * @param location The location of the hopper to remove
     * @return The removed hopper, or null if none was removed
     */
    public Hopper removeHopper(Location location) {
        Hopper removed = this.registeredHoppers.remove(location);

        for (Hopper hopper : this.registeredHoppers.values()) {
            hopper.removeLinkedBlock(location);
        }

        for (Level level : this.plugin.getLevelManager().getLevels().values()) {
            for (Module module : level.getRegisteredModules()) {
                module.clearData(removed);
            }
        }

        return removed;
    }

    public Hopper getHopper(Location location) {
        if (!this.registeredHoppers.containsKey(location = roundLocation(location))) {
            if (!this.ready) {
                throw new IllegalStateException("Hoppers are still being loaded");
            }

            Hopper hopper = addHopper(new Hopper(location));
            this.plugin.getDataManager().createHopper(hopper);
        }
        return this.registeredHoppers.get(location);
    }

    public Hopper getHopper(Block block) {
        return getHopper(block.getLocation());
    }

    /**
     * <em>Returns {@code false} if {@link #isReady()} is false too</em>
     */
    public boolean isHopper(Location location) {
        return this.registeredHoppers.containsKey(roundLocation(location));
    }

    public Map<Location, Hopper> getHoppers() {
        return Collections.unmodifiableMap(this.registeredHoppers);
    }

    public Hopper getHopperFromPlayer(Player player) {
        if (!this.ready) {
            throw new IllegalStateException("Hoppers are still being loaded");
        }

        for (Hopper hopper : this.registeredHoppers.values()) {
            if (hopper.getLastPlayerOpened() == player.getUniqueId()) {
                return hopper;
            }
        }

        return null;
    }

    private Location roundLocation(Location location) {
        location = location.clone();
        location.setX(location.getBlockX());
        location.setY(location.getBlockY());
        location.setZ(location.getBlockZ());
        return location;
    }
}
