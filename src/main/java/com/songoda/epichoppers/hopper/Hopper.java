package com.songoda.epichoppers.hopper;

import com.songoda.core.compatibility.ServerVersion;
import com.songoda.core.gui.GuiManager;
import com.songoda.core.hooks.EconomyManager;
import com.songoda.epichoppers.EpicHoppers;
import com.songoda.epichoppers.gui.GUIOverview;
import com.songoda.epichoppers.hopper.levels.Level;
import com.songoda.epichoppers.player.PlayerData;
import com.songoda.epichoppers.utils.CostType;
import com.songoda.epichoppers.utils.Methods;
import com.songoda.epichoppers.utils.TeleportTrigger;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * Created by songoda on 3/14/2017.
 */
public class Hopper {

    private final Location location;
    private Level level = EpicHoppers.getInstance().getLevelManager().getLowestLevel();
    private UUID lastPlayerOpened = null;
    private UUID placedBy = null;
    private final List<Location> linkedBlocks = new ArrayList<>();
    private Filter filter = new Filter();
    private TeleportTrigger teleportTrigger = TeleportTrigger.DISABLED;
    private int transferTick = 0;

    private int syncId = -1;

    private final Map<String, Object> moduleCache = new HashMap<>();

    public Hopper(Location location) {
        this.location = location;
    }

    public void overview(GuiManager guiManager, Player player) {
        if (lastPlayerOpened != null
                && lastPlayerOpened != player.getUniqueId()
                && Bukkit.getPlayer(lastPlayerOpened) != null) {
            Bukkit.getPlayer(lastPlayerOpened).closeInventory();
        }
        if (placedBy == null) placedBy = player.getUniqueId();

        EpicHoppers instance = EpicHoppers.getInstance();
        if (!player.hasPermission("epichoppers.overview")) return;
        guiManager.showGUI(player, new GUIOverview(instance, this, player));
    }

    public void upgrade(Player player, CostType type) {
        EpicHoppers plugin = EpicHoppers.getInstance();
        if (!plugin.getLevelManager().getLevels().containsKey(this.level.getLevel() + 1)) return;

        Level level = plugin.getLevelManager().getLevel(this.level.getLevel() + 1);
        int cost = type == CostType.ECONOMY ? level.getCostEconomy() : level.getCostExperience();

        if (type == CostType.ECONOMY) {
            if (!EconomyManager.isEnabled()) {
                player.sendMessage("Economy not enabled.");
                return;
            }
            if (!EconomyManager.hasBalance(player, cost)) {
                plugin.getInstance().getLocale().getMessage("event.upgrade.cannotafford").sendPrefixedMessage(player);
                return;
            }
            EconomyManager.withdrawBalance(player, cost);
            upgradeFinal(level, player);
        } else if (type == CostType.EXPERIENCE) {
            if (player.getLevel() >= cost || player.getGameMode() == GameMode.CREATIVE) {
                if (player.getGameMode() != GameMode.CREATIVE) {
                    player.setLevel(player.getLevel() - cost);
                }
                upgradeFinal(level, player);
            } else {
                plugin.getLocale().getMessage("event.upgrade.cannotafford").sendPrefixedMessage(player);
            }
        }
    }

    private void upgradeFinal(Level level, Player player) {
        EpicHoppers plugin = EpicHoppers.getInstance();
        this.level = level;
        syncName();
        if (plugin.getLevelManager().getHighestLevel() != level) {
            plugin.getLocale().getMessage("event.upgrade.success")
                    .processPlaceholder("level", level.getLevel()).sendPrefixedMessage(player);
        } else {
            plugin.getLocale().getMessage("event.upgrade.maxed")
                    .processPlaceholder("level", level.getLevel()).sendPrefixedMessage(player);
        }
        Location loc = location.clone().add(.5, .5, .5);

        if (!ServerVersion.isServerVersionAtLeast(ServerVersion.V1_12)) return;

        player.getWorld().spawnParticle(org.bukkit.Particle.valueOf(plugin.getConfig().getString("Main.Upgrade Particle Type")), loc, 200, .5, .5, .5);

        if (plugin.getLevelManager().getHighestLevel() != level) {
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.6F, 15.0F);
        } else {
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 2F, 25.0F);

