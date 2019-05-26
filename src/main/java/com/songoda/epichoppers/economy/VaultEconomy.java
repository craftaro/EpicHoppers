package com.songoda.epichoppers.economy;

import com.songoda.epichoppers.EpicHoppers;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

public class VaultEconomy implements Economy {

    private final EpicHoppers plugin;

    private final net.milkbowl.vault.economy.Economy vault;

    public VaultEconomy(EpicHoppers plugin) {
        this.plugin = plugin;

        this.vault = plugin.getServer().getServicesManager().
                getRegistration(net.milkbowl.vault.economy.Economy.class).getProvider();
    }

    @Override
    public boolean hasBalance(OfflinePlayer player, double cost) {
        return vault.has(player, cost);
    }

    @Override
    public boolean withdrawBalance(OfflinePlayer player, double cost) {
        return vault.withdrawPlayer(player, cost).transactionSuccess();
    }

    @Override
    public boolean deposit(OfflinePlayer player, double amount) {
        return vault.depositPlayer(player, amount).transactionSuccess();
    }
}
