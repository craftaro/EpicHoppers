package com.songoda.epichoppers.api.hopper.levels.modules;

import com.songoda.epichoppers.api.hopper.Hopper;
import org.bukkit.Material;

import java.util.List;

public interface Module {

    String getName();

    void run(Hopper hopper);

    List<Material> getBlockedItems(Hopper hopper);

    String getDescription();

}
