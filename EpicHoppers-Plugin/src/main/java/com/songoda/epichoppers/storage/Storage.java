package com.songoda.epichoppers.storage;

import com.songoda.arconix.api.utils.ConfigWrapper;
import com.songoda.epichoppers.EpicHoppersPlugin;

import java.util.List;

public abstract class Storage {

    protected final EpicHoppersPlugin instance;
    protected final ConfigWrapper dataFile;

    public Storage(EpicHoppersPlugin instance) {
        this.instance = instance;
        this.dataFile = new ConfigWrapper(instance, "", "data.yml");
        this.dataFile.createNewFile(null, "EpicHoppers Data File");
        this.dataFile.getConfig().options().copyDefaults(true);
        this.dataFile.saveConfig();
    }

    public abstract boolean containsGroup(String group);

    public abstract List<StorageRow> getRowsByGroup(String group);

    public abstract void prepareSaveItem(String group, StorageItem... items);

    public abstract void doSave();

    public abstract void closeConnection();

}
