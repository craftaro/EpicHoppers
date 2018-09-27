package com.songoda.epichoppers.hopper;

import com.songoda.epichoppers.api.hopper.Filter;
import org.bukkit.Material;
import org.bukkit.block.Block;

import java.util.ArrayList;
import java.util.List;

public class EFilter implements Filter {

    private List<Material> whiteList = new ArrayList<>();
    private List<Material> blackList = new ArrayList<>();
    private List<Material> voidList = new ArrayList<>();

    private Block endPoint;

    @Override
    public List<Material> getWhiteList() {
        if (whiteList == null) return new ArrayList<>();
        return whiteList;
    }

    @Override
    public void setWhiteList(List<Material> whiteList) {
        this.whiteList = whiteList;
    }

    @Override
    public List<Material> getBlackList() {
        if (blackList == null) return new ArrayList<>();
        return blackList;
    }

    @Override
    public void setBlackList(List<Material> blackList) {
        this.blackList = blackList;
    }

    @Override
    public List<Material> getVoidList() {
        if (voidList == null) return new ArrayList<>();
        return voidList;
    }

    @Override
    public void setVoidList(List<Material> voidList) {
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
