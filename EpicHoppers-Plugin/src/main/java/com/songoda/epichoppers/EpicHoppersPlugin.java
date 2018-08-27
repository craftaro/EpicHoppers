package com.songoda.epichoppers;

import com.google.common.base.Preconditions;
import com.songoda.arconix.api.mcupdate.MCUpdate;
import com.songoda.arconix.api.utils.ConfigWrapper;
import com.songoda.arconix.plugin.Arconix;
import com.songoda.epichoppers.api.EpicHoppers;
import com.songoda.epichoppers.api.EpicHoppersAPI;
import com.songoda.epichoppers.api.hopper.*;
import com.songoda.epichoppers.api.utils.ClaimableProtectionPluginHook;
import com.songoda.epichoppers.api.utils.ProtectionPluginHook;
import com.songoda.epichoppers.boost.BoostData;
import com.songoda.epichoppers.boost.BoostManager;
import com.songoda.epichoppers.command.CommandManager;
import com.songoda.epichoppers.handlers.EnchantmentHandler;
import com.songoda.epichoppers.handlers.HopHandler;
import com.songoda.epichoppers.handlers.TeleportHandler;
import com.songoda.epichoppers.hooks.*;
import com.songoda.epichoppers.hopper.EFilter;
import com.songoda.epichoppers.hopper.EHopper;
import com.songoda.epichoppers.hopper.EHopperManager;
import com.songoda.epichoppers.hopper.ELevelManager;
import com.songoda.epichoppers.listeners.BlockListeners;
import com.songoda.epichoppers.listeners.HopperListeners;
import com.songoda.epichoppers.listeners.InteractListeners;
import com.songoda.epichoppers.listeners.InventoryListeners;
import com.songoda.epichoppers.player.PlayerDataManager;
import com.songoda.epichoppers.utils.Methods;
import com.songoda.epichoppers.utils.SettingsManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;


public class EpicHoppersPlugin extends JavaPlugin implements EpicHoppers {
    public static CommandSender console = Bukkit.getConsoleSender();

    private static EpicHoppersPlugin INSTANCE;

    private List<ProtectionPluginHook> protectionHooks = new ArrayList<>();
    private ClaimableProtectionPluginHook factionsHook, townyHook, aSkyblockHook, uSkyblockHook;

    private SettingsManager settingsManager;

    public References references = null;
    private ConfigWrapper hooksFile = new ConfigWrapper(this, "", "hooks.yml");
    public ConfigWrapper dataFile = new ConfigWrapper(this, "", "data.yml");

    public EnchantmentHandler enchantmentHandler;

    private Locale locale;

    private HopperManager hopperManager;
    private LevelManager levelManager;
    private BoostManager boostManager;
    private PlayerDataManager playerDataManager;

    private TeleportHandler teleportHandler;

