package com.songoda.epichoppers;

import com.songoda.core.SongodaCore;
import com.songoda.core.SongodaPlugin;
import com.songoda.core.commands.CommandManager;
import com.songoda.core.compatibility.CompatibleMaterial;
import com.songoda.core.configuration.Config;
import com.songoda.core.database.DataMigrationManager;
import com.songoda.core.database.DatabaseConnector;
import com.songoda.core.database.SQLiteConnector;
import com.songoda.core.gui.GuiManager;
import com.songoda.core.hooks.EconomyManager;
import com.songoda.core.locale.Locale;
import com.songoda.core.nms.NmsManager;
import com.songoda.core.nms.nbt.NBTItem;
import com.songoda.epichoppers.boost.BoostData;
import com.songoda.epichoppers.boost.BoostManager;
import com.songoda.epichoppers.commands.*;
import com.songoda.epichoppers.compatiility.EpicHoppersPermission;
import com.songoda.epichoppers.database.DataManager;
import com.songoda.epichoppers.database.migrations._1_InitialMigration;
import com.songoda.epichoppers.handlers.TeleportHandler;
import com.songoda.epichoppers.hopper.*;
import com.songoda.epichoppers.hopper.levels.Level;
import com.songoda.epichoppers.hopper.levels.LevelManager;
import com.songoda.epichoppers.hopper.levels.modules.*;
import com.songoda.epichoppers.listeners.*;
import com.songoda.epichoppers.player.PlayerDataManager;
import com.songoda.epichoppers.settings.Settings;
import com.songoda.epichoppers.storage.Storage;
import com.songoda.epichoppers.storage.StorageRow;
import com.songoda.epichoppers.storage.types.StorageYaml;
import com.songoda.epichoppers.tasks.HopTask;
import com.songoda.epichoppers.utils.Methods;
import com.songoda.epichoppers.utils.TeleportTrigger;
import com.songoda.skyblock.SkyBlock;
import com.songoda.skyblock.permission.BasicPermission;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.PluginManager;

import java.io.File;
import java.util.*;


public class EpicHoppers extends SongodaPlugin {

    private static EpicHoppers INSTANCE;
    private final GuiManager guiManager = new GuiManager(this);
    private final Config levelsConfig = new Config(this, "levels.yml");
    private HopperManager hopperManager;
    private CommandManager commandManager;
    private LevelManager levelManager;
    private BoostManager boostManager;
    private PlayerDataManager playerDataManager;

    private TeleportHandler teleportHandler;

    private DatabaseConnector databaseConnector;
    private DataMigrationManager dataMigrationManager;
    private DataManager dataManager;

    private boolean liquidtanks = false;
    private boolean epicfarming = false;

    public static EpicHoppers getInstance() {
        return INSTANCE;
    }

    @Override
    public void onPluginLoad() {
        INSTANCE = this;
    }

    @Override
    public void onPluginDisable() {
        this.databaseConnector.closeConnection();
        saveModules();
    }

