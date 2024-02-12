package com.craftaro.epichoppers.compatibility;

import com.craftaro.skyblock.permission.BasicPermission;
import com.craftaro.skyblock.permission.PermissionType;
import com.craftaro.third_party.com.cryptomorin.xseries.XMaterial;

public class EpicHoppersPermission extends BasicPermission {
    public EpicHoppersPermission() {
        super("EpicHoppers", XMaterial.HOPPER, PermissionType.GENERIC);
    }
}
