package com.songoda.epichoppers.player;

import com.songoda.epichoppers.hopper.Hopper;

import java.util.UUID;

public class PlayerData {

    private final UUID playerUUID;

    private Hopper lastHopper = null;

    private SyncType syncType = null; // Null means off.

    PlayerData(UUID playerUUID) {
        this.playerUUID = playerUUID;
    }

    public Hopper getLastHopper() {
        return lastHopper;
    }

    public void setLastHopper(Hopper lastHopper) {
        this.lastHopper = lastHopper;
    }

    public SyncType getSyncType() {
        return syncType;
    }

    public void setSyncType(SyncType syncType) {
        this.syncType = syncType;
    }
}
