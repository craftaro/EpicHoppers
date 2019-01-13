package com.songoda.epichoppers.utils;

import com.songoda.epichoppers.EpicHoppersPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by songo on 6/4/2017.
 */
public class SettingsManager implements Listener {

    private String pluginName = "EpicHoppers";

    private static final Pattern SETTINGS_PATTERN = Pattern.compile("(.{1,28}(?:\\s|$))|(.{0,28})", Pattern.DOTALL);

    
    private Map<Player, String> cat = new HashMap<>();

    private final EpicHoppersPlugin instance;

    public SettingsManager(EpicHoppersPlugin plugin) {
        this.instance = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    private Map<Player, String> current = new HashMap<>();

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        ItemStack clickedItem = event.getCurrentItem();

        if (event.getInventory() != event.getWhoClicked().getOpenInventory().getTopInventory()
                || clickedItem == null || !clickedItem.hasItemMeta()
                || !clickedItem.getItemMeta().hasDisplayName()) {
            return;
        }

        if (event.getInventory().getTitle().equals(pluginName + " Settings Manager")) {
            event.setCancelled(true);
            if (clickedItem.getType().name().contains("STAINED_GLASS")) return;

            String type = ChatColor.stripColor(clickedItem.getItemMeta().getDisplayName());
            this.cat.put((Player) event.getWhoClicked(), type);
            this.openEditor((Player) event.getWhoClicked());
        } else if (event.getInventory().getTitle().equals(pluginName + " Settings Editor")) {
            event.setCancelled(true);
            if (clickedItem.getType().name().contains("STAINED_GLASS")) return;

            Player player = (Player) event.getWhoClicked();

            String key = cat.get(player) + "." + ChatColor.stripColor(clickedItem.getItemMeta().getDisplayName());

            if (instance.getConfig().get(key).getClass().getName().equals("java.lang.Boolean")) {
                this.instance.getConfig().set(key, !instance.getConfig().getBoolean(key));
                this.finishEditing(player);
            } else {
                this.editObject(player, key);
            }
        }
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        if (!current.containsKey(player)) return;

        String value = current.get(player);
        FileConfiguration config = instance.getConfig();
        if (config.isInt(value)) {
            config.set(value, Integer.parseInt(event.getMessage()));
        } else if (config.isDouble(value)) {
            config.set(value, Double.parseDouble(event.getMessage()));
        } else if (config.isString(value)) {
            config.set(value, event.getMessage());
        }

        this.finishEditing(player);
        event.setCancelled(true);
    }

    private void finishEditing(Player player) {
        this.current.remove(player);
        this.instance.saveConfig();
        this.openEditor(player);
    }

    private void editObject(Player player, String current) {
        this.current.put(player, ChatColor.stripColor(current));

        player.closeInventory();
        player.sendMessage("");
        player.sendMessage(Methods.formatText("&7Please enter a value for &6" + current + "&7."));
        if (instance.getConfig().isInt(current) || instance.getConfig().isDouble(current)) {
            player.sendMessage(Methods.formatText("&cUse only numbers."));
        }
        player.sendMessage("");
    }

    public void openSettingsManager(Player player) {
        Inventory inventory = Bukkit.createInventory(null, 27, pluginName + " Settings Manager");
        ItemStack glass = Methods.getGlass();
        for (int i = 0; i < inventory.getSize(); i++) {
            inventory.setItem(i, glass);
        }

        int slot = 10;
        for (String key : instance.getConfig().getDefaultSection().getKeys(false)) {
            ItemStack item = new ItemStack(Material.WHITE_WOOL, 1, (byte) (slot - 9)); //ToDo: Make this function as it was meant to.
            ItemMeta meta = item.getItemMeta();
            meta.setLore(Collections.singletonList(Methods.formatText("&6Click To Edit This Category.")));
            meta.setDisplayName(Methods.formatText("&f&l" + key));
            item.setItemMeta(meta);
            inventory.setItem(slot, item);
            slot++;
        }

        player.openInventory(inventory);
    }

