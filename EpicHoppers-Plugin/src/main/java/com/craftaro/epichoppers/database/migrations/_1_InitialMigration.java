package com.craftaro.epichoppers.database.migrations;

import com.craftaro.core.database.DataMigration;
import com.craftaro.core.database.DatabaseConnector;
import com.craftaro.core.database.MySQLConnector;
import com.craftaro.epichoppers.EpicHoppers;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class _1_InitialMigration extends DataMigration {
    private final EpicHoppers plugin;

    public _1_InitialMigration(EpicHoppers plugin) {
        super(1);
        this.plugin = plugin;
    }

    @Override
    public void migrate(Connection connection, String tablePrefix) throws SQLException {// Create hoppers table
        try (Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE IF NOT EXISTS " + tablePrefix + "placed_hoppers (" +
                    "id INTEGER PRIMARY KEY AUTO_INCREMENT" + ", " +
                    "level INTEGER NOT NULL, " +
                    "placed_by VARCHAR(36), " +
                    "last_opened_by VARCHAR(36), " +
                    "teleport_trigger VARCHAR(36), " +
                    "world TEXT NOT NULL, " +
                    "x DOUBLE NOT NULL, " +
                    "y DOUBLE NOT NULL, " +
                    "z DOUBLE NOT NULL " +
                    ")");
        }

        // Create hopper links
        try (Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE IF NOT EXISTS " + tablePrefix + "links (" +
                    "hopper_id INTEGER NOT NULL, " +
                    "link_type TEXT NOT NULL," +
                    "world TEXT NOT NULL, " +
                    "x DOUBLE NOT NULL, " +
                    "y DOUBLE NOT NULL, " +
                    "z DOUBLE NOT NULL " +
                    ")");
        }

        // Create items
        // Items are base64.
        try (Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE IF NOT EXISTS " + tablePrefix + "items (" +
                    "hopper_id INTEGER NOT NULL, " +
                    "item_type VARCHAR(20) NOT NULL," +
                    "item TEXT NOT NULL " +
                    ")");
        }

        // Create player boosts
        try (Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE IF NOT EXISTS " + tablePrefix + "boosted_players (" +
                    "player VARCHAR(36) NOT NULL, " +
                    "multiplier INTEGER NOT NULL," +
                    "end_time BIGINT NOT NULL " +
                    ")");
        }
    }

}
