package com.songoda.epichoppers.command.commands;

import com.songoda.epichoppers.EpicHoppers;
import com.songoda.epichoppers.command.AbstractCommand;
import com.songoda.epichoppers.utils.Methods;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class CommandBook extends AbstractCommand {

    public CommandBook(AbstractCommand parent) {
        super(parent, false, "book");
    }

    @Override
    protected ReturnType runCommand(EpicHoppers instance, CommandSender sender, String... args) {
        if (args.length == 1) {
            if (sender instanceof Player) {
                ((Player) sender).getInventory().addItem(instance.enchantmentHandler.getbook());
                return ReturnType.SUCCESS;
            }
        } else if (Bukkit.getPlayerExact(args[1]) == null) {
            instance.getLocale().newMessage("&cThat username does not exist, or the user is not online!")
                    .sendPrefixedMessage(sender);
            return ReturnType.FAILURE;
        } else {
            Bukkit.getPlayerExact(args[1]).getInventory().addItem(instance.enchantmentHandler.getbook());
            return ReturnType.SUCCESS;
        }
        return ReturnType.FAILURE;
    }

    @Override
    protected List<String> onTab(EpicHoppers instance, CommandSender sender, String... args) {
        return null;
    }

    @Override
    public String getPermissionNode() {
        return "epichoppers.admin";
    }

    @Override
    public String getSyntax() {
        return "/eh book [player]";
    }

    @Override
    public String getDescription() {
        return "Gives Sync Touch book to you or a player.";
    }
}
