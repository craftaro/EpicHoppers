package com.songoda.epichoppers.hopper;

import com.songoda.epichoppers.api.hopper.Filter;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class EFilter implements Filter {

    private List<ItemStack> whiteList = new ArrayList<>();
    private List<ItemStack> blackList = new ArrayList<>();
    private List<ItemStack> voidList = new ArrayList<>();

    private Block endPoint;

    @Override
    public List<ItemStack> getWhiteList() {
        if (whiteList == null) return new ArrayList<>();
        return whiteList;
    }

    @Override
    public void setWhiteList(List<ItemStack> whiteList) {
        this.whiteList = whiteList;
    }

    @Override
    public List<ItemStack> getBlackList() {
        if (blackList == null) return new ArrayList<>();
        return blackList;
    }

    @Override
    public void setBlackList(List<ItemStack> blackList) {
        this.blackList = blackList;
    }

    @Override
    public List<ItemStack> getVoidList() {
        if (voidList == null) return new ArrayList<>();
        return voidList;
    }

    @Override
    public void setVoidList(List<ItemStack> voidList) {
        this.voidList = voidList;
    }

    @Override
    public Block getEndPoint() {
        return endPoint;
    }

    @Override
    public void setEndPoint(Block endPoint) {
        this.endPoint = endPoint;
    }
}
