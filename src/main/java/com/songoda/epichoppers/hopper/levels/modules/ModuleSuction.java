package com.songoda.epichoppers.hopper.levels.modules;

import com.bgsoftware.wildstacker.api.WildStackerAPI;
import com.songoda.epichoppers.EpicHoppers;
import com.songoda.epichoppers.hopper.Hopper;
import com.songoda.epichoppers.utils.Methods;
import com.songoda.epichoppers.utils.ServerVersion;
import com.songoda.epichoppers.utils.StorageContainerCache;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class ModuleSuction extends Module {

    private final int searchRadius;

    public static List<UUID> blacklist = new ArrayList<>();

    private final static boolean wildStacker = Bukkit.getPluginManager().isPluginEnabled("WildStacker");
    private final static boolean ultimateStacker = Bukkit.getPluginManager().isPluginEnabled("UltimateStacker");
    private static boolean oldUltimateStacker;
    private static Method oldUltimateStacker_updateItemAmount;
    static {
        if (ultimateStacker) {
            try {
                oldUltimateStacker_updateItemAmount = com.songoda.ultimatestacker.utils.Methods.class.getDeclaredMethod("updateItemAmount", Item.class, int.class);
                oldUltimateStacker = true;
            } catch (NoSuchMethodException | SecurityException ex) {
            }
        } else {
            oldUltimateStacker = false;
        }
    }

    public ModuleSuction(EpicHoppers plugin, int amount) {
        super(plugin);
        this.searchRadius = amount;
    }

    @Override
    public String getName() {
        return "Suction";
    }

    @Override
    public void run(Hopper hopper, StorageContainerCache.Cache hopperCache) {
        double radius = searchRadius + .5;

        Set<Item> itemsToSuck = hopper.getLocation().getWorld().getNearbyEntities(hopper.getLocation().add(0.5, 0.5, 0.5), radius, radius, radius)
                .stream()
                .filter(entity -> entity.getType() == EntityType.DROPPED_ITEM
                        && !entity.isDead()
                        && entity.getTicksLived() >= ((Item) entity).getPickupDelay()
                        && entity.getLocation().getBlock().getType() != Material.HOPPER)
                .map(entity -> (Item) entity)
                .collect(Collectors.toSet());

        boolean filterEndpoint = hopper.getFilter().getEndPoint() != null;

        for (Item item : itemsToSuck) {

            ItemStack itemStack = item.getItemStack();

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

            if (blacklist.contains(item.getUniqueId()))
                return;

            // respect filter if no endpoint
            if (!filterEndpoint
                    && !(hopper.getFilter().getWhiteList().isEmpty() && hopper.getFilter().getBlackList().isEmpty())) {
                // this hopper has a filter with no rejection endpoint, so don't absorb disalowed items
                // whitelist has priority
                if (!hopper.getFilter().getWhiteList().isEmpty()) {
                    // is this item on the whitelist?
                    if (!hopper.getFilter().getWhiteList().stream().anyMatch(filterItem -> Methods.isSimilarMaterial(itemStack, filterItem))) {
                        // nope!
                        continue;
                    }
                } else {
                    // check the blacklist
                    if (hopper.getFilter().getBlackList().stream().anyMatch(filterItem -> Methods.isSimilarMaterial(itemStack, filterItem))) {
                        // don't grab this, then
                        continue;
                    }
                }
            }

            // try to add the items to the hopper
            int toAdd, added = hopperCache.addAny(itemStack, toAdd = getActualItemAmount(item));
            if (added == 0)
                return;

            // items added ok!
            if (added == toAdd)
                item.remove();
            else {
                // update the item's total
                updateAmount(item, toAdd - added);

                // wait before trying to add again
                blacklist.add(item.getUniqueId());
                Bukkit.getScheduler().runTaskLater(EpicHoppers.getInstance(),
                        () -> blacklist.remove(item.getUniqueId()), 10L);
            }

            if (EpicHoppers.getInstance().isServerVersionAtLeast(ServerVersion.V1_9)) {
                float xx = (float) (0 + (Math.random() * .1));
                float yy = (float) (0 + (Math.random() * .1));
                float zz = (float) (0 + (Math.random() * .1));
                item.getLocation().getWorld().spawnParticle(Particle.FLAME, item.getLocation(), 5, xx, yy, zz, 0);
            }
        }
    }

    private int getActualItemAmount(Item item) {
        if (ultimateStacker) {
            return com.songoda.ultimatestacker.utils.Methods.getActualItemAmount(item);
        } else if (wildStacker)
            return WildStackerAPI.getItemAmount(item);
        else
            return item.getItemStack().getAmount();

    }

    private void updateAmount(Item item, int amount) {
        if (ultimateStacker) {
            if (oldUltimateStacker) {
                try {
                    oldUltimateStacker_updateItemAmount.invoke(null, item, amount);
                } catch (Exception ex) {
                    item.remove(); // not the best solution, but they should update, anyway..
                }
            } else {
                com.songoda.ultimatestacker.utils.Methods.updateItemAmount(item, item.getItemStack(), amount);
            }
        } else if (wildStacker)
            WildStackerAPI.getStackedItem(item).setStackAmount(amount, true);
        else
            item.getItemStack().setAmount(amount > item.getItemStack().getMaxStackSize()
                    ? item.getItemStack().getMaxStackSize() : amount);
    }

    public static boolean isBlacklisted(UUID uuid) {
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
                .processPlaceholder("suction", searchRadius).getMessage();
    }
}
