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

    final EpicHoppers instance;

    public CommandGive(EpicHoppers instance) {
        super(false, "give");
        this.instance = instance;
    }

    @Override
    protected ReturnType runCommand(CommandSender sender, String... args) {
        if (args.length <= 1) {
            return ReturnType.SYNTAX_ERROR;
        }
        if (Bukkit.getPlayerExact(args[0]) == null) {
            instance.getLocale().newMessage("&cThat username does not exist, or the user is not online!").sendPrefixedMessage(sender);
            return ReturnType.FAILURE;
        }

        Level level = instance.getLevelManager().getLowestLevel();
        Player player;
        if (Bukkit.getPlayer(args[0]) == null) {
            instance.getLocale().newMessage("&cThat player does not exist or is currently offline.").sendPrefixedMessage(sender);
            return ReturnType.FAILURE;
        } else {
            player = Bukkit.getPlayer(args[0]);
        }


        if (!instance.getLevelManager().isLevel(Integer.parseInt(args[1]))) {
            instance.getLocale().newMessage("&cNot a valid level... The current valid levels are: &4" + level.getLevel() + "-" + instance.getLevelManager().getHighestLevel().getLevel() + "&c.")
                    .sendPrefixedMessage(sender);
            return ReturnType.FAILURE;
        } else {
            level = instance.getLevelManager().getLevel(Integer.parseInt(args[1]));
        }
        player.getInventory().addItem(instance.newHopperItem(level));
        instance.getLocale().getMessage("command.give.success").processPlaceholder("level", level.getLevel()).sendPrefixedMessage(player);

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
        return "/eh give [player] [level]";
    }

    @Override
    public String getDescription() {
        return "Give a leveled hopper to a player.";
    }
}
