package com.songoda.epichoppers;

import com.songoda.epichoppers.boost.BoostData;
import com.songoda.epichoppers.boost.BoostManager;
import com.songoda.epichoppers.command.CommandManager;
import com.songoda.epichoppers.economy.Economy;
import com.songoda.epichoppers.economy.PlayerPointsEconomy;
import com.songoda.epichoppers.economy.VaultEconomy;
import com.songoda.epichoppers.enchantment.Enchantment;
import com.songoda.epichoppers.handlers.TeleportHandler;
import com.songoda.epichoppers.hopper.Filter;
import com.songoda.epichoppers.hopper.Hopper;
import com.songoda.epichoppers.hopper.HopperManager;
import com.songoda.epichoppers.hopper.levels.Level;
import com.songoda.epichoppers.hopper.levels.LevelManager;
import com.songoda.epichoppers.hopper.levels.modules.*;
import com.songoda.epichoppers.listeners.*;
import com.songoda.epichoppers.player.PlayerDataManager;
import com.songoda.epichoppers.storage.Storage;
import com.songoda.epichoppers.storage.StorageRow;
import com.songoda.epichoppers.storage.types.StorageMysql;
import com.songoda.epichoppers.storage.types.StorageYaml;
import com.songoda.epichoppers.tasks.HopTask;
import com.songoda.epichoppers.utils.*;
import com.songoda.epichoppers.utils.settings.Setting;
import com.songoda.epichoppers.utils.settings.SettingsManager;
import org.apache.commons.lang.ArrayUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;


public class EpicHoppers extends JavaPlugin {
    private static CommandSender console = Bukkit.getConsoleSender();

    private static EpicHoppers INSTANCE;

    private ServerVersion serverVersion = ServerVersion.fromPackageName(Bukkit.getServer().getClass().getPackage().getName());

    public References references = null;
    public Enchantment enchantmentHandler;
    private SettingsManager settingsManager;
    private ConfigWrapper levelsFile = new ConfigWrapper(this, "", "levels.yml");
    private Locale locale;

    private HopperManager hopperManager;
    private CommandManager commandManager;
    private LevelManager levelManager;
    private BoostManager boostManager;
    private PlayerDataManager playerDataManager;

    private Economy economy;

    private TeleportHandler teleportHandler;

    private Storage storage;

    private boolean liquidtanks = false;

    public static EpicHoppers getInstance() {
        return INSTANCE;
    }

    @Override
    public void onEnable() {
        INSTANCE = this;

        console.sendMessage(Methods.formatText("&a============================="));
        console.sendMessage(Methods.formatText("&7EpicHoppers " + this.getDescription().getVersion() + " by &5Songoda <3&7!"));
        console.sendMessage(Methods.formatText("&7Action: &aEnabling&7..."));

        this.settingsManager = new SettingsManager(this);
        this.settingsManager.setupConfig();

        // Setup language
        String langMode = getConfig().getString("System.Language Mode");
        Locale.init(this);
        Locale.saveDefaultLocale("en_US");
        this.locale = Locale.getLocale(getConfig().getString("System.Language Mode", langMode));

        if (getConfig().getBoolean("System.Download Needed Data Files"))
            this.update();

        this.enchantmentHandler = new Enchantment();
        this.hopperManager = new HopperManager();
        this.playerDataManager = new PlayerDataManager();
        this.boostManager = new BoostManager();
        this.references = new References();
        this.commandManager = new CommandManager(this);

        // Setup Economy
        if (Setting.VAULT_ECONOMY.getBoolean()
                && getServer().getPluginManager().getPlugin("Vault") != null)
            this.economy = new VaultEconomy(this);
        else if (Setting.PLAYER_POINTS_ECONOMY.getBoolean()
                && getServer().getPluginManager().getPlugin("PlayerPoints") != null)
            this.economy = new PlayerPointsEconomy(this);

        this.loadLevelManager();
        this.checkStorage();

        // Load from file
        loadFromFile();

        new HopTask(this);
        this.teleportHandler = new TeleportHandler(this);

        PluginManager pluginManager = Bukkit.getPluginManager();

        // Register Listeners
        pluginManager.registerEvents(new HopperListeners(this), this);
        pluginManager.registerEvents(new EntityListeners(this), this);
        pluginManager.registerEvents(new BlockListeners(this), this);
        pluginManager.registerEvents(new InteractListeners(this), this);
        pluginManager.registerEvents(new InventoryListeners(this), this);

        // Check for liquid tanks
        if (pluginManager.isPluginEnabled("LiquidTanks")) liquidtanks = true;

        // Start auto save
        int saveInterval = Setting.AUTOSAVE.getInt() * 60 * 20;
        Bukkit.getScheduler().runTaskTimerAsynchronously(this, this::saveToFile, saveInterval, saveInterval);

        // Start Metrics
        new Metrics(this);

        console.sendMessage(Methods.formatText("&a============================="));
    }

