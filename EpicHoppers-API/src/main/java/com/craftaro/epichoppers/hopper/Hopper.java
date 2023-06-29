package com.craftaro.epichoppers.hopper;

import com.craftaro.core.SongodaPlugin;
import com.craftaro.core.compatibility.CompatibleParticleHandler;
import com.craftaro.core.compatibility.ServerVersion;
import com.craftaro.core.hooks.EconomyManager;
import com.craftaro.core.third_party.com.cryptomorin.xseries.XSound;
import com.craftaro.epichoppers.EpicHoppersApi;
import com.craftaro.epichoppers.api.events.HopperAccessEvent;
import com.craftaro.epichoppers.database.DataManager;
import com.craftaro.epichoppers.hopper.levels.Level;
import com.craftaro.epichoppers.hopper.levels.LevelManager;
import com.craftaro.epichoppers.player.PlayerData;
import com.craftaro.epichoppers.player.PlayerDataManager;
import com.craftaro.epichoppers.utils.CostType;
import com.craftaro.epichoppers.utils.Methods;
import com.craftaro.epichoppers.hopper.teleport.TeleportTrigger;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.ApiStatus;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * FIXME: Needs heavy refactoring to only have one responsibility.
 */
public class Hopper {
    // Id for database use.
    private int id;

    private final Location location;
    private Level level = getLevelManager().getLowestLevel();
    private UUID lastPlayerOpened = null;
    private UUID placedBy = null;
    private final List<Location> linkedBlocks = new ArrayList<>();
    private Filter filter = new Filter();
    private TeleportTrigger teleportTrigger = TeleportTrigger.DISABLED;
    private int transferTick = 0;

    private int syncId = -1;

    private Player activePlayer;

    private final Map<String, Object> moduleCache = new HashMap<>();

    public Hopper(Location location) {
        this.location = location;
    }

    @ApiStatus.Internal
    public boolean prepareForOpeningOverviewGui(Player player) {
        if (this.lastPlayerOpened != null &&
                this.lastPlayerOpened != player.getUniqueId() &&
                Bukkit.getPlayer(this.lastPlayerOpened) != null) {
            Bukkit.getPlayer(this.lastPlayerOpened).closeInventory();
        }

        HopperAccessEvent accessEvent = new HopperAccessEvent(player, this);
        Bukkit.getPluginManager().callEvent(accessEvent);
        if (accessEvent.isCancelled()) {
            return false;
        }

        if (this.placedBy == null) {
            this.placedBy = player.getUniqueId();
        }

        if (!player.hasPermission("epichoppers.overview")) {
            return false;
        }

        setActivePlayer(player);
        return true;
    }

    @ApiStatus.Internal
    public void forceClose() {
        if (this.activePlayer != null) {
            this.activePlayer.closeInventory();
        }
    }

    public void dropItems() {
        Inventory inventory = ((InventoryHolder) this.location.getBlock().getState()).getInventory();
        World world = this.location.getWorld();

        for (ItemStack itemStack : inventory.getContents()) {
            if (itemStack == null || itemStack.getType() == Material.AIR) {
                continue;
            }

            world.dropItemNaturally(this.location, itemStack);
        }
    }

    public void upgrade(Player player, CostType type) {
        if (!getLevelManager().getLevels().containsKey(this.level.getLevel() + 1)) {
            return;
        }

        Level level = getLevelManager().getLevel(this.level.getLevel() + 1);
        int cost = type == CostType.ECONOMY ? level.getCostEconomy() : level.getCostExperience();

        if (type == CostType.ECONOMY) {
            if (!EconomyManager.isEnabled()) {
                player.sendMessage("Economy not enabled.");
                return;
            }
            if (!EconomyManager.hasBalance(player, cost)) {
                getPlugin().getLocale().getMessage("event.upgrade.cannotafford").sendPrefixedMessage(player);
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
                getPlugin().getLocale().getMessage("event.upgrade.cannotafford").sendPrefixedMessage(player);
            }
        }
    }

