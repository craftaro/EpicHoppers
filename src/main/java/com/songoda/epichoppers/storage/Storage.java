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
        // We're not saving data anymore.
    }

    public abstract void doSave();

    public abstract void save();

    public abstract void makeBackup();

    public abstract void closeConnection();

}
