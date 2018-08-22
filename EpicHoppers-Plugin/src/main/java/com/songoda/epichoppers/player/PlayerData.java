package com.songoda.epichoppers.player;

import com.songoda.epichoppers.api.hopper.Hopper;

import java.util.Date;
import java.util.UUID;

public class PlayerData {

    private final UUID playerUUID;

    private Hopper lastHopper = null;

    private MenuType inMenu = MenuType.NOT_IN;

    private SyncType syncType = null; // Null means off.

    private Date lastTeleport = null; // Null means off.

    public PlayerData(UUID playerUUID) {
        this.playerUUID = playerUUID;
    }

    public UUID getPlayerUUID() {
        return playerUUID;
    }

    public Hopper getLastHopper() {
        return lastHopper;
    }

    public void setLastHopper(Hopper lastHopper) {
        this.lastHopper = lastHopper;
    }

    public MenuType getInMenu() {
        return inMenu;
    }

    public void setInMenu(MenuType inMenu) {
        this.inMenu = inMenu;
    }

    public SyncType getSyncType() {
        return syncType;
    }

    public void setSyncType(SyncType syncType) {
        this.syncType = syncType;
    }

    public Date getLastTeleport() {
        return lastTeleport;
    }

    public void setLastTeleport(Date lastTeleport) {
        this.lastTeleport = lastTeleport;
    }
}