    private void upgradeFinal(Level level, Player player) {
        this.level = level;
        getDataManager().updateHopper(this);
        syncName();
        if (getLevelManager().getHighestLevel() != level) {
            getPlugin().getLocale().getMessage("event.upgrade.success")
                    .processPlaceholder("level", level.getLevel()).sendPrefixedMessage(player);
        } else {
            getPlugin().getLocale().getMessage("event.upgrade.maxed")
                    .processPlaceholder("level", level.getLevel()).sendPrefixedMessage(player);
        }
        Location loc = this.location.clone().add(.5, .5, .5);

        if (!getUpgradeParticleType().trim().isEmpty()) {
            CompatibleParticleHandler.spawnParticles(
                    CompatibleParticleHandler.ParticleType.getParticle(getUpgradeParticleType()),
                    loc, 100, .5, .5, .5);
        }

        if (getLevelManager().getHighestLevel() != level) {
            XSound.ENTITY_PLAYER_LEVELUP.play(player, .6f, 15);
        } else {
            XSound.ENTITY_PLAYER_LEVELUP.play(player, 2, 25);
            XSound.BLOCK_NOTE_BLOCK_CHIME.play(player, 2, 25);
            Bukkit.getScheduler().scheduleSyncDelayedTask(getPlugin(), () -> XSound.BLOCK_NOTE_BLOCK_CHIME.play(player, 1.2f, 35), 5);
            Bukkit.getScheduler().scheduleSyncDelayedTask(getPlugin(), () -> XSound.BLOCK_NOTE_BLOCK_CHIME.play(player, 1.8f, 35), 10);
        }
    }

    private void syncName() {
        org.bukkit.block.Hopper hopper = (org.bukkit.block.Hopper) this.location.getBlock().getState();
        if (ServerVersion.isServerVersionAtLeast(ServerVersion.V1_10)) {
            hopper.setCustomName(Methods.formatName(this.level.getLevel()));
        }
        hopper.update(true);
    }

    public void timeout(Player player) {
        this.syncId = Bukkit.getScheduler().scheduleSyncDelayedTask(getPlugin(), () -> {
            PlayerData playerData = getPlayerDataManager().getPlayerData(player);
            if (playerData.getSyncType() != null && playerData.getLastHopper() == this) {
                getPlugin().getLocale().getMessage("event.hopper.synctimeout").sendPrefixedMessage(player);
                playerData.setSyncType(null);
            }
        }, getLinkTimeoutFromPluginConfig() * this.level.getLinkAmount());
    }

    public void link(Block toLink, boolean filtered, Player player) {
        if (this.location.getWorld().equals(toLink.getLocation().getWorld())
                && !player.hasPermission("EpicHoppers.Override")
                && !player.hasPermission("EpicHoppers.Admin")
                && this.location.distance(toLink.getLocation()) > this.level.getRange()) {
            getPlugin().getLocale().getMessage("event.hopper.syncoutofrange").sendPrefixedMessage(player);
            return;
        }

        if (this.linkedBlocks.contains(toLink.getLocation())) {
            getPlugin().getLocale().getMessage("event.hopper.already").sendPrefixedMessage(player);
            return;
        }

        if (!filtered) {
            this.linkedBlocks.add(toLink.getLocation());
            getDataManager().createLink(this, toLink.getLocation(), LinkType.REGULAR);
        } else {
            this.filter.setEndPoint(toLink.getLocation());
            getDataManager().createLink(this, toLink.getLocation(), LinkType.REJECT);
            getPlugin().getLocale().getMessage("event.hopper.syncsuccess").sendPrefixedMessage(player);
            getPlayerDataManager().getPlayerData(player).setSyncType(null);
            return;
        }
        this.lastPlayerOpened = player.getUniqueId();

        if (this.level.getLinkAmount() > 1) {
            if (this.linkedBlocks.size() >= this.level.getLinkAmount()) {
                getPlugin().getLocale().getMessage("event.hopper.syncdone").sendPrefixedMessage(player);
                cancelSync(player);
                return;
            }
            getPlugin().getLocale().getMessage("event.hopper.syncsuccessmore")
                    .processPlaceholder("amount", this.level.getLinkAmount() - this.linkedBlocks.size())
                    .sendPrefixedMessage(player);
            return;
        }
        getPlugin().getLocale().getMessage("event.hopper.syncsuccess").sendPrefixedMessage(player);
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
        return this.location.clone();
    }

