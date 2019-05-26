package com.songoda.epichoppers.hopper.levels.modules;

import com.bgsoftware.wildstacker.api.WildStackerAPI;
import com.songoda.epichoppers.EpicHoppers;
import com.songoda.epichoppers.hopper.Hopper;
import com.songoda.epichoppers.utils.ServerVersion;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ModuleSuction implements Module {

    private final int amount;

    public static List<UUID> blacklist = new ArrayList<>();

    private boolean wildStacker = Bukkit.getPluginManager().isPluginEnabled("WildStacker");

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
    }


    public String getName() {
        return "Suction";
    }


    public void run(Hopper hopper, Inventory hopperInventory) {
        double radius = amount + .5;

        hopper.getLocation().getWorld().getNearbyEntities(hopper.getLocation().add(0.5, 0.5, 0.5), radius, radius, radius).stream()
                .filter(entity -> entity instanceof Item
                        && entity.getTicksLived() > 10
                        && entity.getLocation().getBlock().getType() != Material.HOPPER).forEach(entity -> {

            Item item = (Item) entity;
            ItemStack itemStack = setMax(item.getItemStack().clone(), 0, true);

            if (itemStack.getType().name().contains("SHULKER_BOX"))
                return;

            if (itemStack.hasItemMeta() && itemStack.getItemMeta().hasDisplayName() &&
                    StringUtils.substring(itemStack.getItemMeta().getDisplayName(), 0, 3).equals("***")) {
                return; //Compatibility with Shop instance: https://www.spigotmc.org/resources/shop-a-simple-intuitive-shop-instance.9628/
            }
            if (entity.hasMetadata("grabbed") || !entity.isOnGround())
                return;

            if (wildStacker)
                itemStack.setAmount(WildStackerAPI.getItemAmount((Item) entity));

            if (!canMove(hopperInventory, itemStack))
                return;

            blacklist.add(item.getUniqueId());

            ((Item) entity).setPickupDelay(10);
            entity.setMetadata("grabbed", new FixedMetadataValue(EpicHoppers.getInstance(), ""));
            float xx = (float) (0 + (Math.random() * .1));
            float yy = (float) (0 + (Math.random() * .1));
            float zz = (float) (0 + (Math.random() * .1));

            if (EpicHoppers.getInstance().isServerVersionAtLeast(ServerVersion.V1_9))
            entity.getLocation().getWorld().spawnParticle(Particle.FLAME, entity.getLocation(), 5, xx, yy, zz, 0);

            for (ItemStack is : hopperInventory.addItem(itemStack).values()) {
                entity.getWorld().dropItemNaturally(entity.getLocation(), is);
            }
            entity.remove();
        });
    }

    public static boolean isBlacklisted(UUID uuid) {
        return blacklist.contains(uuid);
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
