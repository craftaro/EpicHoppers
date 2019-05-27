package com.songoda.epichoppers.hopper;

import com.songoda.epichoppers.EpicHoppers;
import com.songoda.epichoppers.gui.GUIOverview;
import com.songoda.epichoppers.hopper.levels.Level;
import com.songoda.epichoppers.player.PlayerData;
import com.songoda.epichoppers.utils.CostType;
import com.songoda.epichoppers.utils.Methods;
import com.songoda.epichoppers.utils.ServerVersion;
import com.songoda.epichoppers.utils.TeleportTrigger;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Created by songoda on 3/14/2017.
 */
public class Hopper {

    private Location location;
    private Level level;
    private UUID lastPlayer;
    private UUID placedBy;
    private List<Location> linkedBlocks;
    private Filter filter;
    private TeleportTrigger teleportTrigger;
    private ItemStack autoCrafting;
    private int autoSellTimer = 0;
    private boolean autoBreaking = false;

    public Hopper(Location location, Level level, UUID lastPlayer, UUID placedBy, List<Location> linkedBlocks, Filter filter, TeleportTrigger teleportTrigger, ItemStack autoCrafting) {
        this.location = location;
        this.level = level;
        this.linkedBlocks = linkedBlocks;
        this.filter = filter;
        this.lastPlayer = lastPlayer;
        this.placedBy = placedBy;
        this.teleportTrigger = teleportTrigger;
        this.autoCrafting = autoCrafting;
    }

    public Hopper(Block block, Level level, UUID lastPlayer, UUID placedBy, List<Location> linkedBlocks, Filter filter, TeleportTrigger teleportTrigger, ItemStack autoCrafting) {
        this(block.getLocation(), level, lastPlayer, placedBy, linkedBlocks, filter, teleportTrigger, autoCrafting);
    }

    public void overview(Player player) {
            if (lastPlayer != null
                    && lastPlayer != player.getUniqueId()
                    && Bukkit.getPlayer(lastPlayer) != null) {
                Bukkit.getPlayer(lastPlayer).closeInventory();
            }
            if (placedBy == null) placedBy = player.getUniqueId();

        EpicHoppers instance = EpicHoppers.getInstance();
            if (!player.hasPermission("epichoppers.overview")) return;
            new GUIOverview(instance, this, player);
    }

    public void upgrade(Player player, CostType type) {
        EpicHoppers plugin = EpicHoppers.getInstance();
        if (plugin.getLevelManager().getLevels().containsKey(this.level.getLevel() + 1)) {

            Level level = plugin.getLevelManager().getLevel(this.level.getLevel() + 1);
                int cost = type == CostType.ECONOMY ? level.getCostEconomy() : level.getCostExperience();

                if (type == CostType.ECONOMY) {
                    if (plugin.getEconomy() == null) {
                        player.sendMessage("Economy not enabled.");
                        return;
                    }
                    if (!plugin.getEconomy().hasBalance(player, cost)) {
                        player.sendMessage(plugin.references.getPrefix() + plugin.getInstance().getLocale().getMessage("event.upgrade.cannotafford"));
                        return;
                    }
                    upgradeFinal(level, player);
                } else if (type == CostType.EXPERIENCE) {
                    if (player.getLevel() >= cost || player.getGameMode() == GameMode.CREATIVE) {
                        if (player.getGameMode() != GameMode.CREATIVE) {
                            player.setLevel(player.getLevel() - cost);
                        }
                        upgradeFinal(level, player);
                    } else {
                        player.sendMessage(plugin.references.getPrefix() + plugin.getLocale().getMessage("event.upgrade.cannotafford"));
                    }
                }
            }
    }

    private void upgradeFinal(Level level, Player player) {
        EpicHoppers instance = EpicHoppers.getInstance();
            this.level = level;
            syncName();
            if (instance.getLevelManager().getHighestLevel() != level) {
                player.sendMessage(instance.getLocale().getMessage("event.upgrade.success", level.getLevel()));
            } else {
                player.sendMessage(instance.getLocale().getMessage("event.upgrade.maxed", level.getLevel()));
            }
            Location loc = location.clone().add(.5, .5, .5);

            if (!instance.isServerVersionAtLeast(ServerVersion.V1_12)) return;

            player.getWorld().spawnParticle(org.bukkit.Particle.valueOf(instance.getConfig().getString("Main.Upgrade Particle Type")), loc, 200, .5, .5, .5);

            if (instance.getLevelManager().getHighestLevel() != level) {
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.6F, 15.0F);
            } else {
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 2F, 25.0F);

                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 2F, 25.0F);
                Bukkit.getScheduler().scheduleSyncDelayedTask(instance, () -> player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1.2F, 35.0F), 5L);
                Bukkit.getScheduler().scheduleSyncDelayedTask(instance, () -> player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1.8F, 35.0F), 10L);
            }
    }

    private void syncName() {
        org.bukkit.block.Hopper hopper = (org.bukkit.block.Hopper)location.getBlock().getState();
        if (EpicHoppers.getInstance().isServerVersionAtLeast(ServerVersion.V1_10))
            hopper.setCustomName(Methods.formatName(level.getLevel(), false));
            hopper.update(true);
    }

    public void timeout(Player player) {
        EpicHoppers instance = EpicHoppers.getInstance();
            Bukkit.getScheduler().scheduleSyncDelayedTask(instance, () -> {
                PlayerData playerData = instance.getPlayerDataManager().getPlayerData(player);
                if (playerData.getSyncType() != null) {
                    player.sendMessage(instance.getLocale().getMessage("event.hopper.synctimeout"));
                    playerData.setSyncType(null);
                }
            }, instance.getConfig().getLong("Main.Timeout When Syncing Hoppers"));
    }

    public void link(Block toLink, boolean filtered, Player player) {
        EpicHoppers instance = EpicHoppers.getInstance();

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
    }

    public Location getLocation() {
        return location.clone();
    }

    public World getWorld() {
        return location.getWorld();
    }

    public int getX() {
        return location.getBlockX();
    }

    public int getY() {
        return location.getBlockY();
    }

    public int getZ() {
        return location.getBlockZ();
    }

    public Level getLevel() {
        return level;
    }

    public UUID getPlacedBy() {
        return placedBy;
    }

    public UUID getLastPlayer() {
        return lastPlayer;
    }

    public void setLastPlayer(UUID uuid) {
        lastPlayer = uuid;
    }

    public ItemStack getAutoCrafting() {
        return autoCrafting;
    }

    public void setAutoCrafting(ItemStack autoCrafting) {
        this.autoCrafting = autoCrafting;
        autoCrafting.setAmount(1);
    }

    public TeleportTrigger getTeleportTrigger() {
        return teleportTrigger;
    }


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

    public List<Location> getLinkedBlocks() {
        return new ArrayList<>(linkedBlocks);
    }

    public void addLinkedBlock(Location block) {
        linkedBlocks.add(block);
    }

    public void removeLinkedBlock(Location location) {
        this.linkedBlocks.remove(location);
    }

    public Filter getFilter() {
        return filter;
    }
}
