package com.songoda.epichoppers.command.commands;

import com.songoda.arconix.api.methods.formatting.TextComponent;
import com.songoda.epichoppers.EpicHoppersPlugin;
import com.songoda.epichoppers.command.AbstractCommand;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CommandBook extends AbstractCommand {

    public CommandBook(AbstractCommand parent) {
        super("book", "epichoppers.admin", parent);
    }

    @Override
    protected boolean runCommand(EpicHoppersPlugin instance, CommandSender sender, String... args) {
        if (args.length == 1) {
            if (sender instanceof Player)
                ((Player) sender).getInventory().addItem(instance.enchantmentHandler.getbook());
        } else if (Bukkit.getPlayerExact(args[1]) == null) {
            sender.sendMessage(instance.references.getPrefix() + TextComponent.formatText("&cThat username does not exist, or the user is not online!"));
        } else {
            Bukkit.getPlayerExact(args[1]).getInventory().addItem(instance.enchantmentHandler.getbook());
        }
        return false;
    }
}
