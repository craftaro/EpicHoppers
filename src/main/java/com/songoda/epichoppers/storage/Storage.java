package com.songoda.epichoppers.storage;

import com.songoda.core.configuration.Config;
import com.songoda.epichoppers.EpicHoppers;
import com.songoda.epichoppers.boost.BoostData;
import com.songoda.epichoppers.hopper.Hopper;
import com.songoda.epichoppers.settings.Settings;
import com.songoda.epichoppers.utils.Methods;

import java.util.ArrayList;
import java.util.List;

public abstract class Storage {

    protected final EpicHoppers plugin;
    protected final Config dataFile;

    public Storage(EpicHoppers plugin) {
        this.plugin = plugin;
        this.dataFile = new Config(plugin, "data.yml");
        this.dataFile.load();
    }

    public abstract boolean containsGroup(String group);

    public abstract List<StorageRow> getRowsByGroup(String group);

    public abstract void prepareSaveItem(String group, StorageItem... items);

    public void updateData(EpicHoppers instance) {
        /*
         * Dump HopperManager to file.
         */
        for (Hopper hopper : new ArrayList<>(instance.getHopperManager().getHoppers().values())) {
            if (hopper.getLevel() == null
                    || hopper.getLocation() == null
                    || hopper.getLevel() == instance.getLevelManager().getLowestLevel()
                    && !Settings.ALLOW_NORMAL_HOPPERS.getBoolean()
                    && (hopper.getLinkedBlocks() == null || hopper.getLinkedBlocks().isEmpty()))
                continue;

            String locationStr = Methods.serializeLocation(hopper.getLocation());

            prepareSaveItem("sync", new StorageItem("location", locationStr),
                    new StorageItem("level", hopper.getLevel().getLevel()),
                    new StorageItem("block", true, hopper.getLinkedBlocks() == null || hopper.getLinkedBlocks().isEmpty() ? new ArrayList<>() : hopper.getLinkedBlocks()),
                    new StorageItem("placedby", hopper.getPlacedBy() == null ? null : hopper.getPlacedBy().toString()),
                    new StorageItem("player", hopper.getLastPlayerOpened() == null ? null : hopper.getLastPlayerOpened().toString()),
                    new StorageItem("teleporttrigger", hopper.getTeleportTrigger().toString()),

                    new StorageItem("whitelist", hopper.getFilter().getWhiteList()),
                    new StorageItem("blacklist", hopper.getFilter().getBlackList()),
                    new StorageItem("void", hopper.getFilter().getVoidList()),
                    new StorageItem("black", hopper.getFilter().getEndPoint() == null ? null : Methods.serializeLocation(hopper.getFilter().getEndPoint())));
        }

        /*
         * Dump BoostManager to file.
         */
        for (BoostData boostData : instance.getBoostManager().getBoosts()) {
            prepareSaveItem("boosts", new StorageItem("endtime", String.valueOf(boostData.getEndTime())),
                    new StorageItem("amount", boostData.getMultiplier()),
                    new StorageItem("uuid", boostData.getPlayer().toString()));
        }
    }

    public abstract void doSave();

    public abstract void save();

    public abstract void makeBackup();

    public abstract void closeConnection();

}
