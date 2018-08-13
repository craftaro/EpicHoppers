package com.songoda.epichoppers;

import com.songoda.arconix.api.mcupdate.MCUpdate;
import com.songoda.arconix.api.utils.ConfigWrapper;
import com.songoda.arconix.plugin.Arconix;
import com.songoda.epichoppers.api.EpicHoppers;
import com.songoda.epichoppers.api.EpicHoppersAPI;
import com.songoda.epichoppers.api.hopper.Hopper;
import com.songoda.epichoppers.api.hopper.HopperManager;
import com.songoda.epichoppers.api.hopper.Level;
import com.songoda.epichoppers.api.hopper.LevelManager;
import com.songoda.epichoppers.events.*;
import com.songoda.epichoppers.handlers.*;
import com.songoda.epichoppers.hopper.EFilter;
import com.songoda.epichoppers.hopper.EHopper;
import com.songoda.epichoppers.hopper.EHopperManager;
import com.songoda.epichoppers.hopper.ELevelManager;
import com.songoda.epichoppers.player.PlayerDataManager;
import com.songoda.epichoppers.utils.Methods;
import com.songoda.epichoppers.utils.SettingsManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;


public class EpicHoppersPlugin extends JavaPlugin implements EpicHoppers {
    public static CommandSender console = Bukkit.getConsoleSender();

    private static EpicHoppersPlugin INSTANCE;

    public HookHandler hooks;
    public SettingsManager settingsManager;

    public References references = null;
    public ConfigWrapper dataFile = new ConfigWrapper(this, "", "data.yml");

    public EnchantmentHandler enchantmentHandler;

    private Locale locale;

    private HopperManager hopperManager;
    private LevelManager levelManager;
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

                boolean walkOnTeleport = dataFile.getConfig().getBoolean("data.sync." + locationStr + ".walkOnTeleport");

                String playerStr = dataFile.getConfig().getString("data.sync." + locationStr + ".player");
                UUID player = playerStr == null ? null : UUID.fromString(playerStr);

                List<ItemStack> whiteList = (ArrayList<ItemStack>)dataFile.getConfig().getList("data.sync." + locationStr + ".whitelist");
                List<ItemStack> blackList = (ArrayList<ItemStack>)dataFile.getConfig().getList("data.sync." + locationStr + ".blacklist");
                List<ItemStack> voidList = (ArrayList<ItemStack>)dataFile.getConfig().getList("data.sync." + locationStr + ".void");

                String blackLoc = dataFile.getConfig().getString("data.sync." + locationStr + ".black");
                Block black = blackLoc == null ? null : Arconix.pl().getApi().serialize().unserializeLocation(dataFile.getConfig().getString("data.sync." + locationStr + ".black")).getBlock();

                EFilter filter = new EFilter();

                filter.setWhiteList(whiteList);
                filter.setBlackList(blackList);
                filter.setVoidList(voidList);
                filter.setEndPoint(black);

                EHopper hopper = new EHopper(location, levelManager.getLevel(level), player, block, filter, walkOnTeleport);

                hopperManager.addHopper(location, hopper);
            }
        }

        }, 10);

        references = new References();

        hooks = new HookHandler();
        hooks.hook();

        new HopHandler(this);
        teleportHandler = new TeleportHandler(this);

        new MCUpdate(this, true);
        //new MassiveStats(this, 9000);

        Bukkit.getScheduler().scheduleSyncRepeatingTask(this, this::saveToFile, 6000, 6000);

        this.getCommand("EpicHoppers").setExecutor(new CommandHandler(this));

        getServer().getPluginManager().registerEvents(new HopperListeners(this), this);
        getServer().getPluginManager().registerEvents(new BlockListeners(this), this);
        getServer().getPluginManager().registerEvents(new InteractListeners(this), this);
        getServer().getPluginManager().registerEvents(new InventoryListeners(this), this);
        getServer().getPluginManager().registerEvents(new LoginListeners(this), this);


        console.sendMessage(Arconix.pl().getApi().format().formatText("&a============================="));
    }

    public void onDisable() {
        saveToFile();
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
            if (hopper.getLevel() == null || hopper.getLocation() == null || hopper.getLocation().getChunk() == null) continue;
            String locationStr = Arconix.pl().getApi().serialize().serializeLocation(hopper.getLocation());
            dataFile.getConfig().set("data.sync." + locationStr + ".level", hopper.getLevel().getLevel());
            dataFile.getConfig().set("data.sync." + locationStr + ".block", hopper.getSyncedBlock() == null ? null : Arconix.pl().getApi().serialize().serializeLocation(hopper.getSyncedBlock().getLocation()));
            dataFile.getConfig().set("data.sync." + locationStr + ".player", hopper.getLastPlayer() == null ? null : hopper.getLastPlayer().toString());
            dataFile.getConfig().set("data.sync." + locationStr + ".walkOnTeleport", hopper.isWalkOnTeleport());
            dataFile.getConfig().set("data.sync." + locationStr + ".whitelist", hopper.getFilter().getWhiteList());
            dataFile.getConfig().set("data.sync." + locationStr + ".blacklist", hopper.getFilter().getBlackList());
            dataFile.getConfig().set("data.sync." + locationStr + ".void", hopper.getFilter().getVoidList());
            dataFile.getConfig().set("data.sync." + locationStr + ".black", hopper.getFilter().getEndPoint() == null ? null : Arconix.pl().getApi().serialize().serializeLocation(hopper.getFilter().getEndPoint().getLocation()));
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
        ((ELevelManager)levelManager).clear();
        for (String levelName : getConfig().getConfigurationSection("settings.levels").getKeys(false)) {
            int level = Integer.valueOf(levelName.split("-")[1]);
            int radius = getConfig().getInt("settings.levels." + levelName + ".Range");
            int amount = getConfig().getInt("settings.levels." + levelName + ".Amount");
            int suction = getConfig().getInt("settings.levels." + levelName + ".Suction");
            int blockBreak = getConfig().getInt("settings.levels." + levelName + ".BlockBreak");
            int costExperiance = getConfig().getInt("settings.levels." + levelName + ".Cost-xp");
            int costEconomy = getConfig().getInt("settings.levels." + levelName + ".Cost-eco");
            levelManager.addLevel(level, costExperiance, costEconomy, radius, amount, suction, blockBreak);
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
        hooks.hooksFile.createNewFile("Loading hooks File", "EpicSpawners Spawners File");
        hooks = new HookHandler();
        hooks.hook();
        references = new References();
        reloadConfig();
        saveConfig();
        loadLevelManager();
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

    @Override
    public LevelManager getLevelManager() {
        return levelManager;
    }

    @Override
    public HopperManager getHopperManager() {
        return hopperManager;
    }

    public PlayerDataManager getPlayerDataManager() {
        return playerDataManager;
    }

    public static EpicHoppersPlugin getInstance() {
        return INSTANCE;
    }
}
