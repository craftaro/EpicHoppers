package com.songoda.epichoppers.economy;

import com.songoda.epichoppers.EpicHoppers;
import org.black_ixx.playerpoints.PlayerPoints;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

public class PlayerPointsEconomy implements Economy {

    private final EpicHoppers plugin;

    private final PlayerPoints playerPoints;

    public PlayerPointsEconomy(EpicHoppers plugin) {
        this.plugin = plugin;

        this.playerPoints = (PlayerPoints) plugin.getServer().getPluginManager().getPlugin("PlayerPoints");
    }

    private int convertAmount(double amount) {
        return (int) Math.ceil(amount);
    }

    @Override
    public boolean hasBalance(OfflinePlayer player, double cost) {
        int amount = convertAmount(cost);
        return playerPoints.getAPI().look(player.getUniqueId()) >= amount;

    }

    @Override
    public boolean withdrawBalance(OfflinePlayer player, double cost) {
        int amount = convertAmount(cost);
        return playerPoints.getAPI().take(player.getUniqueId(), amount);

    }

    @Override
    public boolean deposit(OfflinePlayer player, double amount) {
        int amt = convertAmount(amount);
        return playerPoints.getAPI().give(player.getUniqueId(), amt);
    }
}
