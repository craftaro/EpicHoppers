package com.songoda.epichoppers.storage;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        List<Map<String, Object>> object = new ArrayList<>();
        for (ItemStack m : material) {
            Map<String, Object> serialized = m.serialize();
            object.add(serialized);
        }
        this.key = key;
        this.object = object;
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
        List<Map<String,Object>> itemstacks = (List<Map<String, Object>>) object;
        for (Map<String, Object> serial:itemstacks) {
            list.add(ItemStack.deserialize(serial));
        }
        return list;
    }
}
