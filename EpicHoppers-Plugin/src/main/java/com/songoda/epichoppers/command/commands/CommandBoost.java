package com.songoda.epichoppers.command.commands;

import com.songoda.epichoppers.EpicHoppersPlugin;
import com.songoda.epichoppers.boost.BoostData;
import com.songoda.epichoppers.command.AbstractCommand;
import com.songoda.epichoppers.utils.Methods;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.*;

public class CommandBoost extends AbstractCommand {

    public CommandBoost(AbstractCommand parent) {
        super(parent, false, "boost");
    }

    @Override
    protected ReturnType runCommand(EpicHoppersPlugin instance, CommandSender sender, String... args) {
        if (args.length < 3) {
            return ReturnType.SYNTAX_ERROR;
        }
        if (Bukkit.getPlayer(args[1]) == null) {
            sender.sendMessage(Methods.formatText(instance.references.getPrefix() + "&cThat player does not exist..."));
            return ReturnType.FAILURE;
        } else if (!Methods.isInt(args[2])) {
            sender.sendMessage(Methods.formatText(instance.references.getPrefix() + "&6" + args[2] + " &7is not a number..."));
            return ReturnType.FAILURE;
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
                    sender.sendMessage(Methods.formatText(instance.references.getPrefix() + "&7" + args[3] + " &7is invalid."));
                    return ReturnType.SUCCESS;
                }
            } else {
                c.add(Calendar.YEAR, 10);
            }

            BoostData boostData = new BoostData(Integer.parseInt(args[2]), c.getTime().getTime(), Bukkit.getPlayer(args[1]).getUniqueId());
            instance.getBoostManager().addBoostToPlayer(boostData);
            sender.sendMessage(Methods.formatText(instance.references.getPrefix() + "&7Successfully boosted &6" + Bukkit.getPlayer(args[1]).getName() + "'s &7hoppers transfer rates by &6" + args[2] + "x" + time));
        }
        return ReturnType.FAILURE;
    }

    @Override
    protected List<String> onTab(EpicHoppersPlugin instance, CommandSender sender, String... args) {
        if (args.length == 2) {
            List<String> players = new ArrayList<>();
            for (Player player : Bukkit.getOnlinePlayers()) {
                players.add(player.getName());
            }
            return players;
        } else if (args.length == 3) {
            return Arrays.asList("1", "2", "3", "4", "5");
        } else if (args.length == 4) {
            return Arrays.asList("m:", "h:", "d:", "y:");
        }
        return null;
    }

    @Override
    public String getPermissionNode() {
        return "epichoppers.admin";
    }

    @Override
    public String getSyntax() {
        return "/eh boost <player> <multiplier> [m:minute, h:hour, d:day, y:year]";
    }

    @Override
    public String getDescription() {
        return "This allows you to boost a players hoppers transfer speeds by a multiplier (Put 2 for double, 3 for triple and so on).";
    }
}
