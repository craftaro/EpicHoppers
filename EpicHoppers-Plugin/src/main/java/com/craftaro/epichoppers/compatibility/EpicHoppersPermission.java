package com.craftaro.epichoppers.compatibility;

import com.craftaro.skyblock.core.third_party.com.cryptomorin.xseries.XMaterial;
import com.craftaro.skyblock.permission.BasicPermission;
import com.craftaro.skyblock.permission.PermissionType;

public class EpicHoppersPermission extends BasicPermission {
    public EpicHoppersPermission() {
        super("EpicHoppers", XMaterial.HOPPER, PermissionType.GENERIC);
    }
}
