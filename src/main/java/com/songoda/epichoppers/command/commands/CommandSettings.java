package com.songoda.epichoppers.command.commands;

import com.songoda.epichoppers.EpicHoppers;
import com.songoda.epichoppers.command.AbstractCommand;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class CommandSettings extends AbstractCommand {

    public CommandSettings(AbstractCommand parent) {
        super(parent, true, "settings");
    }

    @Override
    protected ReturnType runCommand(EpicHoppers instance, CommandSender sender, String... args) {
        Player p = (Player) sender;
        instance.getSettingsManager().openSettingsManager(p);
        return ReturnType.SUCCESS;
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
        return "/eh settings";
    }

    @Override
    public String getDescription() {
        return "Edit the EpicHoppers Settings.";
    }
}
