package com.songoda.epichoppers.hopper.levels.modules;

import com.bgsoftware.wildstacker.api.WildStackerAPI;
import com.songoda.core.SongodaPlugin;
import com.songoda.core.gui.GuiManager;
import com.songoda.core.locale.Locale;
import com.songoda.third_party.com.cryptomorin.xseries.XMaterial;
import com.songoda.core.utils.TextUtils;
import com.songoda.epichoppers.hopper.Hopper;
import com.songoda.epichoppers.settings.Settings;
import com.songoda.epichoppers.utils.Methods;
import com.songoda.epichoppers.utils.StorageContainerCache;
import com.songoda.third_party.com.cryptomorin.xseries.particles.XParticle;
import com.songoda.ultimatestacker.api.UltimateStackerApi;
import dev.rosewood.rosestacker.api.RoseStackerAPI;
import dev.rosewood.rosestacker.stack.StackedItem;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class ModuleSuction extends Module {
    private static final List<UUID> BLACKLIST = new ArrayList<>();

    private static final boolean WILD_STACKER = Bukkit.getPluginManager().isPluginEnabled("WildStacker");
    private static final boolean ULTIMATE_STACKER = Bukkit.getPluginManager().isPluginEnabled("UltimateStacker");
    private static final boolean ROSE_STACKER = Bukkit.getPluginManager().isPluginEnabled("RoseStacker");

    private final int maxSearchRadius;

    public ModuleSuction(SongodaPlugin plugin, GuiManager guiManager, int amount) {
        super(plugin, guiManager);
        this.maxSearchRadius = amount;
    }

    @Override
    public String getName() {
        return "Suction";
    }

    @Override
    public void run(Hopper hopper, StorageContainerCache.Cache hopperCache) {
        double radius = getRadius(hopper) + .5;

        if (!isEnabled(hopper)) {
            return;
        }

        Set<Item> itemsToSuck = hopper.getLocation().getWorld()
                .getNearbyEntities(hopper.getLocation().add(0.5, 0.5, 0.5), radius, radius, radius)
                .stream()
                .filter(entity -> entity.getType() == EntityType.DROPPED_ITEM)
                .map(Item.class::cast)
                .filter(entity -> {
                    if (entity.isDead() || entity.getLocation().getBlock().getType() == Material.HOPPER) {
                        return false;
                    }

                    // For WildStacker items, bypass the PickupDelay check since we use their API
                    if (WILD_STACKER && WildStackerAPI.getStackedItem(entity) != null) {
                        return true;
                    }

                    // For UltimateStacker items, bypass the PickupDelay check
                    if (ULTIMATE_STACKER && UltimateStackerApi.getStackedItemManager().isStackedItem(entity)) {
                        return true;
                    }

                    // For RoseStacker items, bypass the PickupDelay check
                    if (ROSE_STACKER && RoseStackerAPI.getInstance().getStackedItem(entity) != null) {
                        return true;
                    }

                    // For normal items, check PickupDelay
                    return entity.getTicksLived() >= entity.getPickupDelay();
                })
                .collect(Collectors.toSet());

        if (itemsToSuck.isEmpty()) {
            return;
        }

        boolean filterEndpoint = hopper.getFilter().getEndPoint() != null;

        // Always create inventory for the suction module to allow plugins to cancel the pickup event
        // Check if the hopper block is still valid before creating the inventory
        if (!(hopper.getBlock().getState() instanceof InventoryHolder)) {
            // Hopper block is no longer valid (chunk unloaded, block removed, etc.)
            return;
        }

        InventoryHolder inventoryHolder = (InventoryHolder) hopper.getBlock().getState();
        Inventory hopperInventory = Bukkit.createInventory(inventoryHolder, InventoryType.HOPPER);

        for (Item item : itemsToSuck) {
            ItemStack itemStack = item.getItemStack();

            // Check if this is a stacker plugin item
            boolean isStackerItem = (WILD_STACKER && WildStackerAPI.getStackedItem(item) != null)
                    || (ULTIMATE_STACKER && UltimateStackerApi.getStackedItemManager().isStackedItem(item))
                    || (ROSE_STACKER && RoseStackerAPI.getInstance().getStackedItem(item) != null);

            // Skip items with PickupDelay 0 (unless they're stacker items, which we handle via API)
            if (!isStackerItem && item.getPickupDelay() == 0) {
                item.setPickupDelay(25);
                continue;
            }

            if (itemStack.getType().name().contains("SHULKER_BOX")) {
                continue;
            }

            if (itemStack.hasItemMeta() && itemStack.getItemMeta().hasDisplayName() &&
                    itemStack.getItemMeta().getDisplayName().startsWith("***")) {
                continue; //Compatibility with Shop instance: https://www.spigotmc.org/resources/shop-a-simple-intuitive-shop-instance.9628/
            }

            if (BLACKLIST.contains(item.getUniqueId())) {
                continue;
            }

            // CRITICAL: Check if this item is in the VOID filter first
            // Void items should be DELETED immediately without entering the hopper
            boolean isVoidItem = hopper.getFilter().getVoidList().stream()
                    .anyMatch(filterItem -> Methods.isSimilarMaterial(itemStack, filterItem));

            if (isVoidItem) {
                // VOID filter: Delete the item immediately without adding to hopper
                // This prevents the item from consuming transfer cycles
                item.remove();
                continue;
            }

            // respect filter if no endpoint
            if (!filterEndpoint
                    && !(hopper.getFilter().getWhiteList().isEmpty() && hopper.getFilter().getBlackList().isEmpty())) {
                // this hopper has a filter with no rejection endpoint, so don't absorb disallowed items
                // whitelist has priority
                if (!hopper.getFilter().getWhiteList().isEmpty()) {
                    // is this item on the whitelist?
                    if (hopper.getFilter().getWhiteList().stream().noneMatch(filterItem -> Methods.isSimilarMaterial(itemStack, filterItem))) {
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

            // IMPORTANT: Read the actual item amount BEFORE emitting the event!
            // WildStacker (priority HIGHEST) modifies the item during the event,
            // so we must capture the correct amount first
            int toAdd = getActualItemAmount(item);

            // Check if autocrafter is active AND has a configured recipe
            // Only reserve slot if there's actually a recipe configured
            boolean hasAutoCrafter = false;
            ModuleAutoCrafting autoCrafter = (ModuleAutoCrafting) hopper.getLevel().getModule("AutoCrafting");
            if (autoCrafter != null) {
                ItemStack autoCrafting = autoCrafter.getAutoCrafting(hopper);
                hasAutoCrafter = autoCrafting != null && autoCrafting.getType() != Material.AIR;
            }

            // Try to add the items to the hopper FIRST
            int added = hopperCache.addAny(itemStack, toAdd, hasAutoCrafter);

            // CRITICAL FIX: Do NOT emit the event if we can't add anything!
            // Emitting the event when added == 0 causes WildStacker/UltimateStacker to modify
            // the entity amount even though we're not picking it up, resulting in item loss!
            if (added == 0) {
                continue; // Continue to next item instead of return
            }

            // We added items! Now emit the event to allow other plugins to be aware
            // Note: Cache is already modified at this point, but this prevents WildStacker
            // from modifying entities we can't pick up anyway
            hopperInventory.setContents(hopperCache.cachedInventory);
            InventoryPickupItemEvent pickupEvent = new InventoryPickupItemEvent(hopperInventory, item);
            Bukkit.getPluginManager().callEvent(pickupEvent);

            if (pickupEvent.isCancelled()) {
                // Check if this is a protected item (from shops or other plugins) first
                // Protected items should always respect the cancellation to maintain compatibility
                boolean isProtectedItem = itemStack.hasItemMeta() && (
                    itemStack.getItemMeta().hasDisplayName() ||
                    itemStack.getItemMeta().hasLore() ||
                    !itemStack.getItemMeta().getPersistentDataContainer().isEmpty()
                );

                if (isProtectedItem) {
                    // This item is protected by another plugin - ROLLBACK the cache to prevent duplication!
                    hopperCache.rollbackAdd(itemStack, added);
                    continue;
                } else if (!isStackerItem) {
                    // Normal item that's been cancelled - ROLLBACK the cache
                    hopperCache.rollbackAdd(itemStack, added);
                    continue;
                }

                // If we get here, it's a stacker item that's not protected, so we bypass the cancellation
                // This allows suction to work properly with stacker plugins
            }

            // items added ok!
            if (added >= toAdd) {
                // All items were added, remove the entity
                item.remove();
            } else {
                // Only some items were added, update the remaining amount
                int remaining = toAdd - added;
                updateAmount(item, remaining);

                // wait before trying to add again
                BLACKLIST.add(item.getUniqueId());
                Bukkit.getScheduler().runTaskLater(this.plugin,
                        () -> BLACKLIST.remove(item.getUniqueId()), 10L);
            }

            float xx = (float) (0 + (Math.random() * .1));
            float yy = (float) (0 + (Math.random() * .1));
            float zz = (float) (0 + (Math.random() * .1));
            item.getWorld().spawnParticle(XParticle.FLAME.get(), item.getLocation(), 5, xx, yy, zz, 0);
        }
    }

    private int getActualItemAmount(Item item) {
        if (ULTIMATE_STACKER) {
            return UltimateStackerApi.getStackedItemManager().getActualItemAmount(item);
        } else if (WILD_STACKER) {
            return WildStackerAPI.getItemAmount(item);
        } else if(ROSE_STACKER) {
            StackedItem stackedItem = RoseStackerAPI.getInstance().getStackedItem(item);
            if (stackedItem != null) {
                return stackedItem.getStackSize();
            }
        }
        return item.getItemStack().getAmount();
    }

    private void updateAmount(Item item, int amount) {
        if (ULTIMATE_STACKER) {
            UltimateStackerApi.getStackedItemManager().updateStack(item, amount);
        } else if (WILD_STACKER) {
            com.bgsoftware.wildstacker.api.objects.StackedItem stackedItem = WildStackerAPI.getStackedItem(item);
            if (stackedItem != null) {
                stackedItem.setStackAmount(amount, true);
            } else {
                // Fallback if item is not tracked by WildStacker
                item.getItemStack().setAmount(Math.min(amount, item.getItemStack().getMaxStackSize()));
            }
        } else if (ROSE_STACKER) {
            StackedItem stackedItem = RoseStackerAPI.getInstance().getStackedItem(item);
            if (stackedItem != null) {
                stackedItem.setStackSize(amount);
            } else {
                // Fallback if item is not tracked by RoseStacker
                item.getItemStack().setAmount(Math.min(amount, item.getItemStack().getMaxStackSize()));
            }
        } else {
            item.getItemStack().setAmount(Math.min(amount, item.getItemStack().getMaxStackSize()));
        }
    }

    public static boolean isBlacklisted(UUID uuid) {
        return BLACKLIST.contains(uuid);
    }

    @Override
    public ItemStack getGUIButton(Hopper hopper) {
        Locale locale = this.plugin.getLocale();
        ItemStack item = XMaterial.CAULDRON.parseItem();
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(locale.getMessage("interface.hopper.suctiontitle").toText());
        List<String> loreSuction = TextUtils.formatLore(
                locale.getMessage("interface.hopper.suctionlore")
                        .processPlaceholder("status", isEnabled(hopper) ? locale.getMessage("general.word.enabled").toText() : locale.getMessage("general.word.disabled").toText())
                        .processPlaceholder("radius", getRadius(hopper))
                        .toText()
        );
        meta.setLore(loreSuction);
        item.setItemMeta(meta);
        return item;
    }

    @Override
    public void runButtonPress(Player player, Hopper hopper, ClickType type) {
        if (type == ClickType.LEFT) {
            toggleEnabled(hopper);
        } else if (type == ClickType.RIGHT) {
            int setRadius = getRadius(hopper);
            if (setRadius >= this.maxSearchRadius) {
                setRadius(hopper, 1);
            } else {
                setRadius(hopper, ++setRadius);
            }
        }
    }


    private boolean isEnabled(Hopper hopper) {
        Object obj = getData(hopper, "enabled");
        return obj == null || (boolean) obj;
    }

    private void toggleEnabled(Hopper hopper) {
        saveData(hopper, "enabled", !isEnabled(hopper));
    }

    private int getRadius(Hopper hopper) {
        Object foundRadius = getData(hopper, "radius");
        return foundRadius == null ? this.maxSearchRadius : (int) foundRadius;
    }

    private void setRadius(Hopper hopper, int radius) {
        saveData(hopper, "radius", radius);
    }

    @Override
    public List<Material> getBlockedItems(Hopper hopper) {
        return null;
    }

    @Override
    public String getDescription() {
        return this.plugin.getLocale()
                .getMessage("interface.hopper.suction")
                .processPlaceholder("suction", this.maxSearchRadius)
                .toText();
    }
}
