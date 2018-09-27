package com.songoda.epichoppers.utils;

import com.songoda.epichoppers.EpicHoppersPlugin;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.CompletableFuture;

/**
 * Represents a MySQL database source which directly connects to, queries and
 * executes statements towards the database found at the constructed location.
 * Operations performed on this object are done async (with the help of the
 * {@link CompletableFuture} API) and uses a connection pool as to not have to
 * constantly connect to and disconnect from the database
 */
public class MySQLDatabase {

    private final EpicHoppersPlugin instance;

    private Connection connection;

    /**
     * Construct a new instance of a MySQLDatabase given the specified database
     * credentials file. The file should be under the following format:
     * <p>
     * <code>
     * host:127.0.0.1<br>
     * user:database_username<br>
     * password:database_password
     * </code>
     *
     * @param instance            an instance of the plugin
     */
    public MySQLDatabase(EpicHoppersPlugin instance) {
        this.instance = instance;
        try {
            Class.forName("com.mysql.jdbc.Driver");

            String url = "jdbc:mysql://" + instance.getConfig().getString("Database.IP") + ":" + instance.getConfig().getString("Database.Port") + "/" + instance.getConfig().getString("Database.Database Name");
            this.connection = DriverManager.getConnection(url, instance.getConfig().getString("Database.Username"), instance.getConfig().getString("Database.Password"));

            //ToDo: This is sloppy
            connection.createStatement().execute(
                    "CREATE TABLE IF NOT EXISTS `sync` (\n" +
                    "\t`location` TEXT NULL,\n" +
                    "\t`level` INT NULL,\n" +
                    "\t`block` TEXT NULL,\n" +
                    "\t`placedby` TEXT NULL,\n" +
                    "\t`player` TEXT NULL,\n" +
                    "\t`teleporttrigger` TEXT NULL,\n" +
                    "\t`autocrafting` TEXT NULL,\n" +
                    "\t`whitelist` TEXT NULL,\n" +
                    "\t`blacklist` TEXT NULL,\n" +
                    "\t`void` TEXT NULL,\n" +
                    "\t`black` TEXT NULL\n" +
                    ")");

            connection.createStatement().execute("CREATE TABLE IF NOT EXISTS `boosts` (\n" +
                    "\t`endtime` TEXT NULL,\n" +
                    "\t`amount` INT NULL,\n" +
                    "\t`uuid` TEXT NULL\n" +
                    ")");

        } catch (ClassNotFoundException | SQLException e) {
            System.out.println("Database connection failed.");
        }
    }

    public Connection getConnection() {
        return connection;
    }
}