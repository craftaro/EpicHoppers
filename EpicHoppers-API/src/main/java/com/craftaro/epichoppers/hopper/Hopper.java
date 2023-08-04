package com.craftaro.epichoppers.hopper;

import com.craftaro.core.database.Data;
import com.craftaro.epichoppers.hopper.levels.Level;
import com.craftaro.epichoppers.hopper.teleport.TeleportTrigger;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public interface Hopper extends Data {

    Location getLocation();

    Block getBlock();

    Level getLevel();

    void setLevel(Level level);

    @Nullable UUID getLastPlayerOpened();

    @Nullable UUID getPlacedBy();

    void setPlacedBy(UUID placedBy);

    void setLastPlayerOpened(UUID lastPlayerOpened);

    TeleportTrigger getTeleportTrigger();

    void setTeleportTrigger(TeleportTrigger teleportTrigger);

    List<Location> getLinkedBlocks();

    void addLinkedBlock(Location location, LinkType type);

    void removeLinkedBlock(Location location);

    void clearLinkedBlocks();

    Filter getFilter();

    void setActivePlayer(Player activePlayer);

    void timeout(Player player);

    void addDataToModuleCache(String s, Object value);

    boolean isDataCachedInModuleCache(String cacheStr);

    Object getDataFromModuleCache(String cacheStr);

    void clearModuleCache();
}
