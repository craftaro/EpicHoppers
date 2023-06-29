package com.craftaro.epichoppers.player;

import org.bukkit.entity.Player;

import java.util.Collection;

public interface PlayerDataManager {
    PlayerData getPlayerData(Player player);

    Collection<PlayerData> getRegisteredPlayers();
}