    @Override
    public void onPluginEnable() {
        // Run Songoda Updater
        SongodaCore.registerPlugin(this, 15, CompatibleMaterial.HOPPER);

        // Load Economy
        EconomyManager.load();

        // Setup Config
        Settings.setupConfig();
        this.setLocale(Settings.LANGUGE_MODE.getString(), false);

        // Set Economy & Hologram preference
        EconomyManager.getManager().setPreferredHook(Settings.ECONOMY_PLUGIN.getString());

        // Register commands
        this.commandManager = new CommandManager(this);
        this.commandManager.addCommand(new CommandEpicHoppers(this))
                .addSubCommands(
                        new CommandBoost(this),
                        new CommandGive(this),
                        new CommandReload(this),
                        new CommandSettings(this)
                );

        this.hopperManager = new HopperManager();
        this.playerDataManager = new PlayerDataManager();
        this.boostManager = new BoostManager();

        // Database stuff, go!
        this.databaseConnector = new SQLiteConnector(this);
        this.getLogger().info("Data handler connected using SQLite.");

        this.dataManager = new DataManager(this.databaseConnector, this);
        this.dataMigrationManager = new DataMigrationManager(this.databaseConnector, this.dataManager,
                new _1_InitialMigration());
        this.dataMigrationManager.runMigrations();

        this.loadLevelManager();
        Bukkit.getScheduler().runTaskLaterAsynchronously(this, () -> {
            // Legacy Data
            File folder = getDataFolder();
            File dataFile = new File(folder, "data.yml");

            boolean converted = false;
            if (dataFile.exists()) {
                converted = true;
                Storage storage = new StorageYaml(this);
                if (storage.containsGroup("sync")) {
                    console.sendMessage("[" + getDescription().getName() + "] " + ChatColor.RED + "Conversion process starting DO NOT turn off your server... " +
                            "EpicHoppers hasn't fully loaded yet so its best users don't interact with the plugin until conversion completes.");
                    List<Hopper> hoppers = new ArrayList<>();
                    for (StorageRow row : storage.getRowsByGroup("sync")) {
                        Location location = Methods.unserializeLocation(row.getKey());
                        if (location == null) continue;

                        int levelVal = row.get("level").asInt();
                        Level level = levelManager.isLevel(levelVal) ? levelManager.getLevel(levelVal) : levelManager.getLowestLevel();

                        String playerStr = row.get("player").asString();
                        String placedByStr = row.get("placedby").asString();
                        UUID lastPlayer = playerStr == null ? null : UUID.fromString(row.get("player").asString());
                        UUID placedBy = placedByStr == null ? null : UUID.fromString(placedByStr);

                        List<String> blockLoc = row.get("block").asStringList();
                        List<Location> blocks = new ArrayList<>();
                        if (blockLoc != null) {
                            for (String string : blockLoc) {
                                blocks.add(Methods.unserializeLocation(string));
                            }
                        }

                        Filter filter = new Filter();

                        List<ItemStack> whiteList = row.get("whitelist").asItemStackList();
                        List<ItemStack> blackList = row.get("blacklist").asItemStackList();
                        List<ItemStack> voidList = row.get("void").asItemStackList();

                        List<ItemStack> autoSellWhiteList = row.get("autosell-whitelist").asItemStackList();
                        List<ItemStack> autoSellBlackList = row.get("autosell-blacklist").asItemStackList();

                        String blackLoc = row.get("black").asString();
                        Location black = blackLoc == null ? null : Methods.unserializeLocation(blackLoc);

                        filter.setWhiteList(whiteList);
                        filter.setBlackList(blackList);
                        filter.setVoidList(voidList);

                        filter.setAutoSellWhiteList(autoSellWhiteList);
                        filter.setAutoSellBlackList(autoSellBlackList);

                        filter.setEndPoint(black);

                        TeleportTrigger teleportTrigger = TeleportTrigger.valueOf(row.get("teleporttrigger").asString() == null ? "DISABLED" : row.get("teleporttrigger").asString());

                        hoppers.add(new HopperBuilder(location)
                                .setLevel(level)
                                .setLastPlayerOpened(lastPlayer)
                                .setPlacedBy(placedBy)
                                .addLinkedBlocks(LinkType.REGULAR, blocks.toArray(new Location[0]))
                                .setFilter(filter)
                                .setTeleportTrigger(teleportTrigger)
                                .build());
                    }
                    dataManager.createHoppers(hoppers);
                }

                // Adding in Boosts
                if (storage.containsGroup("boosts")) {
                    for (StorageRow row : storage.getRowsByGroup("boosts")) {
                        if (row.get("uuid").asObject() == null)
                            continue;

                        dataManager.createBoost(new BoostData(
                                row.get("amount").asInt(),
                                Long.parseLong(row.getKey()),
                                UUID.fromString(row.get("uuid").asString())));
                    }
                }
                dataFile.delete();
            }

            final boolean convrted = converted;
            getDataManager().queueAsync(() -> {
                if (convrted)
                    console.sendMessage("[" + getDescription().getName() + "] " + ChatColor.GREEN + "Conversion complete :)");
                // Load data from DB
                this.dataManager.getHoppers((hoppers) -> {
                    this.hopperManager.addHoppers(hoppers.values());
                    this.dataManager.getBoosts((boosts) -> this.boostManager.addBoosts(boosts));
                });
            }, "create");
        }, 20);

        new HopTask(this);
        this.teleportHandler = new TeleportHandler(this);

        // Register Listeners
        guiManager.init();
        PluginManager pluginManager = Bukkit.getPluginManager();
        pluginManager.registerEvents(new HopperListeners(this), this);
        pluginManager.registerEvents(new EntityListeners(), this);
        pluginManager.registerEvents(new BlockListeners(this), this);
        pluginManager.registerEvents(new InteractListeners(this), this);
        pluginManager.registerEvents(new InventoryListeners(), this);

        if (pluginManager.isPluginEnabled("FabledSkyBlock")) {
            try {
                SkyBlock.getInstance().getPermissionManager().registerPermission(
                        (BasicPermission) Class.forName("com.songoda.epichoppers.compatibility.EpicHoppersPermission").newInstance());
            } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
                e.printStackTrace();
            }
        }

