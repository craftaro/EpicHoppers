package com.craftaro.epichoppers.compatibility;

import com.songoda.skyblock.core.compatibility.CompatibleMaterial;
import com.songoda.skyblock.permission.BasicPermission;
import com.songoda.skyblock.permission.PermissionType;

public class EpicHoppersPermission extends BasicPermission {
    public EpicHoppersPermission() {
        super("EpicHoppers", CompatibleMaterial.HOPPER, PermissionType.GENERIC);
    }
}