            if (!ServerVersion.isServerVersionAtLeast(ServerVersion.V1_13)) return;

            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 2F, 25.0F);
            Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1.2F, 35.0F), 5L);
            Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1.8F, 35.0F), 10L);
        }
    }

    private void syncName() {
        org.bukkit.block.Hopper hopper = (org.bukkit.block.Hopper) location.getBlock().getState();
        if (ServerVersion.isServerVersionAtLeast(ServerVersion.V1_10))
            hopper.setCustomName(Methods.formatName(level.getLevel(), false));
        hopper.update(true);
    }

    public void timeout(Player player) {
        EpicHoppers instance = EpicHoppers.getInstance();
        syncId = Bukkit.getScheduler().scheduleSyncDelayedTask(instance, () -> {
            PlayerData playerData = instance.getPlayerDataManager().getPlayerData(player);
            if (playerData.getSyncType() != null && playerData.getLastHopper() == this) {
                instance.getLocale().getMessage("event.hopper.synctimeout").sendPrefixedMessage(player);
                playerData.setSyncType(null);
            }
        }, instance.getConfig().getLong("Main.Timeout When Syncing Hoppers") * level.getLinkAmount());
    }

    public void link(Block toLink, boolean filtered, Player player) {
        EpicHoppers instance = EpicHoppers.getInstance();

        if (location.getWorld().equals(toLink.getLocation().getWorld())
                && !player.hasPermission("EpicHoppers.Override")
                && !player.hasPermission("EpicHoppers.Admin")
                && location.distance(toLink.getLocation()) > level.getRange()) {
            instance.getLocale().getMessage("event.hopper.syncoutofrange").sendPrefixedMessage(player);
            return;
        }

        if (linkedBlocks.contains(toLink.getLocation())) {
            instance.getLocale().getMessage("event.hopper.already").sendPrefixedMessage(player);
            return;
        }

        if (!filtered)
            this.linkedBlocks.add(toLink.getLocation());
        else {
            this.filter.setEndPoint(toLink.getLocation());
            instance.getLocale().getMessage("event.hopper.syncsuccess").sendPrefixedMessage(player);
            instance.getPlayerDataManager().getPlayerData(player).setSyncType(null);
            return;
        }
        this.lastPlayerOpened = player.getUniqueId();

        if (level.getLinkAmount() > 1) {
            if (linkedBlocks.size() >= level.getLinkAmount()) {
                instance.getLocale().getMessage("event.hopper.syncdone").sendPrefixedMessage(player);
                cancelSync(player);
                return;
            }
            instance.getLocale().getMessage("event.hopper.syncsuccessmore")
                    .processPlaceholder("amount", level.getLinkAmount() - linkedBlocks.size())
                    .sendPrefixedMessage(player);
            return;
        }
        instance.getLocale().getMessage("event.hopper.syncsuccess").sendPrefixedMessage(player);
        cancelSync(player);
    }

    /**
     * Ticks a hopper to determine when it can transfer items next
     *
     * @param maxTick      The maximum amount the hopper can be ticked before next transferring items
     * @param allowLooping If true, the hopper is allowed to transfer items if the tick is also valid
     * @return true if the hopper should transfer an item, otherwise false
     */
    public boolean tryTick(int maxTick, boolean allowLooping) {
        this.transferTick++;
        if (this.transferTick >= maxTick) {
            if (allowLooping) {
                this.transferTick = 0;
                return true;
            } else {
                this.transferTick = maxTick;
            }
        }
        return false;
    }

    public Location getLocation() {
        return location.clone();
    }

    public Block getBlock() {
        return location.getBlock();
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

    public void setLevel(Level level) {
        this.level = level;
    }

    public UUID getPlacedBy() {
        return placedBy;
    }

    public void setPlacedBy(UUID placedBy) {
        this.placedBy = placedBy;
    }

    public UUID getLastPlayerOpened() {
        return lastPlayerOpened;
    }

    public void setLastPlayerOpened(UUID uuid) {
        lastPlayerOpened = uuid;
    }

    public TeleportTrigger getTeleportTrigger() {
        return teleportTrigger;
    }

    public void setTeleportTrigger(TeleportTrigger teleportTrigger) {
        this.teleportTrigger = teleportTrigger;
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

    public void clearLinkedBlocks() {
        this.linkedBlocks.clear();
    }

    public Filter getFilter() {
        return filter;
    }

    public void setFilter(Filter filter) {
        this.filter = filter;
    }

    public Object getDataFromModuleCache(String key) {
        return this.moduleCache.getOrDefault(key, null);
    }

    public void addDataToModuleCache(String key, Object data) {
        this.moduleCache.put(key, data);
    }

    public boolean isDataCachedInModuleCache(String key) {
        return this.moduleCache.containsKey(key);
    }

    public void removeDataFromModuleCache(String key) {
        this.moduleCache.remove(key);
    }

    public void clearModuleCache() {
        this.moduleCache.clear();
    }

    public void cancelSync(Player player) {
        Bukkit.getScheduler().cancelTask(syncId);
        EpicHoppers.getInstance().getPlayerDataManager().getPlayerData(player).setSyncType(null);
    }
}
