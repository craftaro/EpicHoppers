package com.songoda.epichoppers.database.migrations;

import com.songoda.core.database.DataMigration;
import com.songoda.core.database.MySQLConnector;
import com.songoda.epichoppers.EpicHoppers;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class _1_InitialMigration extends DataMigration {

    public _1_InitialMigration() {
        super(1);
    }

    @Override
    public void migrate(Connection connection, String tablePrefix) throws SQLException {
        String autoIncrement = EpicHoppers.getInstance().getDatabaseConnector() instanceof MySQLConnector ? " AUTO_INCREMENT" : "";

        // Create hoppers table
        try (Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE " + tablePrefix + "placed_hoppers (" +
                    "id INTEGER PRIMARY KEY" + autoIncrement + ", " +
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
            statement.execute("CREATE TABLE " + tablePrefix + "links (" +
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
            statement.execute("CREATE TABLE " + tablePrefix + "items (" +
                    "hopper_id INTEGER NOT NULL, " +
                    "item_type BIT NOT NULL," +
                    "item TEXT NOT NULL " +
                    ")");
        }

        // Create player boosts
        try (Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE " + tablePrefix + "boosted_players (" +
                    "player VARCHAR(36) NOT NULL, " +
                    "multiplier INTEGER NOT NULL," +
                    "end_time BIGINT NOT NULL " +
                    ")");
        }
    }

}
