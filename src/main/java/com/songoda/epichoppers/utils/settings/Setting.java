package com.songoda.epichoppers.utils.settings;

import com.songoda.epichoppers.EpicHoppers;
import com.songoda.epichoppers.utils.ServerVersion;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public enum Setting {

    HOPPER_UPGRADING("Main.Allow hopper Upgrading", true,
            "Should hoppers be upgradable?"),

    UPGRADE_WITH_ECONOMY("Main.Upgrade With Economy", true,
            "Should you be able to upgrade hoppers with economy?"),

    UPGRADE_WITH_XP("Main.Upgrade With XP", true,
            "Should you be able to upgrade hoppers with experience?"),

    TELEPORT("Main.Allow Players To Teleport Through Hoppers", true,
            "Should players be able to teleport through hoppers?"),

    ENDERCHESTS("Main.Support Enderchests", true,
            "Should hoppers dump items into a player enderchests?"),

    PARTICLE_TYPE("Main.Upgrade Particle Type", "SPELL_WITCH",
            "The type of particle shown when a hopper is upgraded."),

    HOP_TICKS("Main.Amount of Ticks Between Hops", 8L,
            "The amount of ticks between hopper transfers."),

    AUTOSAVE("Main.Auto Save Interval In Seconds", 15,
            "The amount of time in between saving to file.",
            "This is purely a safety function to prevent against unplanned crashes or",
            "restarts. With that said it is advised to keep this enabled.",
            "If however you enjoy living on the edge, feel free to turn it off."),

    TELEPORT_TICKS("Main.Amount of Ticks Between Teleport", 10L,
            "The cooldown between teleports. It prevents players",
            "from getting stuck in a teleport loop."),

    SYNC_TIMEOUT("Main.Timeout When Syncing Hoppers", 300L,
            "The amount of time in ticks a player has between hitting the hopper",
            "Link button and performing the link. When the time is up the link event is canceled."),

    MAX_CHUNK("Main.Max Hoppers Per Chunk", -1,
            "The maximum amount of hoppers per chunk."),

    BLOCKBREAK_PARTICLE("Main.BlockBreak Particle Type", "LAVA",
            "The particle shown when the block break module performs a block break."),

    BLACKLIST("Main.BlockBreak Blacklisted Blocks",
            Arrays.asList("BEDROCK", "END_PORTAL", "ENDER_PORTAL", "END_PORTAL_FRAME", "ENDER_PORTAL_FRAME", "PISTON_HEAD", "PISTON_EXTENSION", "RAIL", "RAILS", "ACTIVATOR_RAIL", "DETECTOR_RAIL", "POWERED_RAIL"),
            "Anything listed here will not be broken by the block break module."),

    AUTOSELL_PRICES("Main.AutoSell Prices",
            Arrays.asList("STONE,0.50", "COBBLESTONE,0.20", "IRON_ORE,0.35", "COAL_ORE,0.20"),
            "These are the prices used by the auto sell module."),

    VAULT_ECONOMY("Economy.Use Vault Economy", true,
            "Should Vault be used?"),

    PLAYER_POINTS_ECONOMY("Economy.Use Player Points Economy", false,
            "Should PlayerPoints be used?"),

    RAINBOW("Interfaces.Replace Glass Type 1 With Rainbow Glass", false),
    ECO_ICON("Interfaces.Economy Icon", EpicHoppers.getInstance().isServerVersionAtLeast(ServerVersion.V1_13) ? "SUNFLOWER" : "DOUBLE_PLANT"),
    XP_ICON("Interfaces.XP Icon", EpicHoppers.getInstance().isServerVersionAtLeast(ServerVersion.V1_13) ? "EXPERIENCE_BOTTLE" : "EXP_BOTTLE"),
    GLASS_TYPE_1("Interfaces.Glass Type 1", 7),
    GLASS_TYPE_2("Interfaces.Glass Type 2", 11),
    GLASS_TYPE_3("Interfaces.Glass Type 3", 3),

    DATABASE_SUPPORT("Database.Activate Mysql Support", false,
            "Should MySQL be used for data storage?"),

    DATABASE_IP("Database.IP", "127.0.0.1",
            "MySQL IP"),

    DATABASE_PORT("Database.Port", 3306,
            "MySQL Port"),

    DATABASE_NAME("Database.Database Name", "EpicHoppers",
            "The database you are inserting data into."),

    DATABASE_PREFIX("Database.Prefix", "EH-",
            "The prefix for tables inserted into the database."),

    DATABASE_USERNAME("Database.Username", "PUT_USERNAME_HERE",
            "MySQL Username"),

    DATABASE_PASSWORD("Database.Password", "PUT_PASSWORD_HERE",
            "MySQL Password"),

    LANGUGE_MODE("System.Language Mode", "en_US",
            "The enabled language file.",
            "More language files (if available) can be found in the plugins data folder.");

    private String setting;
    private Object option;
    private String[] comments;

    Setting(String setting, Object option, String... comments) {
        this.setting = setting;
        this.option = option;
        this.comments = comments;
    }

    Setting(String setting, Object option) {
        this.setting = setting;
        this.option = option;
        this.comments = null;
    }

    public static Setting getSetting(String setting) {
        List<Setting> settings = Arrays.stream(values()).filter(setting1 -> setting1.setting.equals(setting)).collect(Collectors.toList());
        if (settings.isEmpty()) return null;
        return settings.get(0);
    }

    public String getSetting() {
        return setting;
    }

    public Object getOption() {
        return option;
    }

    public String[] getComments() {
        return comments;
    }

    public List<String> getStringList() {
        return EpicHoppers.getInstance().getConfig().getStringList(setting);
    }

    public boolean getBoolean() {
        return EpicHoppers.getInstance().getConfig().getBoolean(setting);
    }

    public int getInt() {
        return EpicHoppers.getInstance().getConfig().getInt(setting);
    }

    public long getLong() {
        return EpicHoppers.getInstance().getConfig().getLong(setting);
    }

    public String getString() {
        return EpicHoppers.getInstance().getConfig().getString(setting);
    }

    public char getChar() {
        return EpicHoppers.getInstance().getConfig().getString(setting).charAt(0);
    }

    public double getDouble() {
        return EpicHoppers.getInstance().getConfig().getDouble(setting);
    }
}