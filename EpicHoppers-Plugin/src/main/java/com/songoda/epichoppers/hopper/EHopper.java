package com.songoda.epichoppers.hopper;

import com.songoda.epichoppers.EpicHoppersPlugin;
import com.songoda.epichoppers.api.CostType;
import com.songoda.epichoppers.api.hopper.Filter;
import com.songoda.epichoppers.api.hopper.Hopper;
import com.songoda.epichoppers.api.hopper.TeleportTrigger;
import com.songoda.epichoppers.api.hopper.levels.Level;
import com.songoda.epichoppers.gui.GUIOverview;
import com.songoda.epichoppers.player.PlayerData;
import com.songoda.epichoppers.utils.Debugger;
import com.songoda.epichoppers.utils.Methods;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Created by songoda on 3/14/2017.
 */
public class EHopper implements Hopper {

    private Location location;
    private Level level;
    private UUID lastPlayer;
    private UUID placedBy;
    private List<Location> linkedBlocks;
    private Filter filter;
    private TeleportTrigger teleportTrigger;
    private Material autoCrafting;
    private int autoSellTimer = 0;
    private boolean autoBreaking = false;

    public EHopper(Location location, Level level, UUID lastPlayer, UUID placedBy, List<Location> linkedBlocks, Filter filter, TeleportTrigger teleportTrigger, Material autoCrafting) {
        this.location = location;
        this.level = level;
        this.linkedBlocks = linkedBlocks;
        this.filter = filter;
        this.lastPlayer = lastPlayer;
        this.placedBy = placedBy;
        this.teleportTrigger = teleportTrigger;
        this.autoCrafting = autoCrafting;
    }

    public EHopper(Block block, Level level, UUID lastPlayer, UUID placedBy, List<Location> linkedBlocks, Filter filter, TeleportTrigger teleportTrigger, Material autoCrafting) {
        this(block.getLocation(), level, lastPlayer, placedBy, linkedBlocks, filter, teleportTrigger, autoCrafting);
    }

    public void overview(Player player) {
        try {
            if (lastPlayer != null && lastPlayer != player.getUniqueId()) {
                Bukkit.getPlayer(lastPlayer).closeInventory();
            }
            if (placedBy == null) placedBy = player.getUniqueId();

            EpicHoppersPlugin instance = EpicHoppersPlugin.getInstance();
            if (!player.hasPermission("epichoppers.overview")) return;
            new GUIOverview(instance, this, player);
        } catch (Exception e) {
            Debugger.runReport(e);
        }
    }

