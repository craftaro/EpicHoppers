package com.songoda.epichoppers.command.commands;

import com.songoda.epichoppers.EpicHoppersPlugin;
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
    protected ReturnType runCommand(EpicHoppersPlugin instance, CommandSender sender, String... args) {
        if (args.length == 1) {
            if (sender instanceof Player) {
                ((Player) sender).getInventory().addItem(instance.enchantmentHandler.getbook());
                return ReturnType.SUCCESS;
            }
        } else if (Bukkit.getPlayerExact(args[1]) == null) {
            sender.sendMessage(instance.references.getPrefix() + Methods.formatText("&cThat username does not exist, or the user is not online!"));
            return ReturnType.FAILURE;
        } else {
            Bukkit.getPlayerExact(args[1]).getInventory().addItem(instance.enchantmentHandler.getbook());
            return ReturnType.SUCCESS;
        }
        return ReturnType.FAILURE;
    }

    @Override
    protected List<String> onTab(EpicHoppersPlugin instance, CommandSender sender, String... args) {
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
