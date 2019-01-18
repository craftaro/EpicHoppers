package com.songoda.epichoppers.hopper.levels.modules;

import com.songoda.epichoppers.EpicHoppersPlugin;
import com.songoda.epichoppers.api.hopper.Hopper;
import com.songoda.epichoppers.api.hopper.levels.modules.Module;
import com.songoda.epichoppers.utils.Debugger;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import xyz.wildseries.wildstacker.api.WildStackerAPI;

import java.util.Collection;
import java.util.List;

public class ModuleSuction implements Module {

    private final int amount;

    private boolean wildStacker = Bukkit.getPluginManager().isPluginEnabled("WildStacker");

    public ModuleSuction(int amount) {
        this.amount = amount;
    }

    @Override
    public String getName() {
        return "Suction";
    }

    @Override
    public void run(Hopper hopper) {
        double radius = amount + .5;

        org.bukkit.block.Hopper hopperBlock = hopper.getHopper();

        Collection<Entity> nearbyEntite = hopper.getLocation().getWorld().getNearbyEntities(hopper.getLocation().add(0.5, 0.5, 0.5), radius, radius, radius);

        for (Entity entity : nearbyEntite) {
            if (!(entity instanceof Item) || entity.getTicksLived() < 10 || entity.getLocation().getBlock().getType() == Material.HOPPER) {
                continue;
            }

            ItemStack hopItem = ((Item) entity).getItemStack().clone();
            if (hopItem.getType().name().contains("SHULKER_BOX"))
                continue;

            if (hopItem.hasItemMeta() && hopItem.getItemMeta().hasDisplayName() &&
                    StringUtils.substring(hopItem.getItemMeta().getDisplayName(), 0, 3).equals("***")) {
                continue; //Compatibility with Shop instance: https://www.spigotmc.org/resources/shop-a-simple-intuitive-shop-instance.9628/
            }
            if (entity.hasMetadata("grabbed") || !entity.isOnGround())
                continue;

            if (wildStacker)
                hopItem.setAmount(WildStackerAPI.getItemAmount((Item) entity));

            ItemStack item = ((Item) entity).getItemStack();
            if (item == null) continue;
            if (!canMove(hopperBlock.getInventory(), item)) {
                continue;
            }
            ((Item) entity).setPickupDelay(10);
            entity.setMetadata("grabbed", new FixedMetadataValue(EpicHoppersPlugin.getInstance(), ""));
            float xx = (float) (0 + (Math.random() * .1));
            float yy = (float) (0 + (Math.random() * .1));
            float zz = (float) (0 + (Math.random() * .1));
            entity.getLocation().getWorld().spawnParticle(Particle.FLAME, entity.getLocation(), 5, xx, yy, zz, 0);

            for (ItemStack itemStack : hopperBlock.getInventory().addItem(hopItem).values()) {
                entity.getWorld().dropItemNaturally(entity.getLocation(), itemStack);
            }
            entity.remove();
            break;
        }
    }

    @Override
    public List<Material> getBlockedItems(Hopper hopper) {
        return null;
    }

    @Override
    public String getDescription() {
        return EpicHoppersPlugin.getInstance().getLocale().getMessage("interface.hopper.suction", amount);
    }

    private boolean canMove(Inventory inventory, ItemStack item) {
        try {
            if (inventory.firstEmpty() != -1) return true;

            for (ItemStack stack : inventory.getContents()) {
                if (stack.isSimilar(item) && (stack.getAmount() + item.getAmount()) < stack.getMaxStackSize()) {
                    return true;
                }
            }
        } catch (Exception e) {
            Debugger.runReport(e);
        }
        return false;
    }
}
