package com.craftaro.epichoppers.boost;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class BoostManagerImpl implements BoostManager {
    private final Set<BoostData> registeredBoosts = new HashSet<>();

    @Override
    public void addBoostToPlayer(BoostData data) {
        this.registeredBoosts.add(data);
    }

    @Override
    public void removeBoostFromPlayer(BoostData data) {
        this.registeredBoosts.remove(data);
    }

    @Override
    public void addBoosts(List<BoostData> boosts) {
        this.registeredBoosts.addAll(boosts);
    }

    @Override
    public Set<BoostData> getBoosts() {
        return Collections.unmodifiableSet(this.registeredBoosts);
    }

    @Override
    public BoostData getBoost(UUID player) {
        if (player == null) {
            return null;
        }

        for (BoostData boostData : this.registeredBoosts) {
            if (boostData.getPlayer().toString().equals(player.toString())) {
                if (System.currentTimeMillis() >= boostData.getEndTime()) {
                    removeBoostFromPlayer(boostData);
                }
                return boostData;
            }
        }
        return null;
    }
}
