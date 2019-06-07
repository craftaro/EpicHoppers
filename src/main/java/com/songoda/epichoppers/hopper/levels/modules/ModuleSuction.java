package com.songoda.epichoppers.hopper.levels.modules;

import com.bgsoftware.wildstacker.api.WildStackerAPI;
import com.songoda.epichoppers.EpicHoppers;
import com.songoda.epichoppers.hopper.Hopper;
import com.songoda.epichoppers.tasks.HopTask;
import com.songoda.epichoppers.utils.ServerVersion;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class ModuleSuction implements Module {

    private final int amount;

    public static List<UUID> blacklist = new ArrayList<>();

    private boolean wildStacker = Bukkit.getPluginManager().isPluginEnabled("WildStacker");
    private boolean ultimateStacker = Bukkit.getPluginManager().isPluginEnabled("UltimateStacker");

    private Class<?> clazzItemStack, clazzItem, clazzCraftItemStack;
    private Method methodGetItem, methodAsNMSCopy;
    private Field fieldMaxStackSize;

    public ModuleSuction(int amount) {
        this.amount = amount;
        try {
            String ver = Bukkit.getServer().getClass().getPackage().getName().substring(23);
            clazzCraftItemStack = Class.forName("org.bukkit.craftbukkit." + ver + ".inventory.CraftItemStack");
            clazzItemStack = Class.forName("net.minecraft.server." + ver + ".ItemStack");
            clazzItem = Class.forName("net.minecraft.server." + ver + ".Item");

            methodAsNMSCopy = clazzCraftItemStack.getMethod("asNMSCopy", ItemStack.class);
            methodGetItem = clazzItemStack.getDeclaredMethod("getItem");

            fieldMaxStackSize = clazzItem.getDeclaredField("maxStackSize");
            fieldMaxStackSize.setAccessible(true);
        } catch (ReflectiveOperationException e) {
            e.printStackTrace();
        }

        Bukkit.getScheduler().scheduleSyncRepeatingTask(EpicHoppers.getInstance(), blacklist::clear, 0L, 100L);
    }


    public String getName() {
        return "Suction";
    }


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
            ItemStack itemStack = setMax(item.getItemStack().clone(), 0, true);

            if (itemStack.getType().name().contains("SHULKER_BOX"))
                return;

            if (itemStack.hasItemMeta() && itemStack.getItemMeta().hasDisplayName() &&
                    StringUtils.substring(itemStack.getItemMeta().getDisplayName(), 0, 3).equals("***")) {
                return; //Compatibility with Shop instance: https://www.spigotmc.org/resources/shop-a-simple-intuitive-shop-instance.9628/
            }

            if (wildStacker)
                itemStack.setAmount(WildStackerAPI.getItemAmount(item));

            if (ultimateStacker && item.hasMetadata("US_AMT"))
                itemStack.setAmount(item.getMetadata("US_AMT").get(0).asInt());

            if (!canMove(hopperInventory, itemStack) || blacklist.contains(item.getUniqueId()))
                return;

            blacklist.add(item.getUniqueId());

            float xx = (float) (0 + (Math.random() * .1));
            float yy = (float) (0 + (Math.random() * .1));
            float zz = (float) (0 + (Math.random() * .1));

            if (EpicHoppers.getInstance().isServerVersionAtLeast(ServerVersion.V1_9))
                item.getLocation().getWorld().spawnParticle(Particle.FLAME, item.getLocation(), 5, xx, yy, zz, 0);

            for (ItemStack is : hopperInventory.addItem(itemStack).values())
                item.getWorld().dropItemNaturally(item.getLocation(), is);

            HopTask.updateAdjacentComparators(hopper.getLocation());
            item.remove();
        }
    }

    public static boolean isBlacklisted(UUID uuid) {
        return blacklist.contains(uuid);
    }

    public static boolean addToBlacklist(UUID uuid) {
        return blacklist.add(uuid);
    }

    public ItemStack getGUIButton(Hopper hopper) {
        return null;
    }

    public void runButtonPress(Player player, Hopper hopper) {

    }

    public List<Material> getBlockedItems(Hopper hopper) {
        return null;
    }


    public String getDescription() {
        return EpicHoppers.getInstance().getLocale().getMessage("interface.hopper.suction", amount);
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

    public ItemStack setMax(ItemStack item, int max, boolean reset) {
        try {
            Object objItemStack = methodGetItem.invoke(methodAsNMSCopy.invoke(null, item));
            fieldMaxStackSize.set(objItemStack, reset ? new ItemStack(item.getType()).getMaxStackSize() : max);
        } catch (ReflectiveOperationException e) {
            e.printStackTrace();
        }
        return item;
    }
}