        // Check for liquid tanks
        if (pluginManager.isPluginEnabled("LiquidTanks")) liquidtanks = true;

        // Check for epicfarming
        if (pluginManager.isPluginEnabled("EpicFarming")) epicfarming = true;

        // Start auto save
        int saveInterval = Settings.AUTOSAVE.getInt() * 60 * 20;
        Bukkit.getScheduler().runTaskTimerAsynchronously(this, this::saveModules, saveInterval, saveInterval);
    }

    @Override
    public void onConfigReload() {
        this.setLocale(getConfig().getString("System.Language Mode"), true);
        this.locale.reloadMessages();
        loadLevelManager();
    }

    @Override
    public List<Config> getExtraConfig() {
        return Collections.singletonList(levelsConfig);
    }

    private void loadLevelManager() {
        if (!new File(this.getDataFolder(), "levels.yml").exists())
            this.saveResource("levels.yml", false);
        levelsConfig.load();

        // Load an instance of LevelManager
        levelManager = new LevelManager();
        /*
         * Register Levels into LevelManager from configuration.
         */
        levelManager.clear();
        for (String levelName : levelsConfig.getKeys(false)) {
            int level = Integer.parseInt(levelName.split("-")[1]);

            ConfigurationSection levels = levelsConfig.getConfigurationSection(levelName);

            int radius = levels.getInt("Range");
            int amount = levels.getInt("Amount");
            int linkAmount = levels.getInt("Link-amount", 1);
            boolean filter = levels.getBoolean("Filter");
            boolean teleport = levels.getBoolean("Teleport");
            int costExperience = levels.getInt("Cost-xp", -1);
            int costEconomy = levels.getInt("Cost-eco", -1);
            int autoSell = levels.getInt("AutoSell");

            ArrayList<Module> modules = new ArrayList<>();

            for (String key : levels.getKeys(false)) {
                if (key.equals("Suction") && levels.getInt("Suction") != 0) {
                    modules.add(new ModuleSuction(this, levels.getInt("Suction")));
                } else if (key.equals("BlockBreak") && levels.getInt("BlockBreak") != 0) {
                    modules.add(new ModuleBlockBreak(this, levels.getInt("BlockBreak")));
                } else if (key.equals("AutoCrafting")) {
                    modules.add(new ModuleAutoCrafting(this));
                } else if (key.equals("AutoSell")) {
                    modules.add(new ModuleAutoSell(this, autoSell));
                }

            }
            levelManager.addLevel(level, costExperience, costEconomy, radius, amount, filter, teleport, linkAmount, modules);
        }
    }

    private void saveModules() {
        if (levelManager != null) {
            for (Level level : levelManager.getLevels().values())
                for (Module module : level.getRegisteredModules())
                    module.saveDataToFile();
        }
    }

    public ItemStack newHopperItem(Level level) {
        ItemStack item = new ItemStack(Material.HOPPER, 1);
        ItemMeta itemmeta = item.getItemMeta();
        itemmeta.setDisplayName(Methods.formatText(Methods.formatName(level.getLevel())));
        String line = getLocale().getMessage("general.nametag.lore").getMessage();
        if (!line.equals("")) {
            itemmeta.setLore(Arrays.asList(line.split("\n")));
        }
        item.setItemMeta(itemmeta);

        NBTItem nbtItem = NmsManager.getNbt().of(item);
        nbtItem.set("level", level.getLevel());
        return nbtItem.finish();
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

    public CommandManager getCommandManager() {
        return commandManager;
    }

    public LevelManager getLevelManager() {
        return levelManager;
    }

    public HopperManager getHopperManager() {
        return hopperManager;
    }

    public PlayerDataManager getPlayerDataManager() {
        return playerDataManager;
    }

    public GuiManager getGuiManager() {
        return guiManager;
    }

    public DataManager getDataManager() {
        return dataManager;
    }

    public DatabaseConnector getDatabaseConnector() {
        return databaseConnector;
    }

    public boolean isLiquidtanks() {
        return liquidtanks;
    }

    public boolean isEpicFarming() {
        return epicfarming;
    }
}
