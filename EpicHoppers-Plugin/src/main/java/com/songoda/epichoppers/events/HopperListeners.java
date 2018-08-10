package com.songoda.epichoppers.events;

import com.songoda.arconix.plugin.Arconix;
import com.songoda.epichoppers.EpicHoppersPlugin;
import com.songoda.epichoppers.api.hopper.Hopper;
import com.songoda.epichoppers.hopper.EHopper;
import com.songoda.epichoppers.utils.Debugger;
import com.songoda.epichoppers.utils.Methods;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Created by songoda on 4/18/2017.
 */
public class HopperListeners implements Listener {

    private EpicHoppersPlugin instance;

    public HopperListeners(EpicHoppersPlugin instance) {
        this.instance = instance;
    }


    @EventHandler(ignoreCancelled = true)
    public void onHop(InventoryMoveItemEvent e) {
        try {
            Inventory source = e.getSource();


            if (!instance.getHopperManager().isHopper(e.getSource().getLocation())) return;

            Hopper hopper = instance.getHopperManager().getHopper(e.getSource().getLocation());

            if (source.getHolder() instanceof Hopper && hopper.getSyncedBlock() != null) {
                    e.setCancelled(true);
            }
        } catch (Exception ee) {
            Debugger.runReport(ee);
        }
    }

    private Map<UUID, Player> ents = new HashMap<>();

    @EventHandler
    public void onDed(EntityDamageByEntityEvent e) {
        try {
            if (e.getDamager() instanceof Player) {
                Player p = (Player) e.getDamager();
                if (Methods.isSync(p)) {
                    double d = ((LivingEntity) e.getEntity()).getHealth() - e.getDamage();
                    if (d < 1) {
                        ents.put(e.getEntity().getUniqueId(), p);
                    }
                }
            }
        } catch (Exception ee) {
            Debugger.runReport(ee);
        }
    }

    @EventHandler
    public void onDrop(EntityDeathEvent e) {
        try {
            if (ents.containsKey(e.getEntity().getUniqueId())) {
                Player p = ents.get(e.getEntity().getUniqueId());

                ItemStack item = p.getItemInHand();
                ItemMeta meta = item.getItemMeta();
                Location location = Arconix.pl().getApi().serialize().unserializeLocation(meta.getLore().get(1).replaceAll("§", ""));
                if (location.getBlock().getType() == Material.CHEST) {
                    InventoryHolder ih = (InventoryHolder) location.getBlock().getState();
                    for (ItemStack is : e.getDrops()) {
                        ih.getInventory().addItem(is);
                    }
                    e.getDrops().clear();
                }
            }
        } catch (Exception ee) {
            Debugger.runReport(ee);
        }
    }

    private int getItemCount(Inventory inventory, ItemStack item) {
        int amount = 0;

        for (ItemStack inventoryItem : inventory) {
            if (!item.isSimilar(inventoryItem)) continue;
            amount += inventoryItem.getAmount();
        }

        return amount;
    }
}
