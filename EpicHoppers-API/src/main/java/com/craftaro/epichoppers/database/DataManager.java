package com.craftaro.epichoppers.database;

import com.craftaro.epichoppers.boost.BoostData;
import com.craftaro.epichoppers.hopper.Hopper;
import com.craftaro.epichoppers.hopper.ItemType;
import com.craftaro.epichoppers.hopper.LinkType;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public interface DataManager {
    void createBoost(BoostData boostData);

    void getBoosts(Consumer<List<BoostData>> callback);

    void deleteBoost(BoostData boostData);

    void createLink(Hopper hopper, Location location, LinkType type);

    void updateItems(Hopper hopper, ItemType type, List<ItemStack> items);

    void deleteLink(Hopper hopper, Location location);

    void deleteLinks(Hopper hopper);

    void createHoppers(List<Hopper> hoppers);

    void createHopper(Hopper hopper);

    void updateHopper(Hopper hopper);

    void deleteHopper(Hopper hopper);

    void getHoppers(Consumer<Map<Integer, Hopper>> callback);
}
