package com.songoda.epichoppers.storage;

import com.songoda.epichoppers.EpicHoppers;
import com.songoda.epichoppers.boost.BoostData;
import com.songoda.epichoppers.hopper.Hopper;
import com.songoda.epichoppers.utils.ConfigWrapper;
import com.songoda.epichoppers.utils.Methods;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.List;

public abstract class Storage {

    protected final EpicHoppers instance;
    protected final ConfigWrapper dataFile;

    public Storage(EpicHoppers instance) {
        this.instance = instance;
        this.dataFile = new ConfigWrapper(instance, "", "data.yml");
        this.dataFile.createNewFile(null, "EpicHoppers Data File");
        this.dataFile.getConfig().options().copyDefaults(true);
        this.dataFile.saveConfig();
    }

    public abstract boolean containsGroup(String group);

    public abstract List<StorageRow> getRowsByGroup(String group);

    public abstract void prepareSaveItem(String group, StorageItem... items);

    public void updateData(EpicHoppers instance) {
        /*
         * Dump HopperManager to file.
         */
        for (Hopper hopper : instance.getHopperManager().getHoppers().values()) {
            if (hopper.getLevel() == null || hopper.getLocation() == null)
                continue;

            String locationStr = Methods.serializeLocation(hopper.getLocation());

            prepareSaveItem("sync", new StorageItem("location", locationStr),
                    new StorageItem("level", hopper.getLevel().getLevel()),
                    new StorageItem("block", true, hopper.getLinkedBlocks() == null || hopper.getLinkedBlocks().isEmpty() ? new ArrayList<>() : hopper.getLinkedBlocks()),
                    new StorageItem("placedby", hopper.getPlacedBy() == null ? null : hopper.getPlacedBy().toString()),
                    new StorageItem("player", hopper.getLastPlayer() == null ? null : hopper.getLastPlayer().toString()),
                    new StorageItem("teleporttrigger", hopper.getTeleportTrigger().toString()),

                    new StorageItem("autocrafting", hopper.getAutoCrafting() == null || hopper.getAutoCrafting().getType() == Material.AIR ? null : hopper.getAutoCrafting().getType().name() + (hopper.getAutoCrafting().getDurability() == 0 ? "" : ":" + hopper.getAutoCrafting().getDurability())),                    new StorageItem("whitelist", hopper.getFilter().getWhiteList()),
                    new StorageItem("blacklist", hopper.getFilter().getBlackList()),
                    new StorageItem("void", hopper.getFilter().getVoidList()),
                    new StorageItem("autobreak", hopper.isAutoBreaking()),
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
