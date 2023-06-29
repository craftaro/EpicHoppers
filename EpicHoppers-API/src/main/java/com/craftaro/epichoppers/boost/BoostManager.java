package com.craftaro.epichoppers.boost;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface BoostManager {
    void addBoostToPlayer(BoostData data);

    void removeBoostFromPlayer(BoostData data);

    void addBoosts(List<BoostData> boosts);

    Set<BoostData> getBoosts();

    BoostData getBoost(UUID player);
}
