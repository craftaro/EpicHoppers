package com.songoda.epichoppers.api.hopper;

import org.bukkit.Material;
import org.bukkit.block.Block;

import java.util.List;

public interface Filter {
    List<Material> getWhiteList();

    void setWhiteList(List<Material> whiteList);

    List<Material> getBlackList();

    void setBlackList(List<Material> blackList);

    List<Material> getVoidList();

    void setVoidList(List<Material> voidList);

    Block getEndPoint();

    void setEndPoint(Block endPoint);
}
