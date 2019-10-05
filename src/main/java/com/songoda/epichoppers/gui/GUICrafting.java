package com.songoda.epichoppers.gui;

import com.songoda.core.gui.Gui;
import com.songoda.core.gui.GuiUtils;
import com.songoda.epichoppers.hopper.Hopper;
import com.songoda.epichoppers.hopper.levels.modules.ModuleAutoCrafting;
import com.songoda.epichoppers.settings.Settings;
import com.songoda.epichoppers.utils.Methods;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class GUICrafting extends Gui {

    public GUICrafting(ModuleAutoCrafting module, Hopper hopper, Player player) {
        setRows(3);
        setTitle(Methods.formatName(hopper.getLevel().getLevel(), false) + Methods.formatText(" &8-&f Crafting"));
        setOnClose((event) -> module.setAutoCrafting(hopper, player, inventory.getItem(13)));
        setAcceptsItems(true);

        ItemStack glass1 = GuiUtils.getBorderItem(Settings.GLASS_TYPE_1.getMaterial());
        ItemStack glass2 = GuiUtils.getBorderItem(Settings.GLASS_TYPE_2.getMaterial());
        ItemStack glass3 = GuiUtils.getBorderItem(Settings.GLASS_TYPE_3.getMaterial());

        setDefaultItem(glass1);

        GuiUtils.mirrorFill(this, 0, 0, true, true, glass2);
        GuiUtils.mirrorFill(this, 0, 1, true, true, glass2);
        GuiUtils.mirrorFill(this, 0, 2, true, true, glass3);
        GuiUtils.mirrorFill(this, 1, 0, false, true, glass2);
        GuiUtils.mirrorFill(this, 1, 1, false, true, glass3);

        setButton(13, module.getAutoCrafting(hopper),
                (event) -> module.setAutoCrafting(hopper, player, inventory.getItem(13)));

        setUnlocked(13);
    }
}
