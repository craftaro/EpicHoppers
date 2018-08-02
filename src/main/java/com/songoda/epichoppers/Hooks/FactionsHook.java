package com.songoda.epichoppers.Hooks;

import com.songoda.epichoppers.EpicHoppers;
import com.songoda.epichoppers.Utils.Debugger;
import me.markeh.factionsframework.entities.FPlayer;
import me.markeh.factionsframework.entities.FPlayers;
import me.markeh.factionsframework.entities.Faction;
import me.markeh.factionsframework.entities.Factions;
import org.bukkit.Location;
import org.bukkit.entity.Player;

/**
 * Created by songoda on 3/17/2017.
 */
public class FactionsHook extends Hook {

    public FactionsHook() {
        super("Factions");
        EpicHoppers plugin = EpicHoppers.getInstance();
        if (isEnabled())
            plugin.hooks.GriefPreventionHook = this;
    }

    @Override
    public boolean canBuild(Player p, Location location) {
        try {
            FPlayer fp = FPlayers.getBySender(p);

            Faction faction = Factions.getFactionAt(location);

            return (fp.getFaction().equals(faction) || faction.isNone());
        } catch (Exception e) {
            Debugger.runReport(e);
        }
        return false;
    }

    @Override
    public boolean isInClaim(String id, Location location) {
        Faction faction = Factions.getFactionAt(location);

        return faction.getId().equals(id);
    }

    @Override
    public String getClaimId(String name) {
        try {
            Faction faction = Factions.getByName(name, "");

            return faction.getId();
        } catch (Exception ignore) {
        }
        return null;
    }

}
