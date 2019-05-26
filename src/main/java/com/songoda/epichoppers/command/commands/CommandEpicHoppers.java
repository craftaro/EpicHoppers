package com.songoda.epichoppers.command.commands;

import com.songoda.epichoppers.EpicHoppers;
import com.songoda.epichoppers.command.AbstractCommand;
import com.songoda.epichoppers.utils.Methods;
import org.bukkit.command.CommandSender;

import java.util.List;

public class CommandEpicHoppers extends AbstractCommand {

    public CommandEpicHoppers() {
        super(null, false, "EpicHoppers");
    }

    @Override
    protected ReturnType runCommand(EpicHoppers instance, CommandSender sender, String... args) {
        sender.sendMessage("");
        sender.sendMessage(Methods.formatText(instance.references.getPrefix() + "&7Version " + instance.getDescription().getVersion() + " Created with <3 by &5&l&oSongoda"));

        for (AbstractCommand command : instance.getCommandManager().getCommands()) {
            if (command.getPermissionNode() == null || sender.hasPermission(command.getPermissionNode())) {
                sender.sendMessage(Methods.formatText("&8 - &a" + command.getSyntax() + "&7 - " + command.getDescription()));
            }
        }
        sender.sendMessage("");

        return ReturnType.SUCCESS;
    }

    @Override
    protected List<String> onTab(EpicHoppers instance, CommandSender sender, String... args) {
        return null;
    }

    @Override
    public String getPermissionNode() {
        return null;
    }

    @Override
    public String getSyntax() {
        return "/EpicHoppers";
    }

    @Override
    public String getDescription() {
        return "Displays this page.";
    }
}
