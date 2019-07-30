package com.songoda.epichoppers.hopper.levels.modules;

import com.bgsoftware.wildstacker.api.WildStackerAPI;
import com.songoda.epichoppers.EpicHoppers;
import com.songoda.epichoppers.hopper.Hopper;
import com.songoda.epichoppers.tasks.HopTask;
import com.songoda.epichoppers.utils.ServerVersion;
import com.songoda.ultimatestacker.utils.Methods;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.stream.Collectors;

public class ModuleSuction extends Module {

    private final int amount;

    public static List<UUID> blacklist = new ArrayList<>();

    private static boolean wildStacker = Bukkit.getPluginManager().isPluginEnabled("WildStacker");
    private static boolean ultimateStacker = Bukkit.getPluginManager().isPluginEnabled("UltimateStacker");

    public ModuleSuction(EpicHoppers plugin, int amount) {
        super(plugin);
        this.amount = amount;
    }

    @Override
    public String getName() {
        return "Suction";
    }

    @Override
    public void run(Hopper hopper, Inventory hopperInventory) {
        double radius = amount + .5;

        Set<Item> itemsToSuck = hopper.getLocation().getWorld().getNearbyEntities(hopper.getLocation().add(0.5, 0.5, 0.5), radius, radius, radius)
                .stream()
                .filter(entity -> entity.getType() == EntityType.DROPPED_ITEM
                        && entity.getTicksLived() >= ((Item) entity).getPickupDelay()
                        && entity.getLocation().getBlock().getType() != Material.HOPPER)
                .map(entity -> (Item) entity)
                .collect(Collectors.toSet());

        for (Item item : itemsToSuck) {
            ItemStack itemStack = item.getItemStack().clone();

            if (item.getPickupDelay() == 0) {
                item.setPickupDelay(25);
                continue;
            }

            if (itemStack.getType().name().contains("SHULKER_BOX"))
                return;

            if (itemStack.hasItemMeta() && itemStack.getItemMeta().hasDisplayName() &&
                    StringUtils.substring(itemStack.getItemMeta().getDisplayName(), 0, 3).equals("***")) {
                return; //Compatibility with Shop instance: https://www.spigotmc.org/resources/shop-a-simple-intuitive-shop-instance.9628/
            }

            if (!canMove(hopperInventory, itemStack) || blacklist.contains(item.getUniqueId()))
                return;

            addItems(item, hopperInventory);


            float xx = (float) (0 + (Math.random() * .1));
            float yy = (float) (0 + (Math.random() * .1));
            float zz = (float) (0 + (Math.random() * .1));

            if (EpicHoppers.getInstance().isServerVersionAtLeast(ServerVersion.V1_9))
                item.getLocation().getWorld().spawnParticle(Particle.FLAME, item.getLocation(), 5, xx, yy, zz, 0);

            HopTask.updateAdjacentComparators(hopper.getLocation());
        }
    }

    private void addItems(Item item, Inventory inventory) {
        int amount = getActualItemAmount(item);

        while (amount > 0) {
            int subtract = Math.min(amount, 64);
            amount -= subtract;
            ItemStack newItem = item.getItemStack().clone();
            newItem.setAmount(subtract);
            Map<Integer, ItemStack> result = inventory.addItem(newItem);
            if (result.get(0) != null) {
                amount += result.get(0).getAmount();
                break;
            }
        }

        if (amount <= 0) {
            blacklist.add(item.getUniqueId());
            item.remove();
        } else
            updateAmount(item, amount);
    }

    private int getActualItemAmount(Item item) {
        if (ultimateStacker) {
            return Methods.getActualItemAmount(item);
        } else if (wildStacker)
            return WildStackerAPI.getItemAmount(item);
        else
            return item.getItemStack().getAmount();

    }

    private void updateAmount(Item item, int amount) {
        if (ultimateStacker)
            Methods.updateItemAmount(item, amount);
        else if (wildStacker)
            WildStackerAPI.getStackedItem(item).setStackAmount(amount, true);
        else
            item.getItemStack().setAmount(amount > item.getItemStack().getMaxStackSize()
                    ? item.getItemStack().getMaxStackSize() : amount);
    }

    public static boolean isBlacklisted(UUID uuid) {
        if (blacklist.contains(uuid))
            Bukkit.getScheduler().runTaskLater(EpicHoppers.getInstance(),
                    () -> blacklist.remove(uuid), 10L);

        return blacklist.contains(uuid);
    }

    @Override
    public ItemStack getGUIButton(Hopper hopper) {
        return null;
    }

    @Override
    public void runButtonPress(Player player, Hopper hopper, ClickType type) {
    }

    @Override
    public List<Material> getBlockedItems(Hopper hopper) {
        return null;
    }

    @Override
    public String getDescription() {
        return EpicHoppers.getInstance().getLocale().getMessage("interface.hopper.suction")
                .processPlaceholder("suction", amount).getMessage();
    }

    private boolean canMove(Inventory inventory, ItemStack item) {
        if (inventory.firstEmpty() != -1) return true;

        for (ItemStack stack : inventory.getContents()) {
            if (stack.isSimilar(item) && (stack.getAmount() + item.getAmount()) < stack.getMaxStackSize()) {
                return true;
            }
        }
        return false;
    }
}
