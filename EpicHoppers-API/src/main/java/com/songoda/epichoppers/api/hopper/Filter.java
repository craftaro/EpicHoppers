package com.songoda.epichoppers.api.hopper;

import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public interface Filter {
    List<ItemStack> getWhiteList();

    void setWhiteList(List<ItemStack> whiteList);

    List<ItemStack> getBlackList();

    void setBlackList(List<ItemStack> blackList);

    List<ItemStack> getVoidList();

    void setVoidList(List<ItemStack> voidList);

    Block getEndPoint();

    void setEndPoint(Block endPoint);
}
