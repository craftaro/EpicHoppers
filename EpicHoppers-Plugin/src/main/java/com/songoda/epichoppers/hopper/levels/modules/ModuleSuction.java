package com.songoda.epichoppers.hopper.levels.modules;

import com.songoda.arconix.plugin.Arconix;
import com.songoda.epichoppers.EpicHoppersPlugin;
import com.songoda.epichoppers.api.hopper.Hopper;
import com.songoda.epichoppers.api.hopper.levels.modules.Module;
import com.songoda.epichoppers.utils.Debugger;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;

import java.util.Collection;
import java.util.List;

public class ModuleSuction implements Module {
    
    private final int amount;
    
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

        for (Entity e : nearbyEntite) {
            if (!(e instanceof Item) || e.getTicksLived() < 10 || e.getLocation().getBlock().getType() == Material.HOPPER) {
                continue;
            }
            ItemStack hopItem = ((Item) e).getItemStack().clone();
            if (hopItem.getType().name().contains("SHULKER_BOX"))
                continue;
            if (hopItem.hasItemMeta() && hopItem.getItemMeta().hasDisplayName() &&
                    StringUtils.substring(hopItem.getItemMeta().getDisplayName(), 0, 3).equals("***")) {
                continue; //Compatibility with Shop instance: https://www.spigotmc.org/resources/shop-a-simple-intuitive-shop-instance.9628/
            }
            if (e.hasMetadata("grabbed"))
                continue;
            ItemStack item = ((Item) e).getItemStack();
            if (!canMove(hopperBlock.getInventory(), item)) {
                continue;
            }
            ((Item) e).setPickupDelay(999);
            e.setMetadata("grabbed", new FixedMetadataValue(EpicHoppersPlugin.getInstance(), ""));
            if (!e.isOnGround())
                continue;
            float xx = (float) (0 + (Math.random() * .3));
            float yy = (float) (0 + (Math.random() * .3));
            float zz = (float) (0 + (Math.random() * .3));
            Arconix.pl().getApi().packetLibrary.getParticleManager().broadcastParticle(e.getLocation(), xx, yy, zz, 0, "FLAME", 5);
            e.remove();
            hopperBlock.getInventory().addItem(hopItem);
            break;
        }
    }

    @Override
    public List<Material> getBlockedItems(Hopper hopper) {
        return null;
    }

    @Override
    public String getDescription() {
        return EpicHoppersPlugin.getInstance().getLocale().getMessage("interface.hopper.suction", true);
    }

    private boolean canMove(Inventory inventory, ItemStack item) {
        try {
            if (inventory.firstEmpty() != -1) return true;

            for (ItemStack stack : inventory.getContents()) {
                if (stack.isSimilar(item) && stack.getAmount() < stack.getMaxStackSize()) {
                    return true;
                }
            }
        } catch (Exception e) {
            Debugger.runReport(e);
        }
        return false;
    }
}
