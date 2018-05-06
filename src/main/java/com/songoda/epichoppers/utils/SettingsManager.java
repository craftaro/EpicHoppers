package com.songoda.epichoppers.utils;

import com.songoda.arconix.api.utils.ConfigWrapper;
import com.songoda.arconix.plugin.Arconix;
import com.songoda.epichoppers.EpicHoppers;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
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
    private final EpicHoppers instance;

    private static ConfigWrapper defs;

    private Map<Player, String> cat = new HashMap<>();

    public SettingsManager(EpicHoppers instance) {
        this.instance = instance;
        instance.saveResource("SettingDefinitions.yml", true);
        defs = new ConfigWrapper(instance, "", "SettingDefinitions.yml");
        defs.createNewFile("Loading data file", "EpicHoppers SettingDefinitions file");
        instance.getServer().getPluginManager().registerEvents(this, instance);
    }

    public Map<Player, String> current = new HashMap<>();

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (e.getInventory() == null
                || e.getCurrentItem() == null
                || !e.getCurrentItem().hasItemMeta()
                || !e.getCurrentItem().getItemMeta().hasDisplayName()
                || e.getWhoClicked().getOpenInventory().getTopInventory() != e.getInventory()) {
            return;
        }
        if (e.getInventory().getTitle().equals(pluginName + " Settings Manager")) {

            if (e.getCurrentItem().getType().equals(Material.STAINED_GLASS_PANE)) {
                e.setCancelled(true);
                return;
            }

            String type = ChatColor.stripColor(e.getCurrentItem().getItemMeta().getDisplayName());
            cat.put((Player) e.getWhoClicked(), type);
            openEditor((Player) e.getWhoClicked());
            e.setCancelled(true);
        } else if (e.getInventory().getTitle().equals(pluginName + " Settings Editor")) {

            if (e.getCurrentItem().getType().equals(Material.STAINED_GLASS_PANE)) {
                e.setCancelled(true);
                return;
            }

            Player p = (Player) e.getWhoClicked();
            e.setCancelled(true);

            String key = cat.get(p) + "." + ChatColor.stripColor(e.getCurrentItem().getItemMeta().getDisplayName());

            if (instance.getConfig().get(key).getClass().getName().equals("java.lang.Boolean")) {
                boolean bool = (Boolean) instance.getConfig().get(key);
                if (!bool)
                    instance.getConfig().set(key, true);
                else
                    instance.getConfig().set(key, false);
                finishEditing(p);
            } else {
                editObject(p, key);
            }
        }
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent e) {
        final Player p = e.getPlayer();
        if (!current.containsKey(p)) {
            return;
        }
        switch (instance.getConfig().get(current.get(p)).getClass().getName()) {
            case "java.lang.Integer":
                instance.getConfig().set(current.get(p), Integer.parseInt(e.getMessage()));
                break;
            case "java.lang.Double":
                instance.getConfig().set(current.get(p), Double.parseDouble(e.getMessage()));
                break;
            case "java.lang.String":
                instance.getConfig().set(current.get(p), e.getMessage());
                break;
        }
        finishEditing(p);
        e.setCancelled(true);

    }

    public void finishEditing(Player p) {
        current.remove(p);
        instance.saveConfig();
        openEditor(p);
    }


    public void editObject(Player p, String current) {
        this.current.put(p, ChatColor.stripColor(current));
        p.closeInventory();
        p.sendMessage("");
        p.sendMessage(Arconix.pl().getApi().format().formatText("&7Please enter a value for &6" + current + "&7."));
        if (instance.getConfig().get(current).getClass().getName().equals("java.lang.Integer")) {
            p.sendMessage(Arconix.pl().getApi().format().formatText("&cUse only numbers."));
        }
        p.sendMessage("");
    }

    public void openSettingsManager(Player p) {
        Inventory i = Bukkit.createInventory(null, 27, pluginName + " Settings Manager");
        int nu = 0;
        while (nu != 27) {
            i.setItem(nu, Methods.getGlass());
            nu++;
        }

        int spot = 10;
        for (String key : instance.getConfig().getConfigurationSection("").getKeys(false)) {
            ItemStack item = new ItemStack(Material.WOOL, 1, (byte) (spot - 9));
            ItemMeta meta = item.getItemMeta();
            meta.setLore(Collections.singletonList(Arconix.pl().getApi().format().formatText("&6Click To Edit This Category.")));
            meta.setDisplayName(Arconix.pl().getApi().format().formatText("&f&l" + key));
            item.setItemMeta(meta);
            i.setItem(spot, item);
            spot++;
        }
        p.openInventory(i);
    }

    public void openEditor(Player p) {
        Inventory i = Bukkit.createInventory(null, 54, pluginName + " Settings Editor");

        int num = 0;
        for (String key : instance.getConfig().getConfigurationSection(cat.get(p)).getKeys(true)) {
            String fKey = cat.get(p) + "." + key;
            ItemStack item = new ItemStack(Material.DIAMOND_HELMET);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(Arconix.pl().getApi().format().formatText("&6" + key));
            ArrayList<String> lore = new ArrayList<>();
            switch (instance.getConfig().get(fKey).getClass().getName()) {
                case "java.lang.Boolean":

                    item.setType(Material.LEVER);
                    boolean bool = (Boolean) instance.getConfig().get(fKey);

                    if (!bool)
                        lore.add(Arconix.pl().getApi().format().formatText("&c" + false));
                    else
                        lore.add(Arconix.pl().getApi().format().formatText("&a" + true));

                    break;
                case "java.lang.String":
                    item.setType(Material.PAPER);
                    String str = (String) instance.getConfig().get(fKey);
                    lore.add(Arconix.pl().getApi().format().formatText("&9" + str));
                    break;
                case "java.lang.Integer":
                    item.setType(Material.WATCH);

                    int in = (Integer) instance.getConfig().get(fKey);
                    lore.add(Arconix.pl().getApi().format().formatText("&5" + in));
                    break;
                default:
                    continue;
            }
            if (defs.getConfig().contains(fKey)) {
                String text = defs.getConfig().getString(key);

                Pattern regex = Pattern.compile("(.{1,28}(?:\\s|$))|(.{0,28})", Pattern.DOTALL);
                Matcher m = regex.matcher(text);
                while (m.find()) {
                    if (m.end() != text.length() || m.group().length() != 0)
                        lore.add(Arconix.pl().getApi().format().formatText("&7" + m.group()));
                }
            }
            meta.setLore(lore);
            item.setItemMeta(meta);

            i.setItem(num, item);
            num++;
        }
        p.openInventory(i);
    }

    public void updateSettings() {
        for (settings s : settings.values()) {
            if (instance.getConfig().contains("settings." + s.oldSetting)) {
                instance.getConfig().addDefault(s.setting, instance.getConfig().get("settings." + s.oldSetting));
                instance.getConfig().set("settings." + s.oldSetting, null);
            } else if (s.setting.equals("Main.Upgrade Particle Type")) {
                if (instance.v1_7 || instance.v1_8)
                    instance.getConfig().addDefault(s.setting, "WITCH_MAGIC");
                else
                    instance.getConfig().addDefault(s.setting, s.option);
            } else
                instance.getConfig().addDefault(s.setting, s.option);
        }
    }

    public enum settings {
        o1("Upgrading-enabled", "Main.Allow Hopper Upgrading", true),
        o2("Upgrade-with-eco", "Main.Upgrade With Economy", true),
        o3("Upgrade-with-xp", "Main.Upgrade With XP", true),
        o4("Teleport-hoppers", "Main.Allow Players To Teleport Through Hoppers", true),
        o5("Filter-hoppers", "Main.Allow Players To use The Hopper Filter", true),
        o6("EnderChest-support", "Main.Support Enderchests", true),
        o7("Upgrade-particle-type", "Main.Upgrade Particle Type", "SPELL_WITCH"),
        o8("Hop-Tick", "Main.Amount of Ticks Between Hops", 8L),
        o9("Tele-Tick", "Main.Amount of Ticks Between Teleport", 10L),
        o10("Sync-Timeout", "Main.Timeout When Syncing Hoppers", 300L),
        o11("hopper-Limit", "Main.Max Hoppers Per Chunk", -1),
        o12("Helpful-Tips", "Main.Display Helpful Tips For Operators", true),
        o13("Sounds", "Main.Sounds Enabled", true),
        o14("BlockBreak-Particle-Type", "Main.BlockBreak Particle Type","LAVA"),
        o15("BlockBreak-Blacklist", "Main.BlockBreak Blacklisted Blocks", Arrays.asList("BEDROCK")),

        o16("Rainbow-Glass", "Interfaces.Replace Glass Type 1 With Rainbow Glass", false),
        o17("ECO-Icon", "Interfaces.Economy Icon", "DOUBLE_PLANT"),
        o18("XP-Icon", "Interfaces.XP Icon", "EXP_BOTTLE"),
        o19("Glass-Type-1", "Interfaces.Glass Type 1", 7),
        o20("Glass-Type-2", "Interfaces.Glass Type 2", 11),
        o21("Glass-Type-3", "Interfaces.Glass Type 3", 3),

        o22("Debug-Mode", "System.Debugger Enabled", false);

        private String setting;
        private String oldSetting;
        private Object option;

        settings(String oldSetting, String setting, Object option) {
            this.oldSetting = oldSetting;
            this.setting = setting;
            this.option = option;
        }

    }
}