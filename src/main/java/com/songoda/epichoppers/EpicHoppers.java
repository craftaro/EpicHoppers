package com.songoda.epichoppers;

import com.songoda.core.SongodaCore;
import com.songoda.core.SongodaPlugin;
import com.songoda.core.commands.CommandManager;
import com.songoda.core.compatibility.CompatibleMaterial;
import com.songoda.core.configuration.Config;
import com.songoda.core.database.DataMigrationManager;
import com.songoda.core.database.DatabaseConnector;
import com.songoda.core.database.MySQLConnector;
import com.songoda.core.database.SQLiteConnector;
import com.songoda.core.gui.GuiManager;
import com.songoda.core.hooks.EconomyManager;
import com.songoda.core.hooks.ProtectionManager;
import com.songoda.core.locale.Locale;
import com.songoda.core.third_party.de.tr7zw.nbtapi.NBTItem;
import com.songoda.core.utils.TextUtils;
import com.songoda.epichoppers.boost.BoostManager;
import com.songoda.epichoppers.commands.CommandBoost;
import com.songoda.epichoppers.commands.CommandGive;
import com.songoda.epichoppers.commands.CommandReload;
import com.songoda.epichoppers.commands.CommandSettings;
import com.songoda.epichoppers.containers.ContainerManager;
import com.songoda.epichoppers.database.DataManager;
import com.songoda.epichoppers.database.migrations._1_InitialMigration;
import com.songoda.epichoppers.hopper.HopperManager;
import com.songoda.epichoppers.hopper.levels.Level;
import com.songoda.epichoppers.hopper.levels.LevelManager;
import com.songoda.epichoppers.hopper.levels.modules.Module;
import com.songoda.epichoppers.hopper.levels.modules.ModuleAutoCrafting;
import com.songoda.epichoppers.hopper.levels.modules.ModuleAutoSell;
import com.songoda.epichoppers.hopper.levels.modules.ModuleAutoSmelter;
import com.songoda.epichoppers.hopper.levels.modules.ModuleBlockBreak;
import com.songoda.epichoppers.hopper.levels.modules.ModuleMobHopper;
import com.songoda.epichoppers.hopper.levels.modules.ModuleSuction;
import com.songoda.epichoppers.hopper.teleport.TeleportHandler;
import com.songoda.epichoppers.listeners.BlockListeners;
import com.songoda.epichoppers.listeners.EntityListeners;
import com.songoda.epichoppers.listeners.HopperListeners;
import com.songoda.epichoppers.listeners.InteractListeners;
import com.songoda.epichoppers.listeners.InventoryListeners;
import com.songoda.epichoppers.player.PlayerDataManager;
import com.songoda.epichoppers.settings.Settings;
import com.songoda.epichoppers.tasks.HopTask;
import com.songoda.epichoppers.utils.Methods;
import com.songoda.skyblock.SkyBlock;
import com.songoda.skyblock.permission.BasicPermission;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.PluginManager;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class EpicHoppers extends SongodaPlugin {

    private static EpicHoppers INSTANCE;
    private final GuiManager guiManager = new GuiManager(this);
    private final Config levelsConfig = new Config(this, "levels.yml");
    private HopperManager hopperManager;
    private CommandManager commandManager;
    private LevelManager levelManager;
    private BoostManager boostManager;
    private PlayerDataManager playerDataManager;
    private ContainerManager containerManager;

    private TeleportHandler teleportHandler;

    private DatabaseConnector databaseConnector;
    private DataManager dataManager;

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

        // Load protection hook
        ProtectionManager.load(this);

        // Setup Config
        Settings.setupConfig();
        this.setLocale(Settings.LANGUGE_MODE.getString(), false);

        // Set Economy & Hologram preference
        EconomyManager.getManager().setPreferredHook(Settings.ECONOMY_PLUGIN.getString());

        // Register commands
        this.commandManager = new CommandManager(this);
        this.commandManager.addMainCommand("eh")
                .addSubCommands(
                        new CommandBoost(this),
                        new CommandGive(this),
                        new CommandReload(this),
                        new CommandSettings(this)
                );

        this.hopperManager = new HopperManager();
        this.playerDataManager = new PlayerDataManager();
        this.containerManager = new ContainerManager(this);
        this.boostManager = new BoostManager();

        // Database stuff, go!
        try {
            if (Settings.MYSQL_ENABLED.getBoolean()) {
                String hostname = Settings.MYSQL_HOSTNAME.getString();
                int port = Settings.MYSQL_PORT.getInt();
                String database = Settings.MYSQL_DATABASE.getString();
                String username = Settings.MYSQL_USERNAME.getString();
                String password = Settings.MYSQL_PASSWORD.getString();
                boolean useSSL = Settings.MYSQL_USE_SSL.getBoolean();
                int poolSize = Settings.MYSQL_POOL_SIZE.getInt();

                this.databaseConnector = new MySQLConnector(this, hostname, port, database, username, password, useSSL, poolSize);
                this.getLogger().info("Data handler connected using MySQL.");
            } else {
                this.databaseConnector = new SQLiteConnector(this);
                this.getLogger().info("Data handler connected using SQLite.");
            }
        } catch (Exception ex) {
            this.getLogger().severe("Fatal error trying to connect to database. Please make sure all your connection settings are correct and try again. Plugin has been disabled.");
            this.emergencyStop();
        }

        this.dataManager = new DataManager(this.databaseConnector, this);
        DataMigrationManager dataMigrationManager = new DataMigrationManager(this.databaseConnector, this.dataManager,
                new _1_InitialMigration());
        dataMigrationManager.runMigrations();

        this.loadLevelManager();

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

        // Start auto save
        int saveInterval = Settings.AUTOSAVE.getInt() * 60 * 20;
        Bukkit.getScheduler().runTaskTimerAsynchronously(this, this::saveModules, saveInterval, saveInterval);

        // Hotfix for EH loading before FSB
        Bukkit.getScheduler().runTask(this, () -> {
            if (pluginManager.isPluginEnabled("FabledSkyBlock")) {
                try {
                    SkyBlock.getInstance().getPermissionManager().registerPermission(
                            (BasicPermission) Class.forName("com.songoda.epichoppers.compatibility.EpicHoppersPermission").getDeclaredConstructor().newInstance());
                } catch (ReflectiveOperationException ex) {
                    ex.printStackTrace();
                }
            }
        });
    }

    @Override
    public void onDataLoad() {
        // Load data from DB
        this.dataManager.getHoppers((hoppers) -> {
            this.hopperManager.addHoppers(hoppers.values());
            this.dataManager.getBoosts((boosts) -> this.boostManager.addBoosts(boosts));

            this.hopperManager.setReady();
        });
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
                } else if (key.equals("MobHopper")) {
                    modules.add(new ModuleMobHopper(this, levels.getInt("MobHopper")));
                } else if (key.equals("AutoSmelting")) {
                    modules.add(new ModuleAutoSmelter(this, levels.getInt("AutoSmelting")));
                }
            }
            levelManager.addLevel(level, costExperience, costEconomy, radius, amount, filter, teleport, linkAmount, modules);
        }
    }

    private void saveModules() {
        if (levelManager != null) {
            for (Level level : levelManager.getLevels().values()) {
                for (Module module : level.getRegisteredModules()) {
                    module.saveDataToFile();
                }
            }
        }
    }

    public ItemStack newHopperItem(Level level) {
        ItemStack item = new ItemStack(CompatibleMaterial.HOPPER.getMaterial());
        ItemMeta itemmeta = item.getItemMeta();
        itemmeta.setDisplayName(TextUtils.formatText(Methods.formatName(level.getLevel())));
        String line = getLocale().getMessage("general.nametag.lore").getMessage();
        if (!line.isEmpty()) {
            itemmeta.setLore(Arrays.asList(line.split("\n")));
        }
        item.setItemMeta(itemmeta);

        NBTItem nbtItem = new NBTItem(item);
        nbtItem.setInteger("level", level.getLevel());
        return nbtItem.getItem();
    }

    @Override
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

    public ContainerManager getContainerManager() {
        return containerManager;
    }
}