    @Override
    public void onDisable() {
        this.saveToFile();
        this.storage.closeConnection();

        console.sendMessage(Methods.formatText("&a============================="));
        console.sendMessage(Methods.formatText("&7EpicHoppers " + this.getDescription().getVersion() + " by &5Songoda <3!"));
        console.sendMessage(Methods.formatText("&7Action: &cDisabling&7..."));
        console.sendMessage(Methods.formatText("&a============================="));
    }

    private void update() {
        try {
            URL url = new URL("http://update.songoda.com/index.php?plugin=" + getDescription().getName() + "&version=" + getDescription().getVersion());
            URLConnection urlConnection = url.openConnection();
            InputStream is = urlConnection.getInputStream();
            InputStreamReader isr = new InputStreamReader(is);

            int numCharsRead;
            char[] charArray = new char[1024];
            StringBuffer sb = new StringBuffer();
            while ((numCharsRead = isr.read(charArray)) > 0) {
                sb.append(charArray, 0, numCharsRead);
            }
            String jsonString = sb.toString();
            JSONObject json = (JSONObject) new JSONParser().parse(jsonString);

            JSONArray files = (JSONArray) json.get("neededFiles");
            for (Object o : files) {
                JSONObject file = (JSONObject) o;

                switch ((String) file.get("type")) {
                    case "locale":
                        InputStream in = new URL((String) file.get("link")).openStream();
                        Locale.saveDefaultLocale(in, (String) file.get("name"));
                        break;
                }
            }
        } catch (Exception e) {
            System.out.println("Failed to update.");
            //e.printStackTrace();
        }
    }


    public ServerVersion getServerVersion() {
        return serverVersion;
    }

    public boolean isServerVersion(ServerVersion version) {
        return serverVersion == version;
    }

    public boolean isServerVersion(ServerVersion... versions) {
        return ArrayUtils.contains(versions, serverVersion);
    }

    public boolean isServerVersionAtLeast(ServerVersion version) {
        return serverVersion.ordinal() >= version.ordinal();
    }

    private void checkStorage() {
        if (getConfig().getBoolean("Database.Activate Mysql Support")) {
            this.storage = new StorageMysql(this);
        } else {
            this.storage = new StorageYaml(this);
        }
    }

    /*
     * Saves registered hoppers to file.
     */
    private void saveToFile() {
        checkStorage();

        storage.doSave();
    }

    private void loadFromFile() {
        /*
         * Register hoppers into HopperManger from configuration
         */
        Bukkit.getScheduler().runTaskLater(this, () -> {
            if (storage.containsGroup("sync")) {
                for (StorageRow row : storage.getRowsByGroup("sync")) {
                    Location location = Methods.unserializeLocation(row.getKey());
                    if (location == null) return;

                    int level = row.get("level").asInt();

                    List<String> blockLoc = row.get("block").asStringList();
                    List<Location> blocks = new ArrayList<>();
                    if (blockLoc != null) {
                        for (String string : blockLoc) {
                            blocks.add(Methods.unserializeLocation(string));
                        }
                    }

                    TeleportTrigger teleportTrigger = TeleportTrigger.valueOf(row.get("teleporttrigger").asString() == null ? "DISABLED" : row.get("teleporttrigger").asString());

                    String playerStr = row.get("player").asString();
                    String placedByStr = row.get("placedby").asString();
                    UUID lastPlayer = playerStr == null ? null : UUID.fromString(playerStr);
                    UUID placedBy = placedByStr == null ? null : UUID.fromString(placedByStr);

                    List<ItemStack> whiteList = row.get("whitelist").asItemStackList();
                    List<ItemStack> blackList = row.get("blacklist").asItemStackList();
                    List<ItemStack> voidList = row.get("void").asItemStackList();

                    String autoCraftingStr = row.get("autocrafting").asString() == null ? "AIR" : row.get("autocrafting").asString();

                    String[] autoCraftingParts = autoCraftingStr.split(":");

                    ItemStack autoCrafting = new ItemStack(Material.valueOf(autoCraftingParts[0]), 1, Short.parseShort(autoCraftingParts.length == 2 ? autoCraftingParts[1] : "0"));

                    String blackLoc = row.get("black").asString();
                    Location black = blackLoc == null ? null : Methods.unserializeLocation(blackLoc);

                    boolean autoBreak = row.get("autobreak").asBoolean();

                    Filter filter = new Filter();

                    filter.setWhiteList(whiteList);
                    filter.setBlackList(blackList);
                    filter.setVoidList(voidList);
                    filter.setEndPoint(black);

                    Hopper hopper = new Hopper(location, levelManager.getLevel(level), lastPlayer, placedBy, blocks, filter, teleportTrigger, autoCrafting);

                    if (autoBreak) hopper.toggleAutoBreaking();

                    hopperManager.addHopper(location, hopper);
                }
            }

            // Adding in Boosts
            if (storage.containsGroup("boosts")) {
                for (StorageRow row : storage.getRowsByGroup("boosts")) {
                    if (row.get("uuid").asObject() == null)
                        continue;

                    BoostData boostData = new BoostData(
                            row.get("amount").asInt(),
                            Long.parseLong(row.getKey()),
                            UUID.fromString(row.get("uuid").asString()));

                    this.boostManager.addBoostToPlayer(boostData);
                }
            }

            // Save data initially so that if the person reloads again fast they don't lose all their data.
            this.saveToFile();
        }, 10);
    }