    public void upgrade(Player player, CostType type) {
        try {
            EpicHoppersPlugin instance = EpicHoppersPlugin.getInstance();
            if (instance.getLevelManager().getLevels().containsKey(this.level.getLevel() + 1)) {

                Level level = instance.getLevelManager().getLevel(this.level.getLevel() + 1);
                int cost = type == CostType.ECONOMY ? level.getCostEconomy() : level.getCostExperience();

                if (type == CostType.ECONOMY) {
                    if (instance.getServer().getPluginManager().getPlugin("Vault") != null) {
                        RegisteredServiceProvider<Economy> rsp = instance.getServer().getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class);
                        net.milkbowl.vault.economy.Economy econ = rsp.getProvider();
                        if (econ.has(player, cost)) {
                            econ.withdrawPlayer(player, cost);
                            upgradeFinal(level, player);
                        } else {
                            player.sendMessage(instance.getLocale().getMessage("event.upgrade.cannotafford"));
                        }
                    } else {
                        player.sendMessage("Vault is not installed.");
                    }
                } else if (type == CostType.EXPERIENCE) {
                    if (player.getLevel() >= cost || player.getGameMode() == GameMode.CREATIVE) {
                        if (player.getGameMode() != GameMode.CREATIVE) {
                            player.setLevel(player.getLevel() - cost);
                        }
                        upgradeFinal(level, player);
                    } else {
                        player.sendMessage(instance.getLocale().getMessage("event.upgrade.cannotafford"));
                    }
                }
            }
        } catch (Exception ex) {
            Debugger.runReport(ex);
        }
    }

    private void upgradeFinal(Level level, Player player) {
        try {
            EpicHoppersPlugin instance = EpicHoppersPlugin.getInstance();
            this.level = level;
            syncName();
            if (instance.getLevelManager().getHighestLevel() != level) {
                player.sendMessage(instance.getLocale().getMessage("event.upgrade.success", level.getLevel()));
            } else {
                player.sendMessage(instance.getLocale().getMessage("event.upgrade.maxed", level.getLevel()));
            }
            Location loc = location.clone().add(.5, .5, .5);
            player.getWorld().spawnParticle(org.bukkit.Particle.valueOf(instance.getConfig().getString("Main.Upgrade Particle Type")), loc, 200, .5, .5, .5);

            if (instance.getConfig().getBoolean("Main.Sounds Enabled")) {
                if (instance.getLevelManager().getHighestLevel() != level) {
                    player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.6F, 15.0F);
                } else {
                    player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 2F, 25.0F);

                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 2F, 25.0F);
                    Bukkit.getScheduler().scheduleSyncDelayedTask(instance, () -> player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1.2F, 35.0F), 5L);
                    Bukkit.getScheduler().scheduleSyncDelayedTask(instance, () -> player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1.8F, 35.0F), 10L);
                }
            }
        } catch (Exception ex) {
            Debugger.runReport(ex);
        }
    }

    private void syncName() {
        org.bukkit.block.Hopper hopper = (org.bukkit.block.Hopper)location.getBlock().getState();
        hopper.setCustomName(Methods.formatName(level.getLevel(), false));
        hopper.update(true);
    }

    public void timeout(Player player) {
        try {
            EpicHoppersPlugin instance = EpicHoppersPlugin.getInstance();
            Bukkit.getScheduler().scheduleSyncDelayedTask(instance, () -> {
                PlayerData playerData = instance.getPlayerDataManager().getPlayerData(player);
                if (playerData.getSyncType() != null) {
                    player.sendMessage(instance.getLocale().getMessage("event.hopper.synctimeout"));
                    playerData.setSyncType(null);
                }
            }, instance.getConfig().getLong("Main.Timeout When Syncing Hoppers"));
        } catch (Exception e) {
            Debugger.runReport(e);
        }
    }

    @Override
    public void link(Block toLink, boolean filtered, Player player) {
        try {
            EpicHoppersPlugin instance = EpicHoppersPlugin.getInstance();

            if (location.getWorld().equals(toLink.getLocation().getWorld())
                    && !player.hasPermission("EpicHoppers.Override")
                    && !player.hasPermission("EpicHoppers.Admin")
                    && location.distance(toLink.getLocation()) > level.getRange()) {
                player.sendMessage(instance.references.getPrefix() + instance.getLocale().getMessage("event.hopper.syncoutofrange"));
                return;
            }

            if (linkedBlocks.contains(toLink)) {
                player.sendMessage(instance.references.getPrefix() + instance.getLocale().getMessage("event.hopper.already"));
                return;
            }

            if (!filtered)
                this.linkedBlocks.add(toLink.getLocation());
            else {
                this.filter.setEndPoint(toLink.getLocation());
                player.sendMessage(instance.references.getPrefix() + instance.getLocale().getMessage("event.hopper.syncsuccess"));
                instance.getPlayerDataManager().getPlayerData(player).setSyncType(null);
                return;
            }
            this.lastPlayer = player.getUniqueId();

            if (level.getLinkAmount() > 1) {
                if (getLinkedBlocks().size() == level.getLinkAmount()) {
                    player.sendMessage(instance.references.getPrefix() + instance.getLocale().getMessage("event.hopper.syncdone"));
                    return;
                }
                player.sendMessage(instance.references.getPrefix() + instance.getLocale().getMessage("event.hopper.syncsuccessmore", level.getLinkAmount() - getLinkedBlocks().size()));
                return;
            }
            player.sendMessage(instance.references.getPrefix() + instance.getLocale().getMessage("event.hopper.syncsuccess"));
            instance.getPlayerDataManager().getPlayerData(player).setSyncType(null);

        } catch (Exception e) {
            Debugger.runReport(e);
        }
    }

    @Override
    public Location getLocation() {
        return location.clone();
    }

    @Override
    public World getWorld() {
        return location.getWorld();
    }

    @Override
    public int getX() {
        return location.getBlockX();
    }

    @Override
    public int getY() {
        return location.getBlockY();
    }

    @Override
    public int getZ() {
        return location.getBlockZ();
    }

    @Override
    public Level getLevel() {
        return level;
    }

    @Override
    public UUID getPlacedBy() {
        return placedBy;
    }

    @Override
    public UUID getLastPlayer() {
        return lastPlayer;
    }

    @Override
    public void setLastPlayer(UUID uuid) {
        lastPlayer = uuid;
    }

    @Override
    public Material getAutoCrafting() {
        return autoCrafting;
    }

    @Override
    public void setAutoCrafting(Material autoCrafting) {
        this.autoCrafting = autoCrafting;
    }

    @Override
    public TeleportTrigger getTeleportTrigger() {
        return teleportTrigger;
    }

    @Override
    public void setTeleportTrigger(TeleportTrigger teleportTrigger) {
        this.teleportTrigger = teleportTrigger;
    }

    public int getAutoSellTimer() {
        return autoSellTimer;
    }

    public void setAutoSellTimer(int autoSellTimer) {
        this.autoSellTimer = autoSellTimer;
    }

    public boolean isAutoBreaking() {
        return autoBreaking;
    }

    public void toggleAutoBreaking() {
        this.autoBreaking = !autoBreaking;
    }

    @Override
    public List<Location> getLinkedBlocks() {
        return new ArrayList<>(linkedBlocks);
    }

    @Override
    public void addLinkedBlock(Location block) {
        linkedBlocks.add(block);
    }

    @Override
    public void clearLinkedBlocks() {
        this.linkedBlocks.clear();
    }

    @Override
    public Filter getFilter() {
        return filter;
    }
}
