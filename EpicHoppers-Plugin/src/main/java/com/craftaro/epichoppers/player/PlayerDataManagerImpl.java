package com.craftaro.epichoppers.player;

import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerDataManagerImpl implements PlayerDataManager {
    private final Map<UUID, PlayerData> registeredPlayers = new HashMap<>();

    private PlayerData getPlayerData(UUID uuid) {
        return this.registeredPlayers.computeIfAbsent(uuid, u -> new PlayerData());
    }

    @Override
    public PlayerData getPlayerData(Player player) {
        return getPlayerData(player.getUniqueId());
    }

    @Override
    public Collection<PlayerData> getRegisteredPlayers() {
        return Collections.unmodifiableCollection(this.registeredPlayers.values());
    }
}
