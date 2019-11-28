package com.songoda.epichoppers.listeners;

import com.songoda.epichoppers.EpicHoppers;
import com.songoda.epichoppers.hopper.levels.modules.ModuleSuction;
import com.songoda.epichoppers.utils.Methods;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
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
        if (!meta.hasLore()) return;
        String str = meta.getLore().get(0).split("~")[0].replaceAll("§", "");
        if (!str.contains(":")) return;
        Location location = Methods.unserializeLocation(str);
        if (location.getBlock().getType() != Material.CHEST) return;
        InventoryHolder ih = (InventoryHolder) location.getBlock().getState();
        for (ItemStack is : event.getDrops()) {
            Map<Integer, ItemStack> notDropped = ih.getInventory().addItem(is);
            if (!notDropped.isEmpty())
                location.getWorld().dropItemNaturally(event.getEntity().getLocation(), new ArrayList<>(notDropped.values()).get(0));
        }
        event.getDrops().clear();
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerPickup(PlayerPickupItemEvent event) {
        if (ModuleSuction.isBlacklisted(event.getItem().getUniqueId()))
            event.setCancelled(true);
    }
}