    public Block getBlock() {
        return this.location.getBlock();
    }

    public World getWorld() {
        return this.location.getWorld();
    }

    public int getX() {
        return this.location.getBlockX();
    }

    public int getY() {
        return this.location.getBlockY();
    }

    public int getZ() {
        return this.location.getBlockZ();
    }

    public Level getLevel() {
        return this.level;
    }

    public void setLevel(Level level) {
        this.level = level;
    }

    public UUID getPlacedBy() {
        return this.placedBy;
    }

    public void setPlacedBy(UUID placedBy) {
        this.placedBy = placedBy;
    }

    public UUID getLastPlayerOpened() {
        return this.lastPlayerOpened;
    }

    public void setLastPlayerOpened(UUID uuid) {
        this.lastPlayerOpened = uuid;
    }

    public TeleportTrigger getTeleportTrigger() {
        return this.teleportTrigger;
    }

    public void setTeleportTrigger(TeleportTrigger teleportTrigger) {
        this.teleportTrigger = teleportTrigger;
    }

    public List<Location> getLinkedBlocks() {
        return new ArrayList<>(this.linkedBlocks);
    }

    public void addLinkedBlock(Location location, LinkType type) {
        if (type == LinkType.REGULAR) {
            this.linkedBlocks.add(location);
        } else {
            this.filter.setEndPoint(location);
        }
    }

    public void removeLinkedBlock(Location location) {
        this.linkedBlocks.remove(location);
    }

    public void clearLinkedBlocks() {
        this.linkedBlocks.clear();
    }

    public Filter getFilter() {
        return this.filter;
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
        Bukkit.getScheduler().cancelTask(this.syncId);
        getPlayerDataManager().getPlayerData(player).setSyncType(null);
    }

    public int getId() {
        return this.id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Player getActivePlayer() {
        return this.activePlayer;
    }

    public void setActivePlayer(Player activePlayer) {
        this.activePlayer = activePlayer;
    }

    private LevelManager getLevelManager() {
        return EpicHoppersApi.getApi().getLevelManager();
    }

    private PlayerDataManager getPlayerDataManager() {
        return EpicHoppersApi.getApi().getPlayerDataManager();
    }

    private DataManager getDataManager() {
        return EpicHoppersApi.getApi().getDataManager();
    }

    /**
     * @deprecated The class needs refactoring to not even need the plugin.
     * This is just a temporary workaround to get a Minecraft 1.20-beta build ready
     */
    @Deprecated
    private long getLinkTimeoutFromPluginConfig() {
        return getPlugin().getConfig().getLong("Main.Timeout When Syncing Hoppers");
    }

    /**
     * @deprecated The class needs refactoring to not even need the plugin.
     * This is just a temporary workaround to get a Minecraft 1.20-beta build ready
     */
    @Deprecated
    private String getUpgradeParticleType() {
        return getPlugin().getConfig().getString("Main.Upgrade Particle Type");
    }

    /**
     * @deprecated The class needs refactoring to not even need the plugin.
     * This is just a temporary workaround to get a Minecraft 1.20-beta build ready
     */
    @Deprecated
    private SongodaPlugin getPlugin() {
        return (SongodaPlugin) Bukkit.getPluginManager().getPlugin("EpicHoppers");
    }
}