    private void loadLevelManager() {
        saveResource("levels.yml", false);

        // Load an instance of LevelManager
        levelManager = new LevelManager();
        /*
         * Register Levels into LevelManager from configuration.
         */
        levelManager.clear();
        for (String levelName : levelsFile.getConfig().getKeys(false)) {
            int level = Integer.valueOf(levelName.split("-")[1]);

            ConfigurationSection levels = levelsFile.getConfig().getConfigurationSection(levelName);

            int radius = levels.getInt("Range");
            int amount = levels.getInt("Amount");
            int linkAmount = levels.getInt("Link-amount", 1);
            boolean filter = levels.getBoolean("Filter");
            boolean teleport = levels.getBoolean("Teleport");
            int costExperiance = levels.getInt("Cost-xp", -1);
            int costEconomy = levels.getInt("Cost-eco", -1);
            int autoSell = levels.getInt("AutoSell");

            ArrayList<Module> modules = new ArrayList<>();

            for (String key : levels.getKeys(false)) {
                if (key.equals("Suction") && levels.getInt("Suction") != 0) {
                    modules.add(new ModuleSuction(levels.getInt("Suction")));
                } else if (key.equals("BlockBreak") && levels.getInt("BlockBreak") != 0) {
                    modules.add(new ModuleBlockBreak(levels.getInt("BlockBreak")));
                } else if (key.equals("AutoCrafting")) {
                    modules.add(new ModuleAutoCrafting());
                } else if (key.equals("AutoSell")) {
                    modules.add(new ModuleAutoSell(autoSell));
                }

            }
            levelManager.addLevel(level, costExperiance, costEconomy, radius, amount, filter, teleport, linkAmount, autoSell, modules);
        }
    }

    public void reload() {
        String langMode = getConfig().getString("System.Language Mode");
        this.locale = Locale.getLocale(getConfig().getString("System.Language Mode", langMode));
        this.locale.reloadMessages();
        references = new References();
        this.settingsManager.reloadConfig();
        loadLevelManager();
    }

    public ItemStack newHopperItem(Level level) {
        ItemStack item = new ItemStack(Material.HOPPER, 1);
        ItemMeta itemmeta = item.getItemMeta();
        itemmeta.setDisplayName(Methods.formatText(Methods.formatName(level.getLevel(), true)));
        String line = getLocale().getMessage("general.nametag.lore");
        if (!line.equals("")) {
            itemmeta.setLore(Arrays.asList(line.split("\n")));
        }
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

    public CommandManager getCommandManager() {
        return commandManager;
    }

    public LevelManager getLevelManager() {
        return levelManager;
    }

    public HopperManager getHopperManager() {
        return hopperManager;
    }

    public SettingsManager getSettingsManager() {
        return settingsManager;
    }

    public Economy getEconomy() {
        return economy;
    }

    public PlayerDataManager getPlayerDataManager() {
        return playerDataManager;
    }

    public boolean isLiquidtanks() {
        return liquidtanks;
    }

}
