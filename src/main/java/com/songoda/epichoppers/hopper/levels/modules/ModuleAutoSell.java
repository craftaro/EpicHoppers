package com.songoda.epichoppers.hopper.levels.modules;

import com.songoda.core.compatibility.CompatibleMaterial;
import com.songoda.core.hooks.EconomyManager;
import com.songoda.core.utils.TextUtils;
import com.songoda.epichoppers.EpicHoppers;
import com.songoda.epichoppers.gui.GUIAutoSellFilter;
import com.songoda.epichoppers.hopper.Filter;
import com.songoda.epichoppers.hopper.Hopper;
import com.songoda.epichoppers.settings.Settings;
import com.songoda.epichoppers.utils.Methods;
import com.songoda.epichoppers.utils.StorageContainerCache;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ModuleAutoSell extends Module {

    private final int timeOut;
    private final int hopperTickRate;
    private static final Map<Hopper, Boolean> cachedNotifications = new ConcurrentHashMap<>();

    public ModuleAutoSell(EpicHoppers plugin, int timeOut) {
        super(plugin);
        this.timeOut = timeOut * 20;
        this.hopperTickRate = Settings.HOP_TICKS.getInt();
    }

    @Override
    public String getName() {
        return "AutoSell";
    }

    @Override
    public void run(Hopper hopper, StorageContainerCache.Cache hopperCache) {

        int currentTime = getTime(hopper);

        if (currentTime == -9999) return;

        int subtract = getTime(hopper) - hopperTickRate;

        if (subtract <= 0) {
            int amountSold = 0;
            double totalValue = 0;

            if (!EconomyManager.isEnabled()) return;

            OfflinePlayer player = Bukkit.getOfflinePlayer(hopper.getPlacedBy());

            // -1
            for (int i = 0; i < hopperCache.cachedInventory.length; i++) {
                final ItemStack itemStack = hopperCache.cachedInventory[i];

                if (itemStack == null) continue;

                Filter filter = hopper.getFilter();

                if (filter.getAutoSellWhiteList().isEmpty()) {
                    // Check blacklist
                    if (filter.getAutoSellBlackList().stream().anyMatch(item -> Methods.isSimilarMaterial(itemStack, item)))
                        continue;
                } else {
                    // Check whitelist
                    if (filter.getAutoSellWhiteList().stream().noneMatch(item -> Methods.isSimilarMaterial(itemStack, item)))
                        continue;
                }

                // Get the value from config or ShopGuiPlus
                double value;
                if (Settings.AUTOSELL_SHOPGUIPLUS.getBoolean() && player.isOnline()) {
                    try {
                        ItemStack clone = itemStack.clone();
                        clone.setAmount(1);
                        value = net.brcdev.shopgui.ShopGuiPlusApi.getItemStackPriceSell(player.getPlayer(), clone);
                    } catch (Exception e) {
                        value = 0;
                    }
                } else
                    value = Settings.AUTOSELL_PRICES.getStringList().stream().filter(line -> CompatibleMaterial.valueOf(line.split(",")[0]) == CompatibleMaterial.getMaterial(itemStack)).findFirst()
                            .map(s -> Double.valueOf(s.split(",")[1])).orElse(0.0);

                if (value <= 0) continue;

                double sellingFor = value * itemStack.getAmount();

                totalValue += sellingFor;
                amountSold += itemStack.getAmount();
                hopperCache.removeItem(i);
            }

            if (totalValue != 0)
                EconomyManager.deposit(player, totalValue);
            if (totalValue != 0 && player.isOnline() && isNotifying(hopper)) {
                plugin.getLocale().getMessage("event.hopper.autosell")
                        .processPlaceholder("items", amountSold)
                        .processPlaceholder("amount", Methods.formatEconomy(totalValue)).sendPrefixedMessage(player.getPlayer());
            }

            modifyDataCache(hopper, "time", timeOut);
            return;
        }

        modifyDataCache(hopper, "time", subtract);
    }

    @Override
    public ItemStack getGUIButton(Hopper hopper) {
        ItemStack sellItem = CompatibleMaterial.SUNFLOWER.getItem();
        ItemMeta sellMeta = sellItem.getItemMeta();

        sellMeta.setDisplayName(EpicHoppers.getInstance().getLocale().getMessage("interface.hopper.selltitle").getMessage());
        ArrayList<String> loreSell = new ArrayList<>();

        String[] parts = EpicHoppers.getInstance().getLocale().getMessage("interface.hopper.selllore")
                .processPlaceholder("timeleft", getTime(hopper) == -9999 ? "\u221E" : (int) Math.floor(getTime(hopper) / 20))
                .processPlaceholder("state", isNotifying(hopper)).getMessage().split("\\|");

        for (String line : parts)
            loreSell.add(TextUtils.formatText(line));

        sellMeta.setLore(loreSell);
        sellItem.setItemMeta(sellMeta);
        return sellItem;
    }

    @Override
    public void runButtonPress(Player player, Hopper hopper, ClickType type) {
        if (type == ClickType.LEFT) {
            if (getTime(hopper) == -9999) {
                saveData(hopper, "time", timeOut);
            } else {
                saveData(hopper, "time", -9999);
            }
        } else if (type == ClickType.RIGHT) {
            setNotifying(hopper, !isNotifying(hopper));
        } else if (type == ClickType.SHIFT_LEFT || type == ClickType.SHIFT_RIGHT) {
            // Any shift click opens AutoSell filter configuration GUI
            hopper.setActivePlayer(player);
            EpicHoppers.getInstance().getGuiManager().showGUI(player, new GUIAutoSellFilter(EpicHoppers.getInstance(), hopper));
        }
    }

    @Override
    public List<Material> getBlockedItems(Hopper hopper) {
        return null;
    }

    @Override
    public String getDescription() {
        return EpicHoppers.getInstance().getLocale().getMessage("interface.hopper.autosell")
                .processPlaceholder("seconds", (int) Math.floor(timeOut / 20)).getMessage();
    }

    @Override
    public void clearData(Hopper hopper) {
        super.clearData(hopper);
        cachedNotifications.remove(hopper);
    }

    private boolean isNotifying(Hopper hopper) {
        Boolean enabled = cachedNotifications.get(hopper);
        if (enabled == null) {
            Object notifications = getData(hopper, "notifications");
            cachedNotifications.put(hopper, enabled = notifications != null && (boolean) notifications);
        }
        return enabled;
    }

    public void setNotifying(Hopper hopper, boolean enable) {
        saveData(hopper, "notifications", enable);
        cachedNotifications.put(hopper, enable);
    }

    private int getTime(Hopper hopper) {
        Object time = getData(hopper, "time");
        if (time == null) return -9999;
        return (int) time;
    }
}