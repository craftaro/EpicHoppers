package com.songoda.epichoppers.hopper;

import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class Filter {

    private List<ItemStack> whiteList = new ArrayList<>();
    private List<ItemStack> blackList = new ArrayList<>();
    private List<ItemStack> voidList = new ArrayList<>();

    private Location endPoint;


    public List<ItemStack> getWhiteList() {
        if (whiteList == null) return new ArrayList<>();
        return whiteList;
    }


    public void setWhiteList(List<ItemStack> whiteList) {
        this.whiteList = whiteList;
    }


    public List<ItemStack> getBlackList() {
        if (blackList == null) return new ArrayList<>();
        return blackList;
    }


    public void setBlackList(List<ItemStack> blackList) {
        this.blackList = blackList;
    }


    public List<ItemStack> getVoidList() {
        if (voidList == null) return new ArrayList<>();
        return voidList;
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