    public void onEnable() {
        INSTANCE = this;
        EpicHoppersAPI.setImplementation(this);

        Arconix.pl().hook(this);

        console.sendMessage(Arconix.pl().getApi().format().formatText("&a============================="));
        console.sendMessage(Arconix.pl().getApi().format().formatText("&7EpicHoppers " + this.getDescription().getVersion() + " by &5Brianna <3&7!"));
        console.sendMessage(Arconix.pl().getApi().format().formatText("&7Action: &aEnabling&7..."));

        settingsManager = new SettingsManager(this);
        boostManager = new BoostManager();
        setupConfig();
        loadDataFile();
        enchantmentHandler = new EnchantmentHandler();
        playerDataManager = new PlayerDataManager();

        // Locales
        Locale.init(this);
        Locale.saveDefaultLocale("en_US");
        this.locale = Locale.getLocale(this.getConfig().getString("Locale", "en_US"));

        loadLevelManager();

        hopperManager = new EHopperManager();

        /*
         * Register hoppers into HopperManger from configuration
         */
        Bukkit.getScheduler().runTaskLater(this, () -> {
            if (dataFile.getConfig().contains("data.sync")) {
                for (String locationStr : dataFile.getConfig().getConfigurationSection("data.sync").getKeys(false)) {
                    Location location = Arconix.pl().getApi().serialize().unserializeLocation(locationStr);
                    if (location == null || location.getBlock() == null) return;

                    int level = dataFile.getConfig().getInt("data.sync." + locationStr + ".level");

                    String blockLoc = dataFile.getConfig().getString("data.sync." + locationStr + ".block");
                    Block block = blockLoc == null ? null : Arconix.pl().getApi().serialize().unserializeLocation(dataFile.getConfig().getString("data.sync." + locationStr + ".block")).getBlock();

                    TeleportTrigger teleportTrigger = TeleportTrigger.valueOf(dataFile.getConfig().getString("data.sync." + locationStr + ".teleportTrigger"));

                    String playerStr = dataFile.getConfig().getString("data.sync." + locationStr + ".player");
                    String placedByStr = dataFile.getConfig().getString("data.sync." + locationStr + ".placedBy");
                    UUID lastPlayer = playerStr == null ? null : UUID.fromString(playerStr);
                    UUID placedBy = placedByStr == null ? null : UUID.fromString(placedByStr);

                    List<ItemStack> whiteList = (ArrayList<ItemStack>) dataFile.getConfig().getList("data.sync." + locationStr + ".whitelist");
                    List<ItemStack> blackList = (ArrayList<ItemStack>) dataFile.getConfig().getList("data.sync." + locationStr + ".blacklist");
                    List<ItemStack> voidList = (ArrayList<ItemStack>) dataFile.getConfig().getList("data.sync." + locationStr + ".void");

                    String blackLoc = dataFile.getConfig().getString("data.sync." + locationStr + ".black");
                    Block black = blackLoc == null ? null : Arconix.pl().getApi().serialize().unserializeLocation(dataFile.getConfig().getString("data.sync." + locationStr + ".black")).getBlock();

                    EFilter filter = new EFilter();

                    filter.setWhiteList(whiteList);
                    filter.setBlackList(blackList);
                    filter.setVoidList(voidList);
                    filter.setEndPoint(black);

                    EHopper hopper = new EHopper(location, levelManager.getLevel(level), lastPlayer, placedBy, block, filter, teleportTrigger);

                    hopperManager.addHopper(location, hopper);
                }
            }

            // Adding in Boosts
            if (dataFile.getConfig().contains("data.boosts")) {
                for (String key : dataFile.getConfig().getConfigurationSection("data.boosts").getKeys(false)) {
                    if (!dataFile.getConfig().contains("data.boosts." + key + ".Player")) continue;
                    BoostData boostData = new BoostData(
                            dataFile.getConfig().getInt("data.boosts." + key + ".Amount"),
                            Long.parseLong(key),
                            UUID.fromString(dataFile.getConfig().getString("data.boosts." + key + ".Player")));

                    this.boostManager.addBoostToPlayer(boostData);
                }
            }

        }, 10);

        references = new References();

        new HopHandler(this);
        teleportHandler = new TeleportHandler(this);

        new MCUpdate(this, true);
        //new MassiveStats(this, 9000);

        Bukkit.getScheduler().scheduleSyncRepeatingTask(this, this::saveToFile, 6000, 6000);

        getServer().getPluginManager().registerEvents(new HopperListeners(this), this);
        getServer().getPluginManager().registerEvents(new BlockListeners(this), this);
        getServer().getPluginManager().registerEvents(new InteractListeners(this), this);
        getServer().getPluginManager().registerEvents(new InventoryListeners(this), this);

        // Command registration
        this.getCommand("EpicHoppers").setExecutor(new CommandManager(this));



        // Register default hooks
        if (Bukkit.getPluginManager().isPluginEnabled("ASkyBlock")) this.register(HookASkyBlock::new);
        if (Bukkit.getPluginManager().isPluginEnabled("Factions")) this.register(HookFactions::new);
        if (Bukkit.getPluginManager().isPluginEnabled("GriefPrevention")) this.register(HookGriefPrevention::new);
        if (Bukkit.getPluginManager().isPluginEnabled("Kingdoms")) this.register(HookKingdoms::new);
        if (Bukkit.getPluginManager().isPluginEnabled("PlotSquared")) this.register(HookPlotSquared::new);
        if (Bukkit.getPluginManager().isPluginEnabled("RedProtect")) this.register(HookRedProtect::new);
        if (Bukkit.getPluginManager().isPluginEnabled("Towny")) this.register(HookTowny::new);
        if (Bukkit.getPluginManager().isPluginEnabled("USkyBlock")) this.register(HookUSkyBlock::new);
        if (Bukkit.getPluginManager().isPluginEnabled("WorldGuard")) this.register(HookWorldGuard::new);


        console.sendMessage(Arconix.pl().getApi().format().formatText("&a============================="));
    }

