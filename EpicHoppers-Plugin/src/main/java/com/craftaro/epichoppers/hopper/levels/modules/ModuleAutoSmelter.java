package com.craftaro.epichoppers.hopper.levels.modules;

import com.craftaro.core.SongodaPlugin;
import com.craftaro.core.compatibility.CompatibleMaterial;
import com.craftaro.core.gui.GuiManager;
import com.craftaro.core.third_party.com.cryptomorin.xseries.XMaterial;
import com.craftaro.core.utils.TextUtils;
import com.craftaro.epichoppers.settings.Settings;
import com.craftaro.epichoppers.gui.GUISmeltable;
import com.craftaro.epichoppers.hopper.Hopper;
import com.craftaro.epichoppers.utils.StorageContainerCache;
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

    public ModuleAutoSmelter(SongodaPlugin plugin, GuiManager guiManager, int timeOut) {
        super(plugin, guiManager);
        this.timeOut = timeOut * 20;
        this.hopperTickRate = Settings.HOP_TICKS.getInt();
    }

    @Override
    public String getName() {
        return "AutoSmelter";
    }

    @Override
    public void run(Hopper hopper, StorageContainerCache.Cache hopperCache) {
        if (!isEnabled(hopper)) {
            return;
        }

        int currentTime = getTime(hopper);
        if (currentTime == -9999) {
            return;
        }

        int subtract = currentTime - this.hopperTickRate;


        if (subtract <= 0) {
            for (int i = 0; i < 5; i++) {
                final ItemStack itemStack = hopperCache.cachedInventory[i];
                if (itemStack == null) {
                    continue;
                }
                XMaterial material = CompatibleMaterial.getMaterial(itemStack.getType()).get();
                if (!isSmeltable(hopper, material)) {
                    continue;
                }
                XMaterial input = CompatibleMaterial.getMaterial(itemStack.getType()).get();
                ItemStack result = CompatibleMaterial.getFurnaceResult(input);

                if (hopperCache.addItem(result)) {
                    if (itemStack.getAmount() == 1) {
                        hopperCache.setItem(i, null);
                    } else {
                        itemStack.setAmount(itemStack.getAmount() - 1);
                        hopperCache.dirty = hopperCache.cacheChanged[i] = true;
                    }
                    break;
                }
            }

            modifyDataCache(hopper, "time", this.timeOut);
            return;
        }

        modifyDataCache(hopper, "time", subtract);
    }

    @Override
    public ItemStack getGUIButton(Hopper hopper) {
        ItemStack block = XMaterial.IRON_INGOT.parseItem();
        ItemMeta blockMeta = block.getItemMeta();
        blockMeta.setDisplayName(this.plugin.getLocale().getMessage("interface.hopper.smelttitle").getMessage());
        ArrayList<String> loreBlock = new ArrayList<>();
        String[] parts = this.plugin.getLocale().getMessage("interface.hopper.smeltlore")
                .processPlaceholder("timeleft", getTime(hopper) == -9999 ? "∞" : (int) Math.floor(getTime(hopper) / 20.0))
                .processPlaceholder("enabled", isEnabled(hopper) ?
                        this.plugin.getLocale().getMessage("general.word.enabled").getMessage() :
                        this.plugin.getLocale().getMessage("general.word.disabled").getMessage()
                )
                .getMessage()
                .split("\\|");
        for (String line : parts) {
            loreBlock.add(TextUtils.formatText(line));
        }
        blockMeta.setLore(loreBlock);
        block.setItemMeta(blockMeta);
        return block;
    }

    public void runButtonPress(Player player, Hopper hopper, ClickType type) {
        if (type == ClickType.LEFT) {
            hopper.setActivePlayer(player);
            this.guiManager.showGUI(player, new GUISmeltable(this, this.plugin, hopper));
        } else if (type == ClickType.RIGHT) {
            toggleEnabled(hopper);
        }
    }

    @Override
    public List<Material> getBlockedItems(Hopper hopper) {
        if (getTime(hopper) == -9999) {
            return Collections.emptyList();
        }

        List<Material> blockedItems = new ArrayList<>();
        for (XMaterial material : XMaterial.values()) {
            if (CompatibleMaterial.getFurnaceResult(material) != null && isSmeltable(hopper, material)) {
                blockedItems.add(material.parseMaterial());
            }
        }

        return blockedItems;
    }

    @Override
    public String getDescription() {
        return this.plugin.getLocale().getMessage("interface.hopper.autosmelt")
                .processPlaceholder("ticks", (int) Math.floor(this.timeOut / 20.0)).getMessage();
    }

    private int getTime(Hopper hopper) {
        Object time = getData(hopper, "time");
        if (time == null) {
            return -9999;
        }
        return (int) time;
    }

    private boolean isEnabled(Hopper hopper) {
        Object obj = getData(hopper, "time");
        if (obj == null) {
            return false;
        }
        return ((int) obj) != -9999;
    }

    public boolean isSmeltable(Hopper hopper, XMaterial material) {
        Object obj = getData(hopper, material.name());
        if (obj == null) {
            return false;
        }

        return ((boolean) obj);
    }

    private void toggleEnabled(Hopper hopper) {
        if (isEnabled(hopper)) {
            saveData(hopper, "time", -9999);
        } else {
            saveData(hopper, "time", this.timeOut);
        }
    }

    public void toggleSmeltable(Hopper hopper, XMaterial material) {
        saveData(hopper, material.name(), !isSmeltable(hopper, material));
    }
}
