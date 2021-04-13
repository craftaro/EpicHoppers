package com.songoda.epichoppers.hopper;

import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Filter {

    private List<ItemStack> whiteList = new ArrayList<>();
    private List<ItemStack> blackList = new ArrayList<>();
    private List<ItemStack> voidList = new ArrayList<>();

    private List<ItemStack> autoSellWhiteList = new ArrayList<>();
    private List<ItemStack> autoSellBlackList = new ArrayList<>();


    private Location endPoint;


    public List<ItemStack> getWhiteList() {
        return whiteList != null ? whiteList : Collections.emptyList();
    }


    public void setWhiteList(List<ItemStack> whiteList) {
        this.whiteList = whiteList;
    }


    public List<ItemStack> getBlackList() {
        return blackList != null ? blackList : Collections.emptyList();
    }


    public void setBlackList(List<ItemStack> blackList) {
        this.blackList = blackList;
    }


    public List<ItemStack> getVoidList() {
        return voidList != null ? voidList : Collections.emptyList();
    }


    public void setVoidList(List<ItemStack> voidList) {
        this.voidList = voidList;
    }


    public List<ItemStack> getAutoSellWhiteList() {
        return autoSellWhiteList != null ? autoSellWhiteList : Collections.emptyList();
    }


    public void setAutoSellWhiteList(List<ItemStack> autoSellWhiteList) {
        this.autoSellWhiteList = autoSellWhiteList;
    }


    public List<ItemStack> getAutoSellBlackList() {
        return autoSellBlackList != null ? autoSellBlackList : Collections.emptyList();
    }


    public void setAutoSellBlackList(List<ItemStack> autoSellBlackList) {
        this.autoSellBlackList = autoSellBlackList;
    }


    public Location getEndPoint() {
        return endPoint;
    }


    public void setEndPoint(Location endPoint) {
        this.endPoint = endPoint;
    }

    public void addItem(ItemStack item, ItemType type) {
        switch (type) {
            case WHITELIST:
                whiteList.add(item);
                break;
            case BLACKLIST:
                blackList.add(item);
                break;
            case VOID:
                voidList.add(item);
                break;
            case AUTO_SELL_WHITELIST:
                autoSellWhiteList.add(item);
                break;
            case AUTO_SELL_BLACKLIST:
                autoSellBlackList.add(item);
                break;
        }
    }
}