    private void openEditor(Player player) {
        Inventory inventory = Bukkit.createInventory(null, 54, pluginName + " Settings Editor");
        FileConfiguration config = instance.getConfig();

        int slot = 0;
        for (String key : config.getConfigurationSection(cat.get(player)).getKeys(true)) {
            String fKey = cat.get(player) + "." + key;
            ItemStack item = new ItemStack(Material.DIAMOND_HELMET);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(Methods.formatText("&6" + key));

            List<String> lore = new ArrayList<>();
            if (config.isBoolean(fKey)) {
                item.setType(Material.LEVER);
                lore.add(Methods.formatText(config.getBoolean(fKey) ? "&atrue" : "&cfalse"));
            } else if (config.isString(fKey)) {
                item.setType(Material.PAPER);
                lore.add(Methods.formatText("&9" + config.getString(fKey)));
            } else if (config.isInt(fKey)) {
                item.setType(Material.CLOCK);
                lore.add(Methods.formatText("&5" + config.getInt(fKey)));
            }

            meta.setLore(lore);
            item.setItemMeta(meta);

            inventory.setItem(slot, item);
            slot++;
        }

        player.openInventory(inventory);
    }

    public void updateSettings() {
        FileConfiguration config = instance.getConfig();

        for (Setting setting : Setting.values()) {
            config.addDefault(setting.setting, setting.option);
        }
    }

    public enum Setting {
        HOPPER_UPGRADING("Main.Allow hopper Upgrading", true),
        UPGRADE_WITH_ECONOMY("Main.Upgrade With Economy", true),
        UPGRADE_WITH_XP("Main.Upgrade With XP", true),
        TELEPORT("Main.Allow Players To Teleport Through Hoppers", true),
        ENDERCHESTS("Main.Support Enderchests", true),
        PARTICLE_TYPE("Main.Upgrade Particle Type", "SPELL_WITCH"),
        HOP_TICKS("Main.Amount of Ticks Between Hops", 8L),
        AUTOSAVE("Main.Auto Save Interval In Seconds", 15),
        TELEPORT_TICKS("Main.Amount of Ticks Between Teleport", 10L),
        SYNC_TIMEOUT("Main.Timeout When Syncing Hoppers", 300L),
        MAX_CHUNK("Main.Max Hoppers Per Chunk", -1),
        TIPS("Main.Display Helpful Tips For Operators", true),
        SOUNDS("Main.Sounds Enabled", true),
        BLOCKBREAK_PARTICLE("Main.BlockBreak Particle Type", "LAVA"),
        BLACKLIST("Main.BlockBreak Blacklisted Blocks", Arrays.asList("BEDROCK")),
        RAINBOW("Interfaces.Replace Glass Type 1 With Rainbow Glass", false),
        ECO_ICON("Interfaces.Economy Icon", "SUNFLOWER"),
        XP_ICON("Interfaces.XP Icon", "EXPERIENCE_BOTTLE"),
        GLASS_TYPE_1("Interfaces.Glass Type 1", 7),
        GLASS_TYPE_2("Interfaces.Glass Type 2", 11),
        GLASS_TYPE_3("Interfaces.Glass Type 3", 3),

        DATABASE_SUPPORT("Database.Activate Mysql Support", false),
        DATABASE_IP("Database.IP", "127.0.0.1"),
        DATABASE_PORT("Database.Port", 3306),
        DATABASE_NAME("Database.Database Name", "EpicHoppers"),
        DATABASE_PREFIX("Database.Prefix", "EH-"),
        DATABASE_USERNAME("Database.Username", "PUT_USERNAME_HERE"),
        DATABASE_PASSWORD("Database.Password", "PUT_PASSWORD_HERE"),

        DOWNLOAD_FILES("System.Download Needed Data Files", true),
        LANGUGE_MODE("System.Language Mode", "en_US"),
        DEBUGGER("System.Debugger Enabled", false);

        private String setting;
        private Object option;

        Setting(String setting, Object option) {
            this.setting = setting;
            this.option = option;
        }

    }
}