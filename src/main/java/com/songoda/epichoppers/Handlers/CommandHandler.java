package com.songoda.epichoppers.Handlers;

import com.songoda.arconix.plugin.Arconix;
import com.songoda.epichoppers.EpicHoppers;
import com.songoda.epichoppers.Utils.Debugger;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Created by songoda on 3/14/2017.
 */

public class CommandHandler implements CommandExecutor {

    private final EpicHoppers plugin;

    public CommandHandler(final EpicHoppers plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
        try {
            if (args.length == 0 || args[0].equalsIgnoreCase("help") || args[0].equalsIgnoreCase("?")) {
                sender.sendMessage("");
                sender.sendMessage(Arconix.pl().getApi().format().formatText(plugin.references.getPrefix() + "&7" + plugin.getDescription().getVersion() + " Created by &5&l&oBrianna"));
                sender.sendMessage(Arconix.pl().getApi().format().formatText(" &8- &aEH help &7Displays this page."));
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
                            ((Player) sender).getInventory().addItem(plugin.enchant.getbook());
                    } else if (Bukkit.getPlayerExact(args[1]) == null) {
                        sender.sendMessage(plugin.references.getPrefix() + Arconix.pl().getApi().format().formatText("&cThat username does not exist, or the user is not online!"));
                    } else {
                        Bukkit.getPlayerExact(args[1]).getInventory().addItem(plugin.enchant.getbook());
                    }
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
                        plugin.sm.openSettingsManager(p);
                    }
                }
            }
        } catch (Exception e) {
            Debugger.runReport(e);
        }
        return true;
    }
}