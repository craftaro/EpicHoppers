package com.songoda.epichoppers.command.commands;

import com.songoda.arconix.api.methods.formatting.TextComponent;
import com.songoda.epichoppers.EpicHoppersPlugin;
import com.songoda.epichoppers.command.AbstractCommand;
import org.bukkit.command.CommandSender;

public class CommandReload extends AbstractCommand {

    public CommandReload(AbstractCommand parent) {
        super("reload", "epichoppers.admin", parent);
    }

    @Override
    protected boolean runCommand(EpicHoppersPlugin instance, CommandSender sender, String... args) {
        instance.reload();
        sender.sendMessage(TextComponent.formatText(instance.references.getPrefix() + "&7Configuration and Language files reloaded."));
        return false;
    }
}
