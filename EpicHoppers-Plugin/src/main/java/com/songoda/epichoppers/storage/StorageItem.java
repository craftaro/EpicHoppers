package com.songoda.epichoppers.storage;

import com.songoda.arconix.plugin.Arconix;
import com.songoda.epichoppers.utils.Serializers;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class StorageItem {

    private String key = null;

    private final Object object;

    public StorageItem(Object object) {
        this.object = object;
    }

    public StorageItem(String key, Object object) {
        this.key = key;
        this.object = object;
    }

    public StorageItem(String key, List<ItemStack> material) {
        StringBuilder object = new StringBuilder();
        for (ItemStack m : material) {
            object.append(Serializers.serialize(m));
            object.append(";;");
        }
        this.key = key;
        this.object = object.toString();
    }

    public StorageItem(String key, boolean type, List<Location> blocks) {
        StringBuilder object = new StringBuilder();
        for (Location location : blocks) {
            object.append(Arconix.pl().getApi().serialize().serializeLocation(location));
            object.append(";;");
        }
        this.key = key;
        this.object = object.toString();
    }

    public String getKey() {
        return key;
    }

    public String asString() {
        if (object == null) return null;
        return (String)object;
    }

    public boolean asBoolean() {
        if (object == null) return false;
        return (boolean)object;
    }

    public int asInt() {
        if (object == null) return 0;
        return (int)object;
    }

    public Object asObject() {
        return object;
    }

    public List<ItemStack> asItemStackList() {
        List<ItemStack> list = new ArrayList<>();
        if (object == null) return list;
        String obj = (String) object;
        if (obj.equals("[]"))return list;
        List<String> sers = new ArrayList<>(Arrays.asList(obj.split(";;")));
        for (String ser : sers) {
            list.add(Serializers.deserialize(ser));
        }
        return list;
    }

    public List<String> asStringList() {
        List<String> list = new ArrayList<>();
        if (object == null) return list;
        String obj = (String) object;
        if (!((String) object).contains(";;")) {
            list.add(obj);
            return list;
        }
        return new ArrayList<>(Arrays.asList(obj.split(";;")));
    }
}
