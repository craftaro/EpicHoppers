package com.songoda.epichoppers.listeners;

import com.songoda.epichoppers.EpicHoppersPlugin;
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
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class EntityListeners implements Listener {

    private final EpicHoppersPlugin instance;
    private Map<UUID, Player> ents = new HashMap<>();

    public EntityListeners(EpicHoppersPlugin instance) {
        this.instance = instance;
    }

    @EventHandler
    public void onDed(EntityDamageByEntityEvent e) {
        try {
            if (!(e.getDamager() instanceof Player)) return;
            Player p = (Player) e.getDamager();
            if (!Methods.isSync(p)) return;
            double d = ((LivingEntity) e.getEntity()).getHealth() - e.getDamage();
            if (d < 1) {
                ents.put(e.getEntity().getUniqueId(), p);

            }
        } catch (Exception ee) {
            Debugger.runReport(ee);
        }
    }

    @EventHandler
    public void onDrop(EntityDeathEvent e) {
        try {
            if (!ents.containsKey(e.getEntity().getUniqueId())) return;
            Player p = ents.get(e.getEntity().getUniqueId());

            ItemStack item = p.getItemInHand();
            ItemMeta meta = item.getItemMeta();
            Location location = Methods.unserializeLocation(meta.getLore().get(1).replaceAll("§", ""));
            if (location.getBlock().getType() != Material.CHEST) return;
            InventoryHolder ih = (InventoryHolder) location.getBlock().getState();
            for (ItemStack is : e.getDrops()) {
                ih.getInventory().addItem(is);
            }
            e.getDrops().clear();
        } catch (Exception ee) {
            Debugger.runReport(ee);
        }
    }
}
