package com.songoda.epichoppers.command.commands;

import com.songoda.arconix.api.methods.formatting.TextComponent;
import com.songoda.arconix.api.methods.math.AMath;
import com.songoda.epichoppers.EpicHoppersPlugin;
import com.songoda.epichoppers.boost.BoostData;
import com.songoda.epichoppers.command.AbstractCommand;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

import java.util.Calendar;
import java.util.Date;

public class CommandBoost extends AbstractCommand {

    public CommandBoost(AbstractCommand parent) {
        super("boost", "epichoppers.admin", parent);
    }

    @Override
    protected boolean runCommand(EpicHoppersPlugin instance, CommandSender sender, String... args) {
        if (args.length >= 3) {
            if (Bukkit.getPlayer(args[1]) == null) {
                sender.sendMessage(TextComponent.formatText(instance.references.getPrefix() + "&cThat player does not exist..."));
            } else if (!AMath.isInt(args[2])) {
                sender.sendMessage(TextComponent.formatText(instance.references.getPrefix() + "&6" + args[2] + " &7is not a number..."));
            } else {
                Calendar c = Calendar.getInstance();
                Date currentDate = new Date();
                c.setTime(currentDate);

                String time = "&7.";

                if (args.length > 3) {
                    if (args[3].contains("m:")) {
                        String[] arr2 = (args[3]).split(":");
                        c.add(Calendar.MINUTE, Integer.parseInt(arr2[1]));
                        time = " &7for &6" + arr2[1] + " minutes&7.";
                    } else if (args[3].contains("h:")) {
                        String[] arr2 = (args[3]).split(":");
                        c.add(Calendar.HOUR, Integer.parseInt(arr2[1]));
                        time = " &7for &6" + arr2[1] + " hours&7.";
                    } else if (args[3].contains("d:")) {
                        String[] arr2 = (args[3]).split(":");
                        c.add(Calendar.HOUR, Integer.parseInt(arr2[1]) * 24);
                        time = " &7for &6" + arr2[1] + " days&7.";
                    } else if (args[3].contains("y:")) {
                        String[] arr2 = (args[3]).split(":");
                        c.add(Calendar.YEAR, Integer.parseInt(arr2[1]));
                        time = " &7for &6" + arr2[1] + " years&7.";
                    } else {
                        sender.sendMessage(TextComponent.formatText(instance.references.getPrefix() + "&7" + args[3] + " &7is invalid."));
                        return true;
                    }
                } else {
                    c.add(Calendar.YEAR, 10);
                }

                BoostData boostData = new BoostData(Integer.parseInt(args[2]), c.getTime().getTime(), Bukkit.getPlayer(args[1]).getUniqueId());
                instance.getBoostManager().addBoostToPlayer(boostData);
                sender.sendMessage(TextComponent.formatText(instance.references.getPrefix() + "&7Successfully boosted &6" + Bukkit.getPlayer(args[1]).getName() + "'s &7hoppers transfer rates by &6" + args[2] + "x" + time));
            }
        } else {
            sender.sendMessage(instance.references.getPrefix() + TextComponent.formatText("&7Syntax error..."));
        }
        return false;
    }
}
