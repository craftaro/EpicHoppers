package com.songoda.epichoppers.command.commands;

import com.songoda.epichoppers.EpicHoppers;
import com.songoda.epichoppers.command.AbstractCommand;
import com.songoda.epichoppers.hopper.levels.Level;
import com.songoda.epichoppers.utils.Methods;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CommandGive extends AbstractCommand {

    public CommandGive(AbstractCommand parent) {
        super(parent, false,"give");
    }

    @Override
    protected ReturnType runCommand(EpicHoppers instance, CommandSender sender, String... args) {
        if (args.length <= 2) {
            return ReturnType.SYNTAX_ERROR;
        }
        if (Bukkit.getPlayerExact(args[1]) == null) {
            sender.sendMessage(instance.references.getPrefix() + Methods.formatText("&cThat username does not exist, or the user is not online!"));
            return ReturnType.FAILURE;
        }

        Level level = instance.getLevelManager().getLowestLevel();
        Player player;
        if (Bukkit.getPlayer(args[1]) == null) {
            sender.sendMessage(instance.references.getPrefix() + Methods.formatText("&cThat player does not exist or is currently offline."));
            return ReturnType.FAILURE;
        } else {
            player = Bukkit.getPlayer(args[1]);
        }


        if (!instance.getLevelManager().isLevel(Integer.parseInt(args[2]))) {
            sender.sendMessage(instance.references.getPrefix() + Methods.formatText("&cNot a valid level... The current valid levels are: &4" + instance.getLevelManager().getLowestLevel().getLevel() + "-" + instance.getLevelManager().getHighestLevel().getLevel() + "&c."));
            return ReturnType.FAILURE;
        } else {
            level = instance.getLevelManager().getLevel(Integer.parseInt(args[2]));
        }
        player.getInventory().addItem(instance.newHopperItem(level));
        player.sendMessage(instance.references.getPrefix() + instance.getLocale().getMessage("command.give.success", level.getLevel()));

        return ReturnType.SUCCESS;
    }

    @Override
    protected List<String> onTab(EpicHoppers instance, CommandSender sender, String... args) {
        if (args.length == 2) {
            List<String> players = new ArrayList<>();
            for (Player player : Bukkit.getOnlinePlayers()) {
                players.add(player.getName());
            }
            return players;
        } else if (args.length == 3) {
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
