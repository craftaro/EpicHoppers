package com.songoda.epichoppers.gui;

import com.songoda.core.compatibility.CompatibleMaterial;
import com.songoda.core.gui.CustomizableGui;
import com.songoda.core.gui.GuiUtils;
import com.songoda.core.utils.TextUtils;
import com.songoda.epichoppers.EpicHoppers;
import com.songoda.epichoppers.hopper.Hopper;
import com.songoda.epichoppers.hopper.levels.modules.ModuleAutoSmelter;
import com.songoda.epichoppers.settings.Settings;
import com.songoda.epichoppers.utils.Methods;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class GUISmeltable extends CustomizableGui {

    private final EpicHoppers plugin;
    private final Hopper hopper;
    private int maxPages;
    private ModuleAutoSmelter moduleAutoSmelter;

    private static List<CompatibleMaterial> burnables = Arrays.stream(CompatibleMaterial.values())
            .filter(m -> m.getBurnResult() != null).collect(Collectors.toList());

    public GUISmeltable(ModuleAutoSmelter moduleAutoSmelter, EpicHoppers plugin, Hopper hopper) {
        super(plugin, "smeltable");
        this.plugin = plugin;
        this.hopper = hopper;
        this.moduleAutoSmelter = moduleAutoSmelter;

        int smeltables = burnables.size();

        maxPages = (int) Math.ceil(smeltables / 32.);

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

        int smeltableIndex = page == 1 ? 0 : 32 * (page - 1);

        for (int i = 9; i < 45; i++) {
            if (i == 9 || i == 17 || i == 44 || i == 36) continue;
            setItem(i, null);
            clearActions(i);
            if (smeltableIndex >= (burnables.size() - 1)) continue;
            CompatibleMaterial burnable = burnables.get(smeltableIndex);
            setButton(i, getItemStack(burnable, moduleAutoSmelter.isSmeltable(hopper, burnable)), (event) -> {
                moduleAutoSmelter.toggleSmeltable(hopper, burnable);
                setItem(event.slot, getItemStack(burnable, moduleAutoSmelter.isSmeltable(hopper, burnable)));
            });
            smeltableIndex++;
        }

        clearActions(51);
        if (page < maxPages) {
            setButton("next", 51, GuiUtils.createButtonItem(CompatibleMaterial.ARROW,
                    plugin.getLocale().getMessage("general.nametag.next").getMessage()),
                    (event) -> {
                        page++;
                        showPage();
                    });
        }

        clearActions(47);
        if (page > 1) {
            setButton("back", 47, GuiUtils.createButtonItem(CompatibleMaterial.ARROW,
                    plugin.getLocale().getMessage("general.nametag.back").getMessage()),
                    (event) -> {
                        page--;
                        showPage();
                    });
        }

        setButton("exit", 49, GuiUtils.createButtonItem(CompatibleMaterial.OAK_DOOR,
                plugin.getLocale().getMessage("general.nametag.exit").getMessage()),
                (event) -> hopper.overview(plugin.getGuiManager(), event.player));
    }

    public ItemStack getItemStack(CompatibleMaterial material, boolean enabled) {
        ItemStack item = material.getItem();
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(TextUtils.formatText("&e" + material.name()));
        meta.setLore(Arrays.asList(TextUtils.formatText("   &7-> &e" + material.getBurnResult().name()),
                TextUtils.formatText("&7Enabled: &6" + String.valueOf(enabled).toLowerCase() + "&7."),
                "",
                plugin.getLocale().getMessage("interface.hopper.toggle").getMessage()));
        item.setItemMeta(meta);

        return item;
    }
}
