package com.craftaro.epichoppers.utils;

import com.craftaro.core.database.DatabaseConnector;
import com.craftaro.third_party.org.jooq.Query;
import com.craftaro.third_party.org.jooq.impl.DSL;
import com.craftaro.core.utils.ItemSerializer;
import com.craftaro.epichoppers.EpicHoppers;
import com.craftaro.epichoppers.hopper.Hopper;
import com.craftaro.epichoppers.hopper.HopperImpl;
import com.craftaro.epichoppers.hopper.ItemType;
import com.craftaro.epichoppers.hopper.LinkType;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Base64;
import java.util.List;

public class DataHelper {

    public static void createLink(HopperImpl hopper, Location location, LinkType type) {
        EpicHoppers.getPlugin(EpicHoppers.class).getDataManager().getDatabaseConnector().connectDSL(dslContext -> {
            dslContext.insertInto(DSL.table(EpicHoppers.getPlugin(EpicHoppers.class).getDataManager().getTablePrefix() + "links"))
                    .columns(
                            DSL.field("hopper_id"),
                            DSL.field("link_type"),
                            DSL.field("world"),
                            DSL.field("x"),
                            DSL.field("y"),
                            DSL.field("z"))
                    .values(
                            hopper.getId(),
                            type.name(),
                            location.getWorld().getName(),
                            location.getBlockX(),
                            location.getBlockY(),
                            location.getBlockZ())
                    .execute();
        });
    }

    public static void updateItems(HopperImpl hopper, ItemType type, List<ItemStack> items) {
        DatabaseConnector databaseConnector = EpicHoppers.getPlugin(EpicHoppers.class).getDataManager().getDatabaseConnector();
        String tablePrefix = EpicHoppers.getPlugin(EpicHoppers.class).getDataManager().getTablePrefix();
        try (Connection connection = databaseConnector.getConnection()) {
            String clearItems = "DELETE FROM " + tablePrefix + "items WHERE hopper_id = ? AND item_type = ?";
            try (PreparedStatement statement = connection.prepareStatement(clearItems)) {
                statement.setInt(1, hopper.getId());
                statement.setString(2, type.name());
                statement.executeUpdate();
            }

            String createItem = "INSERT INTO " + tablePrefix + "items (hopper_id, item_type, item) VALUES (?, ?, ?)";
            try (PreparedStatement statement = connection.prepareStatement(createItem)) {
                for (ItemStack item : items) {
                    statement.setInt(1, hopper.getId());
                    statement.setString(2, type.name());

                    try (ByteArrayOutputStream stream = new ByteArrayOutputStream(); BukkitObjectOutputStream bukkitStream = new BukkitObjectOutputStream(stream)) {
                        bukkitStream.writeObject(item);
                        statement.setString(3, Base64.getEncoder().encodeToString(stream.toByteArray()));
                    } catch (IOException e) {
                        e.printStackTrace();
                        continue;
                    }
                    statement.addBatch();
                }
                statement.executeBatch();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        //Recreate with jooq
//        EpicHoppers.getPlugin(EpicHoppers.class).getDataManager().getDatabaseConnector().connectDSL(dslContext -> {
//            dslContext.deleteFrom(DSL.table(EpicHoppers.getPlugin(EpicHoppers.class).getDataManager().getTablePrefix() + "items"))
//                    .where(DSL.field("hopper_id").eq(hopper.getId()))
//                    .and(DSL.field("item_type").eq(type.name()))
//                    .execute();
//
//            dslContext.batch(
//                    items.stream().map(item -> dslContext.insertInto(DSL.table(EpicHoppers.getPlugin(EpicHoppers.class).getDataManager().getTablePrefix() + "items"))
//                            .columns(
//                                    DSL.field("hopper_id"),
//                                    DSL.field("item_type"),
//                                    DSL.field("item"))
//                            .values(
//                                    hopper.getId(),
//                                    type.name(),
//                                    Base64.getEncoder().encodeToString(ItemSerializer.serializeItem(item)))
//                    ).toArray(Query[]::new)
//            ).execute();
//        });
    }

    public static void deleteLinks(Hopper hopper) {
        EpicHoppers.getPlugin(EpicHoppers.class).getDataManager().getDatabaseConnector().connectDSL(dslContext -> {
            dslContext.deleteFrom(DSL.table(EpicHoppers.getPlugin(EpicHoppers.class).getDataManager().getTablePrefix() + "links"))
                    .where(DSL.field("hopper_id").eq(hopper.getId()))
                    .execute();
        });
    }
}
