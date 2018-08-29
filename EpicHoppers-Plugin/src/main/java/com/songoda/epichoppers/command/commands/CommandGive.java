package com.songoda.epichoppers.command.commands;

import com.songoda.arconix.api.methods.formatting.TextComponent;
import com.songoda.epichoppers.EpicHoppersPlugin;
import com.songoda.epichoppers.api.hopper.levels.Level;
import com.songoda.epichoppers.command.AbstractCommand;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CommandGive extends AbstractCommand {

    public CommandGive(AbstractCommand parent) {
        super("give", "epichoppers.admin", parent);
    }

    @Override
    protected boolean runCommand(EpicHoppersPlugin instance, CommandSender sender, String... args) {
        if (args.length >= 1) {

            Level level = instance.getLevelManager().getLowestLevel();
            Player player;
            if (args.length != 1 && Bukkit.getPlayer(args[1]) == null) {
                sender.sendMessage(instance.references.getPrefix() + TextComponent.formatText("&cThat player does not exist or is currently offline."));
                return true;
            } else if (args.length == 1) {
                if (!(sender instanceof Player)) {
                    sender.sendMessage(instance.references.getPrefix() + TextComponent.formatText("&cYou need to be a player to give a hopper to yourself."));
                    return true;
                }
                player = (Player) sender;
            } else {
                player = Bukkit.getPlayer(args[1]);
            }


            if (args.length >= 3 && !instance.getLevelManager().isLevel(Integer.parseInt(args[2]))) {
                sender.sendMessage(instance.references.getPrefix() + TextComponent.formatText("&cNot a valid level... The current valid levels are: &4" + instance.getLevelManager().getLowestLevel().getLevel() + "-" + instance.getLevelManager().getHighestLevel().getLevel() + "&c."));
                return true;
            } else if (args.length != 1) {
                level = instance.getLevelManager().getLevel(Integer.parseInt(args[2]));
            }
            player.getInventory().addItem(instance.newHopperItem(level));
            player.sendMessage(instance.references.getPrefix() + instance.getLocale().getMessage("command.give.success", level.getLevel()));

        } else if (Bukkit.getPlayerExact(args[1]) == null) {
            sender.sendMessage(instance.references.getPrefix() + TextComponent.formatText("&cThat username does not exist, or the user is not online!"));
        }
        return false;
    }
}