    public void onDisable() {
        saveToFile();
        this.protectionHooks.clear();
        console.sendMessage(Arconix.pl().getApi().format().formatText("&a============================="));
        console.sendMessage(Arconix.pl().getApi().format().formatText("&7EpicHoppers " + this.getDescription().getVersion() + " by &5Brianna <3!"));
        console.sendMessage(Arconix.pl().getApi().format().formatText("&7Action: &cDisabling&7..."));
        console.sendMessage(Arconix.pl().getApi().format().formatText("&a============================="));
        dataFile.saveConfig();
    }

    /*
     * Saves registered hopper to file.
     */
    private void saveToFile() {

        // Wipe old hopper information
        dataFile.getConfig().set("data.sync", null);

        /*
         * Dump HopperManager to file.
         */
        for (Hopper hopper : hopperManager.getHoppers().values()) {
            if (hopper.getLevel() == null || hopper.getLocation() == null || hopper.getLocation().getChunk() == null)
                continue;
            String locationStr = Arconix.pl().getApi().serialize().serializeLocation(hopper.getLocation());
            dataFile.getConfig().set("data.sync." + locationStr + ".level", hopper.getLevel().getLevel());
            dataFile.getConfig().set("data.sync." + locationStr + ".block", hopper.getSyncedBlock() == null ? null : Arconix.pl().getApi().serialize().serializeLocation(hopper.getSyncedBlock().getLocation()));
            dataFile.getConfig().set("data.sync." + locationStr + ".player", hopper.getLastPlayer() == null ? null : hopper.getLastPlayer().toString());
            dataFile.getConfig().set("data.sync." + locationStr + ".placedBy", hopper.getPlacedBy() == null ? null : hopper.getPlacedBy().toString());
            dataFile.getConfig().set("data.sync." + locationStr + ".teleportTrigger", hopper.getTeleportTrigger().toString());
            dataFile.getConfig().set("data.sync." + locationStr + ".whitelist", hopper.getFilter().getWhiteList());
            dataFile.getConfig().set("data.sync." + locationStr + ".blacklist", hopper.getFilter().getBlackList());
            dataFile.getConfig().set("data.sync." + locationStr + ".void", hopper.getFilter().getVoidList());
            dataFile.getConfig().set("data.sync." + locationStr + ".black", hopper.getFilter().getEndPoint() == null ? null : Arconix.pl().getApi().serialize().serializeLocation(hopper.getFilter().getEndPoint().getLocation()));
        }

        /*
         * Dump BoostManager to file.
         */
        for (BoostData boostData : boostManager.getBoosts()) {
            String endTime = String.valueOf(boostData.getEndTime());
            dataFile.getConfig().set("data.boosts." + endTime + ".Player", boostData.getPlayer().toString());
            dataFile.getConfig().set("data.boosts." + endTime + ".Amount", boostData.getMultiplier());
        }

        //Save to file
        dataFile.saveConfig();
    }

