package com.songoda.epichoppers.hopper;

import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

import java.util.Collections;
import java.util.List;

public class Filter {

    private List<ItemStack> whiteList;
    private List<ItemStack> blackList;
    private List<ItemStack> voidList;

    private Location endPoint;


    public List<ItemStack> getWhiteList() {
        return whiteList != null ? whiteList : Collections.EMPTY_LIST;
    }


    public void setWhiteList(List<ItemStack> whiteList) {
        this.whiteList = whiteList;
    }


    public List<ItemStack> getBlackList() {
        return blackList != null ? blackList : Collections.EMPTY_LIST;
    }


    public void setBlackList(List<ItemStack> blackList) {
        this.blackList = blackList;
    }


    public List<ItemStack> getVoidList() {
        return voidList != null ? voidList : Collections.EMPTY_LIST;
    }


    public void setVoidList(List<ItemStack> voidList) {
        this.voidList = voidList;
    }


    public Location getEndPoint() {
        return endPoint;
    }


    public void setEndPoint(Location endPoint) {
        this.endPoint = endPoint;
    }
}
