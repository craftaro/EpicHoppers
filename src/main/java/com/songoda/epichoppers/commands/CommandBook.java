package com.songoda.epichoppers.commands;

import com.songoda.core.commands.AbstractCommand;
import com.songoda.epichoppers.EpicHoppers;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class CommandBook extends AbstractCommand {

    final EpicHoppers instance;

    public CommandBook(EpicHoppers instance) {
        super(false, "book");
        this.instance = instance;
    }

    @Override
    protected ReturnType runCommand(CommandSender sender, String... args) {
        if (args.length == 0) {
            if (sender instanceof Player) {
                ((Player) sender).getInventory().addItem(instance.enchantmentHandler.getbook());
                return ReturnType.SUCCESS;
            }
        } else if (Bukkit.getPlayerExact(args[0]) == null) {
            instance.getLocale().newMessage("&cThat username does not exist, or the user is not online!")
                    .sendPrefixedMessage(sender);
            return ReturnType.FAILURE;
        } else {
            Bukkit.getPlayerExact(args[0]).getInventory().addItem(instance.enchantmentHandler.getbook());
            return ReturnType.SUCCESS;
        }
        return ReturnType.FAILURE;
    }

    @Override
    protected List<String> onTab(CommandSender sender, String... args) {
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