    private void loadLevelManager() {
        // Load an instance of LevelManager
        levelManager = new ELevelManager();
        /*
         * Register Levels into LevelManager from configuration.
         */
        ((ELevelManager) levelManager).clear();
        for (String levelName : getConfig().getConfigurationSection("settings.levels").getKeys(false)) {
            int level = Integer.valueOf(levelName.split("-")[1]);
            int radius = getConfig().getInt("settings.levels." + levelName + ".Range");
            int amount = getConfig().getInt("settings.levels." + levelName + ".Amount");
            int suction = getConfig().getInt("settings.levels." + levelName + ".Suction");
            int blockBreak = getConfig().getInt("settings.levels." + levelName + ".BlockBreak");
            boolean filter = getConfig().getBoolean("settings.levels." + levelName + ".Filter");
            boolean teleport = getConfig().getBoolean("settings.levels." + levelName + ".Teleport");
            int costExperiance = getConfig().getInt("settings.levels." + levelName + ".Cost-xp");
            int costEconomy = getConfig().getInt("settings.levels." + levelName + ".Cost-eco");
            levelManager.addLevel(level, costExperiance, costEconomy, radius, amount, suction, blockBreak, filter, teleport);
        }
    }

    private void setupConfig() {
        settingsManager.updateSettings();

        if (!getConfig().contains("settings.levels.Level-1")) {
            getConfig().addDefault("settings.levels.Level-1.Range", 10);
            getConfig().addDefault("settings.levels.Level-1.Amount", 1);
            getConfig().addDefault("settings.levels.Level-1.Cost-xp", 20);
            getConfig().addDefault("settings.levels.Level-1.Cost-eco", 5000);

            getConfig().addDefault("settings.levels.Level-2.Range", 20);
            getConfig().addDefault("settings.levels.Level-2.Amount", 2);
            getConfig().addDefault("settings.levels.Level-2.Cost-xp", 25);
            getConfig().addDefault("settings.levels.Level-2.Cost-eco", 7500);

            getConfig().addDefault("settings.levels.Level-3.Range", 30);
            getConfig().addDefault("settings.levels.Level-3.Amount", 3);
            getConfig().addDefault("settings.levels.Level-3.Suction", 1);
            getConfig().addDefault("settings.levels.Level-3.Cost-xp", 30);
            getConfig().addDefault("settings.levels.Level-3.Cost-eco", 10000);

            getConfig().addDefault("settings.levels.Level-4.Range", 40);
            getConfig().addDefault("settings.levels.Level-4.Amount", 4);
            getConfig().addDefault("settings.levels.Level-4.Suction", 2);
            getConfig().addDefault("settings.levels.Level-4.BlockBreak", 4);
            getConfig().addDefault("settings.levels.Level-4.Cost-xp", 35);
            getConfig().addDefault("settings.levels.Level-4.Cost-eco", 12000);

            getConfig().addDefault("settings.levels.Level-5.Range", 50);
            getConfig().addDefault("settings.levels.Level-5.Amount", 5);
            getConfig().addDefault("settings.levels.Level-5.Suction", 3);
            getConfig().addDefault("settings.levels.Level-5.BlockBreak", 2);
            getConfig().addDefault("settings.levels.Level-5.Cost-xp", 40);
            getConfig().addDefault("settings.levels.Level-5.Cost-eco", 15000);

            getConfig().addDefault("settings.levels.Level-6.Range", 60);
            getConfig().addDefault("settings.levels.Level-6.Amount", 5);
            getConfig().addDefault("settings.levels.Level-6.Suction", 3);
            getConfig().addDefault("settings.levels.Level-6.BlockBreak", 2);
            getConfig().addDefault("settings.levels.Level-6.Filter", true);
            getConfig().addDefault("settings.levels.Level-6.Teleport", true);
            getConfig().addDefault("settings.levels.Level-6.Cost-xp", 45);
            getConfig().addDefault("settings.levels.Level-6.Cost-eco", 20000);
        }

        getConfig().options().copyDefaults(true);
        saveConfig();
    }

