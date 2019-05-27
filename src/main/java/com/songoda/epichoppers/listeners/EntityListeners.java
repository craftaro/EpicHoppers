package com.songoda.epichoppers.listeners;

import com.songoda.epichoppers.EpicHoppers;
import com.songoda.epichoppers.hopper.levels.modules.ModuleSuction;
import com.songoda.epichoppers.utils.Methods;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class EntityListeners implements Listener {

    private final EpicHoppers instance;
    private Map<UUID, Player> ents = new HashMap<>();

    public EntityListeners(EpicHoppers instance) {
        this.instance = instance;
    }

    @EventHandler
    public void onDed(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) return;
        Player p = (Player) event.getDamager();
        if (!Methods.isSync(p)) return;
        double d = ((LivingEntity) event.getEntity()).getHealth() - event.getDamage();
        if (d < 1) {
            ents.put(event.getEntity().getUniqueId(), p);
        }
    }

    @EventHandler
    public void onDrop(EntityDeathEvent event) {
        if (!ents.containsKey(event.getEntity().getUniqueId())) return;
        Player p = ents.get(event.getEntity().getUniqueId());

        ItemStack item = p.getItemInHand();
        ItemMeta meta = item.getItemMeta();
        Location location = Methods.unserializeLocation(meta.getLore().get(1).replaceAll("§", ""));
        if (location.getBlock().getType() != Material.CHEST) return;
        InventoryHolder ih = (InventoryHolder) location.getBlock().getState();
        for (ItemStack is : event.getDrops()) {
            ih.getInventory().addItem(is);
        }
        event.getDrops().clear();
    }

    @EventHandler
    public void onPlayerPickup(PlayerPickupItemEvent event) {
        if (ModuleSuction.isBlacklisted(event.getItem().getUniqueId()))
            event.setCancelled(true);
        ModuleSuction.addToBlacklist(event.getItem().getUniqueId());
    }
}
