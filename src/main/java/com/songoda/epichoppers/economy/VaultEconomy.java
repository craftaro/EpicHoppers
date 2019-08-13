package com.songoda.epichoppers.economy;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;

public class VaultEconomy implements Economy {
    private final net.milkbowl.vault.economy.Economy vault;

    public VaultEconomy() {
        // separate this out to weed out some conflicts in 1.8
        RegisteredServiceProvider rsp = Bukkit.getServicesManager().
                getRegistration(net.milkbowl.vault.economy.Economy.class);
        this.vault = (net.milkbowl.vault.economy.Economy) (rsp == null ? null : rsp.getProvider());
    }

    public boolean isLoaded() {
        return vault != null;
    }

    @Override
    public boolean hasBalance(OfflinePlayer player, double cost) {
        return vault != null && vault.has(player, cost);
    }

    @Override
    public boolean withdrawBalance(OfflinePlayer player, double cost) {
        return vault != null && vault.withdrawPlayer(player, cost).transactionSuccess();
    }

    @Override
    public boolean deposit(OfflinePlayer player, double amount) {
        return vault != null && vault.depositPlayer(player, amount).transactionSuccess();
    }
}
