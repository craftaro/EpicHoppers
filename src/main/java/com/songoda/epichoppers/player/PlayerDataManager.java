package com.songoda.epichoppers.player;

import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerDataManager {

    private final Map<UUID, PlayerData> registeredPlayers = new HashMap<>();

    private PlayerData getPlayerData(UUID uuid) {
        if (!registeredPlayers.containsKey(uuid))
            registeredPlayers.put(uuid, new PlayerData());
        return registeredPlayers.get(uuid);
    }

    public PlayerData getPlayerData(Player player) {
        return getPlayerData(player.getUniqueId());
    }

    public Collection<PlayerData> getRegisteredPlayers() {
        return Collections.unmodifiableCollection(registeredPlayers.values());
    }
}
