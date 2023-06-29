package com.craftaro.epichoppers.hopper.teleport;

import com.craftaro.epichoppers.hopper.Hopper;
import org.bukkit.entity.Entity;

public interface TeleportHandler {
    void tpEntity(Entity entity, Hopper hopper);
}
