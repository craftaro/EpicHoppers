package com.songoda.epichoppers.command.commands;

import com.songoda.arconix.api.methods.formatting.TextComponent;
import com.songoda.epichoppers.EpicHoppersPlugin;
import com.songoda.epichoppers.command.AbstractCommand;
import org.bukkit.command.CommandSender;

public class CommandEpicHoppers extends AbstractCommand {

    public CommandEpicHoppers() {
        super("EpicHoppers", null, null);
    }

    @Override
    protected boolean runCommand(EpicHoppersPlugin instance, CommandSender sender, String... args) {
        sender.sendMessage("");
        sender.sendMessage(TextComponent.formatText("&f>>&m------------&6&l EpicHoppers Help &f&m------------&f<<"));
        sender.sendMessage(TextComponent.formatText("                   &7" + instance.getDescription().getVersion() + " Created by &5&l&oBrianna"));

        sender.sendMessage(TextComponent.formatText("&6/EpicHoppers&7 - Displays this page."));
        if (sender.hasPermission("epichoppers.admin")) {
            sender.sendMessage(TextComponent.formatText("&6/eh book [player]&7- Gives Sync Touch book to you or a player."));
            sender.sendMessage(TextComponent.formatText("&6/eh give [player] [level]&7 - Give a leveled hopper to a player."));
            sender.sendMessage(TextComponent.formatText("&6/eh settings&7 - Edit the EpicHoppers Settings."));
        }
        sender.sendMessage("");

        return false;
    }
}
