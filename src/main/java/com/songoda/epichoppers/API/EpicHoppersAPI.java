package com.songoda.epichoppers.API;

import com.songoda.epichoppers.EpicHoppers;
import org.bukkit.inventory.ItemStack;

/**
 * Created by songo on 6/11/2017.
 */
public class EpicHoppersAPI {

    public int getILevel(ItemStack item) {
        if (item.getItemMeta().getDisplayName().contains(":")) {
            String arr[] = (item.getItemMeta().getDisplayName().replace("§", "")).split(":");
            return Integer.parseInt(arr[0]);
        } else {
            return EpicHoppers.getInstance().getLevelManager().getLowestLevel().getLevel();
        }
    }
}