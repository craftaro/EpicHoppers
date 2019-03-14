package com.songoda.epicspawners.hook.hooks;

import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.songoda.epicspawners.hook.HookType;
import com.songoda.epicspawners.hook.ProtectionPluginHook;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class HookWorldGuard implements ProtectionPluginHook {

    private final WorldGuardPlugin worldGuard;

    public HookWorldGuard() {
        this.worldGuard = WorldGuardPlugin.inst();
    }

    @Override
    public JavaPlugin getPlugin() {
        return worldGuard;
    }

    @Override
    public HookType getHookType() {
        return HookType.REGULAR;
    }

    @Override
    public boolean canBuild(Player player, Location location) {
        return worldGuard.canBuild(player, location);
    }

    @Override
    public boolean isInClaim(Location location) {
        return true;
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