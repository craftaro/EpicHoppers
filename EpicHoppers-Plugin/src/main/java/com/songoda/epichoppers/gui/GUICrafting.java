package com.songoda.epichoppers.gui;

import com.songoda.epichoppers.EpicHoppersPlugin;
import com.songoda.epichoppers.api.hopper.Hopper;
import com.songoda.epichoppers.hopper.EHopper;
import com.songoda.epichoppers.utils.Methods;
import com.songoda.epichoppers.utils.gui.AbstractGUI;
import com.songoda.epichoppers.utils.gui.Range;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class GUICrafting extends AbstractGUI {

    private final EpicHoppersPlugin plugin;
    private final EHopper hopper;
    private final Player player;

    public GUICrafting(EpicHoppersPlugin plugin, EHopper hopper, Player player) {
        super(player);
        this.plugin = plugin;
        this.hopper = hopper;
        this.player = player;

        init(Methods.formatText(Methods.formatName(hopper.getLevel().getLevel(), false) + " &8-&f Crafting"), 27);
    }


    @Override
    protected void constructGUI() {
        int nu = 0;
        while (nu != 27) {
            inventory.setItem(nu, Methods.getGlass());
            nu++;
        }

        inventory.setItem(0, Methods.getBackgroundGlass(true));
        inventory.setItem(1, Methods.getBackgroundGlass(true));
        inventory.setItem(2, Methods.getBackgroundGlass(false));
        inventory.setItem(6, Methods.getBackgroundGlass(false));
        inventory.setItem(7, Methods.getBackgroundGlass(true));
        inventory.setItem(8, Methods.getBackgroundGlass(true));
        inventory.setItem(9, Methods.getBackgroundGlass(true));
        inventory.setItem(10, Methods.getBackgroundGlass(false));
        inventory.setItem(16, Methods.getBackgroundGlass(false));
        inventory.setItem(17, Methods.getBackgroundGlass(true));
        inventory.setItem(18, Methods.getBackgroundGlass(true));
        inventory.setItem(19, Methods.getBackgroundGlass(true));
        inventory.setItem(20, Methods.getBackgroundGlass(false));
        inventory.setItem(24, Methods.getBackgroundGlass(false));
        inventory.setItem(25, Methods.getBackgroundGlass(true));
        inventory.setItem(26, Methods.getBackgroundGlass(true));

        inventory.setItem(13, new ItemStack(hopper.getAutoCrafting() == null ? Material.AIR : hopper.getAutoCrafting()));

        addDraggable(new Range(13, 13, null, false), true);
    }

    @Override
    protected void registerClickables() {
    }

    @Override
    protected void registerOnCloses() {
        registerOnClose(((player, inventory) -> {
            Hopper hopper = plugin.getHopperManager().getHopperFromPlayer(player);
            ItemStack item = inventory.getItem(13);
            hopper.setAutoCrafting(item == null ? Material.AIR : item.getType());
        }));

    }
}
