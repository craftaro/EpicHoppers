package com.craftaro.epichoppers.utils;

import org.bukkit.Location;
import org.bukkit.block.BlockFace;

public enum HopperDirection {
    DOWN(0, 8, 0, -1, 0),
    NORTH(2, 10, 0, 0, -1),
    SOUTH(3, 11, 0, 0, 1),
    WEST(4, 12, -1, 0, 0),
    EAST(5, 13, 1, 0, 0);

    private final int unpowered;
    private final int powered;

    private final int x;
    private final int y;
    private final int z;

    HopperDirection(int unpowered, int powered, int x, int y, int z) {
        this.unpowered = unpowered;
        this.powered = powered;

        this.x = x;
        this.y = y;
        this.z = z;
    }

    public static HopperDirection getDirection(int value) {
        for (HopperDirection hopperDirection : HopperDirection.values()) {
            if (hopperDirection.getPowered() == value || hopperDirection.getUnpowered() == value) {
                return hopperDirection;
            }
        }
        return null;
    }

    public Location getLocation(Location location) {
        return location.clone().add(getX(), getY(), getZ());
    }

    public BlockFace getDirection() {
        switch (this) {
            case NORTH:
                return BlockFace.NORTH;
            case SOUTH:
                return BlockFace.SOUTH;
            case WEST:
                return BlockFace.WEST;
            case EAST:
                return BlockFace.EAST;
            default:
                return BlockFace.DOWN;
        }
    }

    public int getX() {
        return this.x;
    }

    public int getY() {
        return this.y;
    }

    public int getZ() {
        return this.z;
    }

    public int getUnpowered() {
        return this.unpowered;
    }

    public int getPowered() {
        return this.powered;
    }
}
