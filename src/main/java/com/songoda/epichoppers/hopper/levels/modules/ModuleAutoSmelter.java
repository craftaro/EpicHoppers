package com.songoda.epichoppers.hopper.levels.modules;

import com.songoda.core.compatibility.CompatibleMaterial;
import com.songoda.core.utils.TextUtils;
import com.songoda.epichoppers.EpicHoppers;
import com.songoda.epichoppers.gui.GUISmeltable;
import com.songoda.epichoppers.hopper.Hopper;
import com.songoda.epichoppers.settings.Settings;
import com.songoda.epichoppers.utils.StorageContainerCache;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ModuleAutoSmelter extends Module {

    private final int timeOut;
    private final int hopperTickRate;

    public ModuleAutoSmelter(EpicHoppers plugin, int timeOut) {
        super(plugin);
        this.timeOut = timeOut * 20;
        this.hopperTickRate = Settings.HOP_TICKS.getInt();
    }

    @Override
    public String getName() {
        return "AutoSmelter";
    }

    @Override
    public void run(Hopper hopper, StorageContainerCache.Cache hopperCache) {
        if (!isEnabled(hopper)) return;

        int currentTime = getTime(hopper);

        if (currentTime == -9999) return;

        int subtract = currentTime - hopperTickRate;


        if (subtract <= 0) {
            for (int i = 0; i < 5; i++) {
                final ItemStack itemStack = hopperCache.cachedInventory[i];
                if (itemStack == null) continue;
                CompatibleMaterial material = CompatibleMaterial.getMaterial(itemStack);
                if (!isSmeltable(hopper, material)) continue;
                CompatibleMaterial result = CompatibleMaterial.getMaterial(itemStack).getBurnResult();

                if (hopperCache.addItem(result.getItem())) {
                    if (itemStack.getAmount() == 1) {
                        hopperCache.setItem(i, null);
                    } else {
                        itemStack.setAmount(itemStack.getAmount() - 1);
                        hopperCache.dirty = hopperCache.cacheChanged[i] = true;
                    }
                    break;
                }
            }

            modifyDataCache(hopper, "time", timeOut);
            return;
        }

        modifyDataCache(hopper, "time", subtract);
    }

    @Override
    public ItemStack getGUIButton(Hopper hopper) {
        ItemStack block = CompatibleMaterial.IRON_INGOT.getItem();
        ItemMeta blockmeta = block.getItemMeta();
        blockmeta.setDisplayName(plugin.getLocale().getMessage("interface.hopper.smelttitle").getMessage());
        ArrayList<String> loreblock = new ArrayList<>();
        String[] parts = plugin.getLocale().getMessage("interface.hopper.smeltlore").processPlaceholder("timeleft",
                getTime(hopper) == -9999 ? "\u221E" : (int) Math.floor(getTime(hopper) / 20.0)).processPlaceholder("enabled",
                isEnabled(hopper) ? EpicHoppers.getInstance().getLocale().getMessage("general.word.enabled").getMessage()
                        : EpicHoppers.getInstance().getLocale().getMessage("general.word.disabled").getMessage()).getMessage().split("\\|");
        for (String line : parts) {
            loreblock.add(TextUtils.formatText(line));
        }
        blockmeta.setLore(loreblock);
        block.setItemMeta(blockmeta);
        return block;
    }

    public void runButtonPress(Player player, Hopper hopper, ClickType type) {
        if (type == ClickType.LEFT) {
            hopper.setActivePlayer(player);
            EpicHoppers.getInstance().getGuiManager().showGUI(player, new GUISmeltable(this, plugin, hopper));
        } else if (type == ClickType.RIGHT)
            toggleEnabled(hopper);
    }

    @Override
    public List<Material> getBlockedItems(Hopper hopper) {
        if (getTime(hopper) == -9999)
            return Collections.emptyList();

        List<Material> blockedItems = new ArrayList<>();
        for (CompatibleMaterial material : CompatibleMaterial.values())
            if (material.getBurnResult() != null && isSmeltable(hopper, material))
                blockedItems.add(material.getMaterial());

        return blockedItems;
    }

    @Override
    public String getDescription() {
        return plugin.getLocale().getMessage("interface.hopper.autosmelt")
                .processPlaceholder("ticks", (int) Math.floor(timeOut / 20.0)).getMessage();
    }

    private int getTime(Hopper hopper) {
        Object time = getData(hopper, "time");
        if (time == null) return -9999;
        return (int) time;
    }

    private boolean isEnabled(Hopper hopper) {
        Object obj = getData(hopper, "time");
        if (obj == null) return false;
        return ((int) obj) != -9999;
    }

    public boolean isSmeltable(Hopper hopper, CompatibleMaterial material) {
        Object obj = getData(hopper, material.name());
        if (obj == null) return false;

        return ((boolean) obj);
    }

    private void toggleEnabled(Hopper hopper) {
        if (isEnabled(hopper))
            saveData(hopper, "time", -9999);
        else
            saveData(hopper, "time", timeOut);
    }

    public void toggleSmeltable(Hopper hopper, CompatibleMaterial material) {
        saveData(hopper, material.name(), !isSmeltable(hopper, material));
    }
}