package com.songoda.epichoppers.player;

import com.songoda.epichoppers.hopper.Hopper;

public class PlayerData {
    private Hopper lastHopper = null;
    private SyncType syncType = null; // Null means off.

    public Hopper getLastHopper() {
        return this.lastHopper;
    }

    public void setLastHopper(Hopper lastHopper) {
        this.lastHopper = lastHopper;
    }

    public SyncType getSyncType() {
        return this.syncType;
    }

    public void setSyncType(SyncType syncType) {
        this.syncType = syncType;
    }
}
