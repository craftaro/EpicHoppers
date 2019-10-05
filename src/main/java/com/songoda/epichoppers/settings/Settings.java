package com.songoda.epichoppers.settings;

import com.songoda.core.compatibility.CompatibleMaterial;
import com.songoda.core.configuration.Config;
import com.songoda.core.configuration.ConfigSetting;
import com.songoda.core.hooks.EconomyManager;
import com.songoda.epichoppers.EpicHoppers;

import java.util.Arrays;
import java.util.stream.Collectors;

public class Settings {

    static final Config config = EpicHoppers.getInstance().getCoreConfig();

    public static final ConfigSetting HOPPER_UPGRADING = new ConfigSetting(config, "Main.Allow hopper Upgrading", true,
            "Should hoppers be upgradable?");

    public static final ConfigSetting UPGRADE_WITH_ECONOMY = new ConfigSetting(config, "Main.Upgrade With Economy", true,
            "Should you be able to upgrade hoppers with economy?");

    public static final ConfigSetting UPGRADE_WITH_XP = new ConfigSetting(config, "Main.Upgrade With XP", true,
            "Should you be able to upgrade hoppers with experience?");

    public static final ConfigSetting TELEPORT = new ConfigSetting(config, "Main.Allow Players To Teleport Through Hoppers", true,
            "Should players be able to teleport through hoppers?");

    public static final ConfigSetting ENDERCHESTS = new ConfigSetting(config, "Main.Support Enderchests", true,
            "Should hoppers dump items into a player enderchests?");

    public static final ConfigSetting PARTICLE_TYPE = new ConfigSetting(config, "Main.Upgrade Particle Type", "SPELL_WITCH",
            "The type of particle shown when a hopper is upgraded.");

    public static final ConfigSetting HOP_TICKS = new ConfigSetting(config, "Main.Amount of Ticks Between Hops", 8L,
            "The amount of ticks between hopper transfers.");

    public static final ConfigSetting AUTOSAVE = new ConfigSetting(config, "Main.Auto Save Interval In Seconds", 15,
            "The amount of time in between saving to file.",
            "This is purely a safety function to prevent against unplanned crashes or",
            "restarts. With that said it is advised to keep this enabled.",
            "If however you enjoy living on the edge, feel free to turn it off.");

    public static final ConfigSetting TELEPORT_TICKS = new ConfigSetting(config, "Main.Amount of Ticks Between Teleport", 10L,
            "The cooldown between teleports. It prevents players",
            "from getting stuck in a teleport loop.");

    public static final ConfigSetting SYNC_TIMEOUT = new ConfigSetting(config, "Main.Timeout When Syncing Hoppers", 300L,
            "The amount of time in ticks a player has between hitting the hopper",
            "Link button and performing the link. When the time is up the link event is canceled.");

    public static final ConfigSetting MAX_CHUNK = new ConfigSetting(config, "Main.Max Hoppers Per Chunk", -1,
            "The maximum amount of hoppers per chunk.");

    public static final ConfigSetting BLOCKBREAK_PARTICLE = new ConfigSetting(config, "Main.BlockBreak Particle Type", "LAVA",
            "The particle shown when the block break module performs a block break.");

    public static final ConfigSetting BLACKLIST = new ConfigSetting(config, "Main.BlockBreak Blacklisted Blocks",
            Arrays.asList("BEDROCK", "END_PORTAL", "ENDER_PORTAL", "END_PORTAL_FRAME", "ENDER_PORTAL_FRAME", "PISTON_HEAD", "PISTON_EXTENSION", "RAIL", "RAILS", "ACTIVATOR_RAIL", "DETECTOR_RAIL", "POWERED_RAIL"),
            "Anything listed here will not be broken by the block break module.");

    public static final ConfigSetting AUTOCRAFT_JAM_EJECT = new ConfigSetting(config, "Main.AutoCraft Jam Eject", false,
            "AutoCraft module needs a free slot to craft items with.",
            "Normally, crafting hoppers won't grab items that would fill that slot.",
            "This option ejects items if that last slot is forcibly filled");

    public static final ConfigSetting AUTOSELL_PRICES = new ConfigSetting(config, "Main.AutoSell Prices",
            Arrays.asList("STONE,0.50", "COBBLESTONE,0.20", "IRON_ORE,0.35", "COAL_ORE,0.20"),
            "These are the prices used by the auto sell module.");

    public static final ConfigSetting AUTOSELL_SHOPGUIPLUS = new ConfigSetting(config, "Main.Use ShopGuiPlus for Prices", false,
            "Should prices be grabbed from ShopGuiPlus?",
            "If ShopGuiPlus is not enabled or the player is offline the default price list will be used.",
            "If this is something that you do not want then you should empty the default list.");

    public static final ConfigSetting ECONOMY_PLUGIN = new ConfigSetting(config, "Main.Economy", EconomyManager.getEconomy() == null ? "Vault" : EconomyManager.getEconomy().getName(),
            "Which economy plugin should be used?",
            "Supported plugins you have installed: \"" + EconomyManager.getManager().getRegisteredPlugins().stream().collect(Collectors.joining("\", \"")) + "\".");

    public static final ConfigSetting ECO_ICON = new ConfigSetting(config, "Interfaces.Economy Icon", "SUNFLOWER");
    public static final ConfigSetting XP_ICON = new ConfigSetting(config, "Interfaces.XP Icon", "EXPERIENCE_BOTTLE");

    public static final ConfigSetting GLASS_TYPE_1 = new ConfigSetting(config, "Interfaces.Glass Type 1", "GRAY_STAINED_GLASS_PANE");
    public static final ConfigSetting GLASS_TYPE_2 = new ConfigSetting(config, "Interfaces.Glass Type 2", "BLUE_STAINED_GLASS_PANE");
    public static final ConfigSetting GLASS_TYPE_3 = new ConfigSetting(config, "Interfaces.Glass Type 3", "LIGHT_BLUE_STAINED_GLASS_PANE");

    public static final ConfigSetting LANGUGE_MODE = new ConfigSetting(config, "System.Language Mode", "en_US",
            "The enabled language file.",
            "More language files (if available) can be found in the plugins data folder.");

    /**
     * In order to set dynamic economy comment correctly, this needs to be
     * called after EconomyManager load
     */
    public static void setupConfig() {
        config.load();
        config.setAutoremove(true).setAutosave(true);

        // convert glass pane settings
        int color;
        if ((color = GLASS_TYPE_1.getInt(-1)) != -1) {
            config.set(GLASS_TYPE_1.getKey(), CompatibleMaterial.getGlassPaneColor(color).name());
        }
        if ((color = GLASS_TYPE_2.getInt(-1)) != -1) {
            config.set(GLASS_TYPE_2.getKey(), CompatibleMaterial.getGlassPaneColor(color).name());
        }
        if ((color = GLASS_TYPE_3.getInt(-1)) != -1) {
            config.set(GLASS_TYPE_3.getKey(), CompatibleMaterial.getGlassPaneColor(color).name());
        }

        // convert economy settings
        if (config.getBoolean("Economy.Use Vault Economy") && EconomyManager.getManager().isEnabled("Vault")) {
            config.set("Main.Economy", "Vault");
        } else if (config.getBoolean("Economy.Use Reserve Economy") && EconomyManager.getManager().isEnabled("Reserve")) {
            config.set("Main.Economy", "Reserve");
        } else if (config.getBoolean("Economy.Use Player Points Economy") && EconomyManager.getManager().isEnabled("PlayerPoints")) {
            config.set("Main.Economy", "PlayerPoints");
        }

        config.saveChanges();
    }
}