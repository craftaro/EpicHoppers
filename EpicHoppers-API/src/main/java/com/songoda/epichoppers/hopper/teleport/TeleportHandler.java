package com.songoda.epichoppers.hopper.teleport;

import com.songoda.epichoppers.hopper.Hopper;
import org.bukkit.entity.Entity;

public interface TeleportHandler {
    void tpEntity(Entity entity, Hopper hopper);
}
