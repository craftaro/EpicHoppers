package com.songoda.epichoppers.utils;

import com.songoda.epichoppers.EpicHoppers;
import org.bukkit.Bukkit;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MySQLDatabase {

    private final EpicHoppers instance;

    private Connection connection;

    public MySQLDatabase(EpicHoppers instance) {
        this.instance = instance;
        try {
            Class.forName("com.mysql.jdbc.Driver");

            String url = "jdbc:mysql://" + instance.getConfig().getString("Database.IP") + ":" + instance.getConfig().getString("Database.Port") + "/" + instance.getConfig().getString("Database.Database Name") + "?autoReconnect=true&useSSL=false";
            this.connection = DriverManager.getConnection(url, instance.getConfig().getString("Database.Username"), instance.getConfig().getString("Database.Password"));

            //ToDo: This is sloppy
            connection.createStatement().execute(
                    "CREATE TABLE IF NOT EXISTS `" + instance.getConfig().getString("Database.Prefix") + "sync` (\n" +
                            "\t`location` TEXT NULL,\n" +
                            "\t`level` INT NULL,\n" +
                            "\t`block` TEXT NULL,\n" +
                            "\t`placedby` TEXT NULL,\n" +
                            "\t`player` TEXT NULL,\n" +
                            "\t`teleporttrigger` TEXT NULL,\n" +
                            "\t`whitelist` TEXT NULL,\n" +
                            "\t`blacklist` TEXT NULL,\n" +
                            "\t`void` TEXT NULL,\n" +
                            "\t`black` TEXT NULL,\n" +
                            ")");

            connection.createStatement().execute("CREATE TABLE IF NOT EXISTS `" + instance.getConfig().getString("Database.Prefix") + "boosts` (\n" +
                    "\t`endtime` TEXT NULL,\n" +
                    "\t`amount` INT NULL,\n" +
                    "\t`uuid` TEXT NULL\n" +
                    ")");

        } catch (ClassNotFoundException | SQLException e) {
            Bukkit.getLogger().severe("Database connection failed.");
            Bukkit.getLogger().severe(e.getMessage());
        }
    }

    public Connection getConnection() {
        return connection;
    }
}