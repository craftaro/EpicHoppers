package com.songoda.epichoppers;

import com.google.common.base.Preconditions;
import com.songoda.epichoppers.api.EpicHoppers;
import com.songoda.epichoppers.api.EpicHoppersAPI;
import com.songoda.epichoppers.api.hopper.HopperManager;
import com.songoda.epichoppers.api.hopper.TeleportTrigger;
import com.songoda.epichoppers.api.hopper.levels.Level;
import com.songoda.epichoppers.api.hopper.levels.LevelManager;
import com.songoda.epichoppers.api.hopper.levels.modules.Module;
import com.songoda.epichoppers.boost.BoostData;
import com.songoda.epichoppers.boost.BoostManager;
import com.songoda.epichoppers.command.CommandManager;
import com.songoda.epichoppers.enchantment.Enchantment;
import com.songoda.epichoppers.handlers.HopHandler;
import com.songoda.epichoppers.handlers.TeleportHandler;
import com.songoda.epichoppers.hook.HookManager;
import com.songoda.epichoppers.hopper.EFilter;
import com.songoda.epichoppers.hopper.EHopper;
import com.songoda.epichoppers.hopper.EHopperManager;
import com.songoda.epichoppers.hopper.levels.ELevelManager;
import com.songoda.epichoppers.hopper.levels.modules.ModuleAutoCrafting;
import com.songoda.epichoppers.hopper.levels.modules.ModuleAutoSell;
import com.songoda.epichoppers.hopper.levels.modules.ModuleBlockBreak;
import com.songoda.epichoppers.hopper.levels.modules.ModuleSuction;
import com.songoda.epichoppers.listeners.*;
import com.songoda.epichoppers.player.PlayerDataManager;
import com.songoda.epichoppers.storage.Storage;
import com.songoda.epichoppers.storage.StorageRow;
import com.songoda.epichoppers.storage.types.StorageMysql;
import com.songoda.epichoppers.storage.types.StorageYaml;
import com.songoda.epichoppers.utils.ConfigWrapper;
import com.songoda.epichoppers.utils.Methods;
import com.songoda.epichoppers.utils.Metrics;
import com.songoda.epichoppers.utils.SettingsManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;


public class EpicHoppersPlugin extends JavaPlugin implements EpicHoppers {
    private static CommandSender console = Bukkit.getConsoleSender();

    private static EpicHoppersPlugin INSTANCE;
    public References references = null;
    public Enchantment enchantmentHandler;
    private SettingsManager settingsManager;
    private ConfigWrapper hooksFile = new ConfigWrapper(this, "", "hooks.yml");
    private ConfigWrapper levelsFile = new ConfigWrapper(this, "", "levels.yml");
    private Locale locale;

    private HopperManager hopperManager;
    private CommandManager commandManager;
    private LevelManager levelManager;
    private BoostManager boostManager;
    private PlayerDataManager playerDataManager;
    private HookManager hookManager;

    private TeleportHandler teleportHandler;

    private Storage storage;

    private boolean liquidtanks = false;

    public static EpicHoppersPlugin getInstance() {
        return INSTANCE;
    }

    private boolean checkVersion() {
        int workingVersion = 13;
        int currentVersion = Integer.parseInt(Bukkit.getServer().getClass()
                .getPackage().getName().split("\\.")[3].split("_")[1]);

        if (currentVersion < workingVersion) {
            Bukkit.getScheduler().scheduleSyncDelayedTask(this, () -> {
                Bukkit.getConsoleSender().sendMessage("");
                Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "You installed the 1." + workingVersion + "+ only version of " + this.getDescription().getName() + " on a 1." + currentVersion + " server. Since you are on the wrong version we disabled the plugin for you. Please install correct version to continue using " + this.getDescription().getName() + ".");
                Bukkit.getConsoleSender().sendMessage("");
            }, 20L);
            return false;
        }
        return true;
    }

    @Override
    public void onEnable() {
        INSTANCE = this;
        EpicHoppersAPI.setImplementation(this);

        // Check to make sure the Bukkit version is compatible.
        if (!checkVersion()) return;

        console.sendMessage(Methods.formatText("&a============================="));
        console.sendMessage(Methods.formatText("&7EpicHoppers " + this.getDescription().getVersion() + " by &5Songoda <3&7!"));
        console.sendMessage(Methods.formatText("&7Action: &aEnabling&7..."));

        this.settingsManager = new SettingsManager(this);
        this.setupConfig();

        // Setup language
        String langMode = getConfig().getString("System.Language Mode");
        Locale.init(this);
        Locale.saveDefaultLocale("en_US");
        this.locale = Locale.getLocale(getConfig().getString("System.Language Mode", langMode));

        if (getConfig().getBoolean("System.Download Needed Data Files"))
            this.update();

        this.enchantmentHandler = new Enchantment();
        this.hopperManager = new EHopperManager();
        this.playerDataManager = new PlayerDataManager();
        this.boostManager = new BoostManager();
        this.references = new References();
        this.commandManager = new CommandManager(this);
        this.hookManager = new HookManager(this);

        this.loadLevelManager();
        this.checkStorage();

        // Load from file
        loadFromFile();

        new HopHandler(this);
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
        int saveInterval = getConfig().getInt("Main.Auto Save Interval In Seconds") * 60 * 20;
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

                    Material autoCrafting = Material.valueOf(row.get("autocrafting").asString() == null ? "AIR" : row.get("autocrafting").asString());

                    String blackLoc = row.get("black").asString();
                    Location black = blackLoc == null ? null : Methods.unserializeLocation(blackLoc);

                    EFilter filter = new EFilter();

                    filter.setWhiteList(whiteList);
                    filter.setBlackList(blackList);
                    filter.setVoidList(voidList);
                    filter.setEndPoint(black);

                    EHopper hopper = new EHopper(location, levelManager.getLevel(level), lastPlayer, placedBy, blocks, filter, teleportTrigger, autoCrafting);

                    hopperManager.addHopper(location, hopper);
                }
            }

            // Adding in Boosts
            if (storage.containsGroup("boosts")) {
                for (StorageRow row : storage.getRowsByGroup("boosts")) {
                    if (row.getItems().get("uuid").asObject() != null)
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
        File folder = getDataFolder();
        File voucherFile = new File(folder, "levels.yml");
        if (!voucherFile.exists()) {
            saveResource("levels.yml", true);
        }

        // Load an instance of LevelManager
        levelManager = new ELevelManager();
        /*
         * Register Levels into LevelManager from configuration.
         */
        ((ELevelManager) levelManager).clear();
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

    private void setupConfig() {
        settingsManager.updateSettings();
        this.getConfig().options().copyDefaults(true);
        this.saveConfig();
    }

    public void reload() {
        String langMode = getConfig().getString("System.Language Mode");
        this.locale = Locale.getLocale(getConfig().getString("System.Language Mode", langMode));
        this.locale.reloadMessages();
        this.hookManager = new HookManager(this);
        references = new References();
        this.setupConfig();
        loadLevelManager();
    }

    @Override
    public Level getLevelFromItem(ItemStack item) {
        if (item.getItemMeta().getDisplayName().contains(":")) {
            String arr[] = item.getItemMeta().getDisplayName().replace(String.valueOf(ChatColor.COLOR_CHAR), "").split(":");
            return getLevelManager().getLevel(Integer.parseInt(arr[0]));
        } else {
            return getLevelManager().getLowestLevel();
        }
    }

    @Override
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

    public HookManager getHookManager() {
        return hookManager;
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

    public boolean isLiquidtanks() {
        return liquidtanks;
    }

}
