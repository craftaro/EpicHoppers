package com.songoda.epicspawners.hook.hooks;

import com.intellectualcrafters.plot.api.PlotAPI;
import com.plotsquared.bukkit.BukkitMain;
import com.songoda.epicspawners.hook.HookType;
import com.songoda.epicspawners.hook.ProtectionPluginHook;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class HookPlotSquared implements ProtectionPluginHook {

    private final PlotAPI plotSquared;

    public HookPlotSquared() {
        this.plotSquared = new PlotAPI();
    }

    @Override
    public JavaPlugin getPlugin() { // BukkitMain? Really?
        return JavaPlugin.getPlugin(BukkitMain.class);
    }

    @Override
    public HookType getHookType() {
        return HookType.REGULAR;
    }

    @Override
    public boolean canBuild(Player player, Location location) {
        return plotSquared.getPlot(location) != null && plotSquared.isInPlot(player)
                && plotSquared.getPlot(location) == plotSquared.getPlot(player);
    }

    @Override
    public boolean isInClaim(Location location) {
        return plotSquared.getPlot(location) != null;
    }

    @Override
    public boolean isInClaim(Location location, String id) {
        return false;
    }

    @Override
    public String getClaimID(String name) {
        return null;
    }

}