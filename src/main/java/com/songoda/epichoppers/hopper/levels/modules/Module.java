package com.songoda.epichoppers.hopper.levels.modules;

import com.songoda.epichoppers.hopper.Hopper;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public interface Module {

    String getName();

    void run(Hopper hopper, Inventory hopperInventory);

    ItemStack getGUIButton(Hopper hopper);

    void runButtonPress(Player player, Hopper hopper);

    List<Material> getBlockedItems(Hopper hopper);

    String getDescription();

}
