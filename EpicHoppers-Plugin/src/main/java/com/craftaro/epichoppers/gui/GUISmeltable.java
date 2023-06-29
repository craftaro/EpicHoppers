package com.craftaro.epichoppers.gui;

import com.craftaro.core.SongodaPlugin;
import com.craftaro.core.compatibility.CompatibleMaterial;
import com.craftaro.core.gui.CustomizableGui;
import com.craftaro.core.gui.GuiUtils;
import com.craftaro.core.third_party.com.cryptomorin.xseries.XMaterial;
import com.craftaro.core.utils.TextUtils;
import com.craftaro.epichoppers.hopper.levels.modules.ModuleAutoSmelter;
import com.craftaro.epichoppers.settings.Settings;
import com.craftaro.epichoppers.utils.Methods;
import com.craftaro.epichoppers.hopper.Hopper;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class GUISmeltable extends CustomizableGui {
    private final SongodaPlugin plugin;
    private final Hopper hopper;
    private final int maxPages;
    private final ModuleAutoSmelter moduleAutoSmelter;

    private static final List<XMaterial> BURNABLES = Arrays
            .stream(XMaterial.values())
            .filter(material -> CompatibleMaterial.getFurnaceResult(material) != null)
            .collect(Collectors.toList());

    public GUISmeltable(ModuleAutoSmelter moduleAutoSmelter, SongodaPlugin plugin, Hopper hopper) {
        super(plugin, "smeltable");
        this.plugin = plugin;
        this.hopper = hopper;
        this.moduleAutoSmelter = moduleAutoSmelter;

        int smeltables = BURNABLES.size();

        this.maxPages = (int) Math.ceil(smeltables / 32.);

        setTitle(Methods.formatName(hopper.getLevel().getLevel()) + TextUtils.formatText(" &7-&f Smelting"));
        setRows(6);

        this.setOnPage((event) -> showPage());
        showPage();

        this.setOnClose((event) -> hopper.setActivePlayer(null));
    }

    void showPage() {
        ItemStack glass1 = GuiUtils.getBorderItem(Settings.GLASS_TYPE_1.getMaterial());
        ItemStack glass2 = GuiUtils.getBorderItem(Settings.GLASS_TYPE_2.getMaterial());
        ItemStack glass3 = GuiUtils.getBorderItem(Settings.GLASS_TYPE_3.getMaterial());

        setDefaultItem(glass1);

        mirrorFill("mirrorfill_1", 0, 0, true, true, glass2);
        mirrorFill("mirrorfill_2", 0, 1, true, true, glass2);
        mirrorFill("mirrorfill_3", 0, 2, true, true, glass3);
        mirrorFill("mirrorfill_4", 1, 0, true, true, glass2);

        int smeltableIndex = this.page == 1 ? 0 : 32 * (this.page - 1);

        for (int i = 9; i < 45; i++) {
            if (i == 9 || i == 17 || i == 44 || i == 36) {
                continue;
            }

            setItem(i, null);
            clearActions(i);
            if (smeltableIndex >= (BURNABLES.size() - 1)) {
                continue;
            }

            XMaterial burnable = BURNABLES.get(smeltableIndex);
            setButton(i, getItemStack(burnable, this.moduleAutoSmelter.isSmeltable(this.hopper, burnable)), (event) -> {
                this.moduleAutoSmelter.toggleSmeltable(this.hopper, burnable);
                setItem(event.slot, getItemStack(burnable, this.moduleAutoSmelter.isSmeltable(this.hopper, burnable)));
            });
            smeltableIndex++;
        }

        clearActions(51);
        if (this.page < this.maxPages) {
            setButton("next", 51, GuiUtils.createButtonItem(XMaterial.ARROW,
                            this.plugin.getLocale().getMessage("general.nametag.next").getMessage()),
                    (event) -> {
                        this.page++;
                        showPage();
                    });
        }

        clearActions(47);
        if (this.page > 1) {
            setButton("back", 47, GuiUtils.createButtonItem(XMaterial.ARROW,
                            this.plugin.getLocale().getMessage("general.nametag.back").getMessage()),
                    (event) -> {
                        this.page--;
                        showPage();
                    });
        }

        setButton("exit", 49, GuiUtils.createButtonItem(XMaterial.OAK_DOOR,
                        this.plugin.getLocale().getMessage("general.nametag.exit").getMessage()),
                (event) -> {
                    if (this.hopper.prepareForOpeningOverviewGui(event.player)) {
                        this.guiManager.showGUI(event.player, new GUIOverview(this.plugin, this.hopper, event.player));
                    }
                });
    }

    public ItemStack getItemStack(XMaterial material, boolean enabled) {
        ItemStack item = material.parseItem();
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(TextUtils.formatText("&e" + material.name()));
        meta.setLore(Arrays.asList(TextUtils.formatText("   &7-> &e" + CompatibleMaterial.getFurnaceResult(material).getType().name()),
                TextUtils.formatText("&7Enabled: &6" + String.valueOf(enabled).toLowerCase() + "&7."),
                "",
                this.plugin.getLocale().getMessage("interface.hopper.toggle").getMessage()));
        item.setItemMeta(meta);

        return item;
    }
}
