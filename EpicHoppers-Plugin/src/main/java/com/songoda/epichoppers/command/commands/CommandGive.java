package com.songoda.epichoppers.command.commands;

import com.songoda.epichoppers.EpicHoppersPlugin;
import com.songoda.epichoppers.api.hopper.levels.Level;
import com.songoda.epichoppers.command.AbstractCommand;
import com.songoda.epichoppers.utils.Methods;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CommandGive extends AbstractCommand {

    public CommandGive(AbstractCommand parent) {
        super("give", parent, false);
    }

    @Override
    protected ReturnType runCommand(EpicHoppersPlugin instance, CommandSender sender, String... args) {
        if (args.length <= 2) {
            return ReturnType.SYNTAX_ERROR;
        }
        if (Bukkit.getPlayerExact(args[1]) == null) {
            sender.sendMessage(instance.references.getPrefix() + Methods.formatText("&cThat username does not exist, or the user is not online!"));
            return ReturnType.FAILURE;
        }

            Level level = instance.getLevelManager().getLowestLevel();
            Player player;
            if (args.length != 1 && Bukkit.getPlayer(args[1]) == null) {
                sender.sendMessage(instance.references.getPrefix() + Methods.formatText("&cThat player does not exist or is currently offline."));
                return ReturnType.FAILURE;
            } else if (args.length == 1) {
                if (!(sender instanceof Player)) {
                    sender.sendMessage(instance.references.getPrefix() + Methods.formatText("&cYou need to be a player to give a hopper to yourself."));
                    return ReturnType.FAILURE;
                }
                player = (Player) sender;
            } else {
                player = Bukkit.getPlayer(args[1]);
            }


            if (args.length >= 3 && !instance.getLevelManager().isLevel(Integer.parseInt(args[2]))) {
                sender.sendMessage(instance.references.getPrefix() + Methods.formatText("&cNot a valid level... The current valid levels are: &4" + instance.getLevelManager().getLowestLevel().getLevel() + "-" + instance.getLevelManager().getHighestLevel().getLevel() + "&c."));
                return ReturnType.FAILURE;
            } else if (args.length != 1) {
                level = instance.getLevelManager().getLevel(Integer.parseInt(args[2]));
            }
            player.getInventory().addItem(instance.newHopperItem(level));
            player.sendMessage(instance.references.getPrefix() + instance.getLocale().getMessage("command.give.success", level.getLevel()));

        return ReturnType.SUCCESS;
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
