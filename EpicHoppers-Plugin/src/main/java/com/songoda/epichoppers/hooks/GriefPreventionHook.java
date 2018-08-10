package com.songoda.epichoppers.hooks;

import com.songoda.epichoppers.EpicHoppersPlugin;
import com.songoda.epichoppers.utils.Debugger;
import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;

/**
 * Created by songoda on 3/17/2017.
 */
public class GriefPreventionHook extends Hook {

    public GriefPreventionHook() {
        super("GriefPrevention");
        EpicHoppersPlugin plugin = EpicHoppersPlugin.getInstance();
        if (isEnabled())
            plugin.hooks.GriefPreventionHook = this;
    }

    @Override
    public boolean canBuild(Player p, Location location) {
        try {
            if (hasBypass(p))
                return true;

            Claim claim = GriefPrevention.instance.dataStore.getClaimAt(location, false, null);
            return claim != null && claim.allowBuild(p, Material.STONE) == null;

        } catch (Exception e) {
            Debugger.runReport(e);
        }
        return false;
    }
}