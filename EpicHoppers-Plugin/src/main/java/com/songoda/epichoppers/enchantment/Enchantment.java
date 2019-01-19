package com.songoda.epichoppers.enchantment;

import com.songoda.epichoppers.utils.Debugger;
import com.songoda.epichoppers.utils.Methods;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;

/**
 * Created by songoda on 3/22/2017.
 */
public class Enchantment {

    public ItemStack createSyncTouch(ItemStack item, Block block) {
        try {
            ItemMeta itemmeta = item.getItemMeta();
            ArrayList<String> lore = new ArrayList<>();
            if (block != null) {
                lore.add(Methods.formatText("&aSync Touch"));
                lore.add(Methods.convertToInvisibleString(Methods.serializeLocation(block)));
            } else {
                lore.add(Methods.formatText("&7Sync Touch"));
            }
            itemmeta.setLore(lore);
            item.setItemMeta(itemmeta);
            return item;
        } catch (Exception e) {
            Debugger.runReport(e);
        }
        return null;
    }

    public ItemStack getbook() {
        try {
            ItemStack book = new ItemStack(Material.ENCHANTED_BOOK);
            ItemMeta meta = book.getItemMeta();
            meta.setDisplayName(Methods.formatText("&eEnchanted Book"));

            ArrayList<String> lore = new ArrayList<>();
            lore.add(Methods.formatText("&7Sync Touch"));
            meta.setLore(lore);
            book.setItemMeta(meta);
            return book;
        } catch (Exception e) {
            Debugger.runReport(e);
        }
        return null;
    }
}