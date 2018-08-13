package com.songoda.epichoppers.handlers;

import com.songoda.arconix.plugin.Arconix;
import com.songoda.epichoppers.EpicHoppersPlugin;
import com.songoda.epichoppers.api.hopper.Level;
import com.songoda.epichoppers.utils.Debugger;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Created by songoda on 3/14/2017.
 */

public class CommandHandler implements CommandExecutor {

    private final EpicHoppersPlugin plugin;

    public CommandHandler(final EpicHoppersPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
        try {
            if (args.length == 0 || args[0].equalsIgnoreCase("help") || args[0].equalsIgnoreCase("?")) {
                sender.sendMessage("");
                sender.sendMessage(Arconix.pl().getApi().format().formatText(plugin.references.getPrefix() + "&7" + plugin.getDescription().getVersion() + " Created by &5&l&oBrianna"));
                
                sender.sendMessage(Arconix.pl().getApi().format().formatText(" &8- &aEH help &7Displays this page."));
                sender.sendMessage(Arconix.pl().getApi().format().formatText(" &8- &aEH give [player] [level] &7Give a leveled hopper to a player."));
                sender.sendMessage(Arconix.pl().getApi().format().formatText(" &8- &aEH book [player] &7Gives Sync Touch book to you or a player."));
                sender.sendMessage(Arconix.pl().getApi().format().formatText(" &8- &aEH settings &7Edit the EpicHoppers Settings."));
                sender.sendMessage(Arconix.pl().getApi().format().formatText(" &8- &aEH reload &7Reloads Configuration and Language files."));
                sender.sendMessage("");
            } else if (args[0].equalsIgnoreCase("book")) {
                if (!sender.hasPermission("synccraft.admin") && !sender.hasPermission("epichoppers.admin")) {
                    sender.sendMessage(plugin.references.getPrefix() + plugin.getLocale().getMessage("event.general.nopermission"));
                } else {
                    if (args.length == 1) {
                        if (sender instanceof Player)
                            ((Player) sender).getInventory().addItem(plugin.enchantmentHandler.getbook());
                    } else if (Bukkit.getPlayerExact(args[1]) == null) {
                        sender.sendMessage(plugin.references.getPrefix() + Arconix.pl().getApi().format().formatText("&cThat username does not exist, or the user is not online!"));
                    } else {
                        Bukkit.getPlayerExact(args[1]).getInventory().addItem(plugin.enchantmentHandler.getbook());
                    }
                }

            } else if (args[0].equalsIgnoreCase("give")) {
                if (!sender.hasPermission("synccraft.admin") && !sender.hasPermission("epichoppers.admin")) {
                    sender.sendMessage(plugin.references.getPrefix() + plugin.getLocale().getMessage("event.general.nopermission"));
                    return true;
                }
                if (args.length >= 1) {

                    Level level = plugin.getLevelManager().getLowestLevel();
                    Player player;
                    if (args.length != 1 && Bukkit.getPlayer(args[1]) == null) {
                        sender.sendMessage(plugin.references.getPrefix() + Arconix.pl().getApi().format().formatText("&cThat player does not exist or is currently offline."));
                        return true;
                    } else if (args.length == 1) {
                        if (!(sender instanceof Player)) {
                            sender.sendMessage(plugin.references.getPrefix() + Arconix.pl().getApi().format().formatText("&cYou need to be a player to give a hopper to yourself."));
                            return true;
                        }
                        player = (Player)sender;
                    } else {
                        player = Bukkit.getPlayer(args[1]);
                    }


                    if (args.length >= 3 && !plugin.getLevelManager().isLevel(Integer.parseInt(args[2]))) {
                        sender.sendMessage(plugin.references.getPrefix() + Arconix.pl().getApi().format().formatText("&cNot a valid level... The current valid levels are: &4" + plugin.getLevelManager().getLowestLevel().getLevel() + "-" + plugin.getLevelManager().getHighestLevel().getLevel() + "&c."));
                        return true;
                    } else if (args.length != 1){
                        level = plugin.getLevelManager().getLevel(Integer.parseInt(args[2]));
                    }
                    player.getInventory().addItem(plugin.newHopperItem(level));
                    player.sendMessage(plugin.references.getPrefix() + plugin.getLocale().getMessage("command.give.success", level.getLevel()));

                } else if (Bukkit.getPlayerExact(args[1]) == null) {
                    sender.sendMessage(plugin.references.getPrefix() + Arconix.pl().getApi().format().formatText("&cThat username does not exist, or the user is not online!"));
                }
            } else if (args[0].equalsIgnoreCase("reload")) {
                if (!sender.hasPermission("synccraft.admin") && !sender.hasPermission("epichoppers.admin")) {
                    sender.sendMessage(plugin.references.getPrefix() + plugin.getLocale().getMessage("event.general.nopermission"));
                } else {
                    plugin.reload();
                    sender.sendMessage(Arconix.pl().getApi().format().formatText(plugin.references.getPrefix() + "&8Configuration and Language files reloaded."));
                }
            } else if (sender instanceof Player) {
                if (args[0].equalsIgnoreCase("settings")) {
                    if (!sender.hasPermission("synccraft.admin") && !sender.hasPermission("epichoppers.admin")) {
                        sender.sendMessage(plugin.references.getPrefix() + plugin.getLocale().getMessage("event.general.nopermission"));
                    } else {
                        Player p = (Player) sender;
                        plugin.settingsManager.openSettingsManager(p);
                    }
                }
            }
        } catch (Exception e) {
            Debugger.runReport(e);
        }
        return true;
    }
}