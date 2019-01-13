package com.songoda.epichoppers.command.commands;

import com.songoda.epichoppers.EpicHoppersPlugin;
import com.songoda.epichoppers.command.AbstractCommand;
import com.songoda.epichoppers.utils.Methods;
import org.bukkit.command.CommandSender;

public class CommandReload extends AbstractCommand {

    public CommandReload(AbstractCommand parent) {
        super("reload", parent, false);
    }

    @Override
    protected ReturnType runCommand(EpicHoppersPlugin instance, CommandSender sender, String... args) {
        instance.reload();
        sender.sendMessage(Methods.formatText(instance.references.getPrefix() + "&7Configuration and Language files reloaded."));
        return ReturnType.SUCCESS;
    }

    @Override
    public String getPermissionNode() {
        return "epichoppers.admin";
    }

    @Override
    public String getSyntax() {
        return "/eh reload";
    }

    @Override
    public String getDescription() {
        return "Reload the Configuration and Language files.";
    }
}
