package com.songoda.epichoppers.gui;

import com.songoda.core.compatibility.CompatibleMaterial;
import com.songoda.core.gui.CustomizableGui;
import com.songoda.core.gui.GuiUtils;
import com.songoda.core.utils.TextUtils;
import com.songoda.epichoppers.EpicHoppers;
import com.songoda.epichoppers.hopper.Hopper;
import com.songoda.epichoppers.hopper.levels.modules.ModuleAutoCrafting;
import com.songoda.epichoppers.settings.Settings;
import com.songoda.epichoppers.utils.Methods;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class GUICrafting extends CustomizableGui {

    public GUICrafting(ModuleAutoCrafting module, Hopper hopper, Player player) {
        super(EpicHoppers.getInstance(), "crafting");
        setRows(3);
        setTitle(Methods.formatName(hopper.getLevel().getLevel()) + TextUtils.formatText(" &8-&f Crafting"));
        setOnClose((event) -> {
            hopper.setActivePlayer(null);
            setItem(module, hopper, player);
        });
        setAcceptsItems(true);

        ItemStack glass1 = GuiUtils.getBorderItem(Settings.GLASS_TYPE_1.getMaterial());
        ItemStack glass2 = GuiUtils.getBorderItem(Settings.GLASS_TYPE_2.getMaterial());
        ItemStack glass3 = GuiUtils.getBorderItem(Settings.GLASS_TYPE_3.getMaterial());

        setDefaultItem(glass1);

        mirrorFill("mirrorfill_1", 0, 0, true, true, glass2);
        mirrorFill("mirrorfill_2", 0, 1, true, true, glass2);
        mirrorFill("mirrorfill_3", 0, 2, true, true, glass3);
        mirrorFill("mirrorfill_4", 1, 0, false, true, glass2);
        mirrorFill("mirrorfill_5", 1, 1, false, true, glass3);

        setButton("back", 8, GuiUtils.createButtonItem(CompatibleMaterial.ARROW.getItem(),
                EpicHoppers.getInstance().getLocale().getMessage("general.nametag.back").getMessage()),
                (event) -> {
                    hopper.overview(guiManager, event.player);
                    setItem(module, hopper, player);
                }
        );

        setButton(13, module.getAutoCrafting(hopper),
                (event) -> module.setAutoCrafting(hopper, player, inventory.getItem(13)));

        setUnlocked(13);
    }

    public void setItem(ModuleAutoCrafting module, Hopper hopper, Player player) {
        module.setAutoCrafting(hopper, player, inventory.getItem(13));
    }
}
