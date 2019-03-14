package com.songoda.epichoppers.hook;

import com.songoda.epichoppers.EpicHoppersPlugin;
import com.songoda.epichoppers.hook.hooks.*;
import com.songoda.epichoppers.utils.ConfigWrapper;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class HookManager {

    private final EpicHoppersPlugin plugin;

    private ConfigWrapper hooksFile;
    private List<ProtectionPluginHook> registeredHooks = new ArrayList<>();

    public HookManager(EpicHoppersPlugin plugin) {
        this.plugin = plugin;
        this.hooksFile = new ConfigWrapper(plugin, "", "hooks.yml");
        this.hooksFile.createNewFile("Loading Hooks File", plugin.getDescription().getName() + " Hooks File");

        PluginManager pluginManager = Bukkit.getPluginManager();

        // Register default hooks
        if (pluginManager.isPluginEnabled("ASkyBlock")) this.register(HookASkyBlock::new);
        if (pluginManager.isPluginEnabled("FactionsFramework")) this.register(HookFactions::new);
        if (pluginManager.isPluginEnabled("GriefPrevention")) this.register(HookGriefPrevention::new);
        if (pluginManager.isPluginEnabled("Kingdoms")) this.register(HookKingdoms::new);
        if (pluginManager.isPluginEnabled("PlotSquared")) this.register(HookPlotSquared::new);
        if (pluginManager.isPluginEnabled("RedProtect")) this.register(HookRedProtect::new);
        if (pluginManager.isPluginEnabled("Towny")) this.register(HookTowny::new);
        if (pluginManager.isPluginEnabled("USkyBlock")) this.register(HookUSkyBlock::new);
        if (pluginManager.isPluginEnabled("SkyBlock")) this.register(HookSkyBlockEarth::new);
        if (pluginManager.isPluginEnabled("WorldGuard")) this.register(HookWorldGuard::new);
    }

    public boolean canBuild(Player player, Location location) {
        if (player.hasPermission(EpicHoppersPlugin.getInstance().getDescription().getName() + ".bypass")) return true;

        for (ProtectionPluginHook hook : registeredHooks) {
            if (!hook.isInClaim(location)) continue;

            if (!hook.canBuild(player, location)) return false;
        }

        return true;
    }

    public boolean isInClaim(HookType hookType, String name, Location l) {
        List<ProtectionPluginHook> hooks = registeredHooks.stream().filter(hook -> hook.getHookType() == hookType).collect(Collectors.toList());
        for (ProtectionPluginHook hook : hooks) {
            if (hook.isInClaim(l, name)) {
                return true;
            }
        }
        return false;
    }

    public String getClaimId(HookType hookType, String name) {
        List<ProtectionPluginHook> hooks = registeredHooks.stream().filter(hook -> hook.getHookType() == hookType).collect(Collectors.toList());
        for (ProtectionPluginHook hook : hooks) {
            return hook.getClaimID(name);
        }
        return null;
    }


    private ProtectionPluginHook register(Supplier<ProtectionPluginHook> hookSupplier) {
        return this.registerProtectionHook(hookSupplier.get());
    }

    public ProtectionPluginHook registerProtectionHook(ProtectionPluginHook hook) {
        if (hook != null && hook.getPlugin() != null) return null;

        JavaPlugin hookPlugin = hook.getPlugin();
        for (ProtectionPluginHook existingHook : registeredHooks) {
            if (existingHook.getPlugin().equals(hookPlugin)) {
                throw new IllegalArgumentException("Hook already registered");
            }
        }

        this.hooksFile.getConfig().addDefault("hooks." + hookPlugin.getName(), true);
        if (!hooksFile.getConfig().getBoolean("hooks." + hookPlugin.getName(), true)) return null;
        this.hooksFile.getConfig().options().copyDefaults(true);
        this.hooksFile.saveConfig();

        this.registeredHooks.add(hook);
        plugin.getLogger().info("Registered protection hook for plugin: " + hook.getPlugin().getName());
        return hook;
    }
}
