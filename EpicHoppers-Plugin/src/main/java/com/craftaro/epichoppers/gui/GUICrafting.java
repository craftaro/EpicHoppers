package com.craftaro.epichoppers.gui;

import com.craftaro.core.SongodaPlugin;
import com.craftaro.core.gui.CustomizableGui;
import com.craftaro.core.gui.GuiUtils;
import com.craftaro.core.third_party.com.cryptomorin.xseries.XMaterial;
import com.craftaro.core.utils.TextUtils;
import com.craftaro.epichoppers.hopper.levels.modules.ModuleAutoCrafting;
import com.craftaro.epichoppers.settings.Settings;
import com.craftaro.epichoppers.utils.Methods;
import com.craftaro.epichoppers.hopper.Hopper;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class GUICrafting extends CustomizableGui {
    public GUICrafting(ModuleAutoCrafting module, SongodaPlugin plugin, Hopper hopper, Player player) {
        super(plugin, "crafting");
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

        setButton("back", 8, GuiUtils.createButtonItem(XMaterial.ARROW.parseItem(),
                        plugin.getLocale().getMessage("general.nametag.back").getMessage()),
                (event) -> {
                    if (hopper.prepareForOpeningOverviewGui(event.player)) {
                        this.guiManager.showGUI(event.player, new GUIOverview(plugin, hopper, event.player));
                    }
                    setItem(module, hopper, player);
                }
        );

        setButton(13, module.getAutoCrafting(hopper),
                (event) -> module.setAutoCrafting(hopper, player, this.inventory.getItem(13)));

        setUnlocked(13);
    }

    public void setItem(ModuleAutoCrafting module, Hopper hopper, Player player) {
        module.setAutoCrafting(hopper, player, this.inventory.getItem(13));
    }
}
