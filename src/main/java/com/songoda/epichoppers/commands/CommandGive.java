package com.songoda.epichoppers.commands;

import com.songoda.core.commands.AbstractCommand;
import com.songoda.epichoppers.EpicHoppers;
import com.songoda.epichoppers.hopper.levels.Level;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CommandGive extends AbstractCommand {

    private final EpicHoppers plugin;

    public CommandGive(EpicHoppers plugin) {
        super(CommandType.CONSOLE_OK, "give");
        this.plugin = plugin;
    }

    @Override
    protected ReturnType runCommand(CommandSender sender, String... args) {
        if (args.length <= 1) {
            return ReturnType.SYNTAX_ERROR;
        }
        if (Bukkit.getPlayerExact(args[0]) == null) {
            plugin.getLocale().newMessage("&cThat username does not exist, or the user is not online!").sendPrefixedMessage(sender);
            return ReturnType.FAILURE;
        }

        Level level = plugin.getLevelManager().getLowestLevel();
        Player player;
        if (Bukkit.getPlayer(args[0]) == null) {
            plugin.getLocale().newMessage("&cThat player does not exist or is currently offline.").sendPrefixedMessage(sender);
            return ReturnType.FAILURE;
        } else {
            player = Bukkit.getPlayer(args[0]);
        }


        if (!plugin.getLevelManager().isLevel(Integer.parseInt(args[1]))) {
            plugin.getLocale().newMessage("&cNot a valid level... The current valid levels are: &4" + level.getLevel() + "-" + plugin.getLevelManager().getHighestLevel().getLevel() + "&c.")
                    .sendPrefixedMessage(sender);
            return ReturnType.FAILURE;
        } else {
            level = plugin.getLevelManager().getLevel(Integer.parseInt(args[1]));
        }
        player.getInventory().addItem(plugin.newHopperItem(level));
        plugin.getLocale().getMessage("command.give.success").processPlaceholder("level", level.getLevel()).sendPrefixedMessage(player);

        return ReturnType.SUCCESS;
    }

    @Override
    protected List<String> onTab(CommandSender sender, String... args) {
        if (args.length == 1) {
            List<String> players = new ArrayList<>();
            for (Player player : Bukkit.getOnlinePlayers()) {
                players.add(player.getName());
            }
            return players;
        } else if (args.length == 2) {
            return Arrays.asList("1", "2", "3", "4", "5");
        }
        return null;
    }

    @Override
    public String getPermissionNode() {
        return "epichoppers.admin";
    }

    @Override
    public String getSyntax() {
        return "give [player] [level]";
    }

    @Override
    public String getDescription() {
        return "Give a leveled hopper to a player.";
    }
}