    private void loadDataFile() {
        dataFile.getConfig().options().copyDefaults(true);
        dataFile.saveConfig();
    }

    public void reload() {
        locale.reloadMessages();
        references = new References();
        reloadConfig();
        saveConfig();
        loadLevelManager();
    }


    public boolean canBuild(Player player, Location location) {
        if (player.hasPermission(getDescription().getName() + ".bypass")) {
            return true;
        }

        for (ProtectionPluginHook hook : protectionHooks)
            if (!hook.canBuild(player, location)) return false;
        return true;
    }

    public boolean isInFaction(String name, Location l) {
        return factionsHook != null && factionsHook.isInClaim(l, name);
    }

    public String getFactionId(String name) {
        return (factionsHook != null) ? factionsHook.getClaimID(name) : null;
    }

    public boolean isInTown(String name, Location l) {
        return townyHook != null && townyHook.isInClaim(l, name);
    }

    public String getTownId(String name) {
        return (townyHook != null) ? townyHook.getClaimID(name) : null;
    }

    @SuppressWarnings("deprecation")
    public String getIslandId(String name) {
        return Bukkit.getOfflinePlayer(name).getUniqueId().toString();
    }


    @Override
    public Level getLevelFromItem(ItemStack item) {
        if (item.getItemMeta().getDisplayName().contains(":")) {
            String arr[] = item.getItemMeta().getDisplayName().replace("§", "").split(":");
            return getLevelManager().getLevel(Integer.parseInt(arr[0]));
        } else {
            return getLevelManager().getLowestLevel();
        }
    }

    @Override
    public ItemStack newHopperItem(Level level) {
        ItemStack item = new ItemStack(Material.HOPPER, 1);
        ItemMeta itemmeta = item.getItemMeta();
        itemmeta.setDisplayName(Arconix.pl().getApi().format().formatText(Methods.formatName(level.getLevel(), true)));
        item.setItemMeta(itemmeta);
        return item;
    }

    public Locale getLocale() {
        return locale;
    }

    public TeleportHandler getTeleportHandler() {
        return teleportHandler;
    }

    public BoostManager getBoostManager() {
        return boostManager;
    }

    @Override
    public LevelManager getLevelManager() {
        return levelManager;
    }

    @Override
    public HopperManager getHopperManager() {
        return hopperManager;
    }

    public SettingsManager getSettingsManager() {
        return settingsManager;
    }

    public PlayerDataManager getPlayerDataManager() {
        return playerDataManager;
    }

    public static EpicHoppersPlugin getInstance() {
        return INSTANCE;
    }

    private void register(Supplier<ProtectionPluginHook> hookSupplier) {
        this.registerProtectionHook(hookSupplier.get());
    }

    @Override
    public void registerProtectionHook(ProtectionPluginHook hook) {
        Preconditions.checkNotNull(hook, "Cannot register null hook");
        Preconditions.checkNotNull(hook.getPlugin(), "Protection plugin hook returns null plugin instance (#getPlugin())");

        JavaPlugin hookPlugin = hook.getPlugin();
        for (ProtectionPluginHook existingHook : protectionHooks) {
            if (existingHook.getPlugin().equals(hookPlugin)) {
                throw new IllegalArgumentException("Hook already registered");
            }
        }

        this.hooksFile.getConfig().addDefault("hooks." + hookPlugin.getName(), true);
        if (!hooksFile.getConfig().getBoolean("hooks." + hookPlugin.getName(), true)) return;
        this.hooksFile.getConfig().options().copyDefaults(true);
        this.hooksFile.saveConfig();

        this.protectionHooks.add(hook);
        this.getLogger().info("Registered protection hook for plugin: " + hook.getPlugin().getName());
    }

}
