package com.craftaro.epichoppers.hopper.levels.modules;

import com.craftaro.core.SongodaPlugin;
import com.craftaro.core.compatibility.ServerVersion;
import com.craftaro.core.gui.GuiManager;
import com.craftaro.core.utils.TextUtils;
import com.craftaro.epichoppers.settings.Settings;
import com.craftaro.epichoppers.EpicHoppersApi;
import com.craftaro.epichoppers.hopper.Hopper;
import com.craftaro.epichoppers.utils.StorageContainerCache;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

public class ModuleBlockBreak extends Module {
    private static final Map<Hopper, Boolean> CACHED_BLOCKS = new ConcurrentHashMap<>();

    private final int ticksPerBreak;
    private final Map<Hopper, Integer> blockTick = new HashMap<>();

    public ModuleBlockBreak(SongodaPlugin plugin, GuiManager guiManager, int amount) {
        super(plugin, guiManager);
        this.ticksPerBreak = amount;
    }

    @Override
    public String getName() {
        return "BlockBreak";
    }

    @Override
    public void run(Hopper hopper, StorageContainerCache.Cache hopperCache) {
        if (!isEnabled(hopper)) {
            return;
        }

        // don't try to break stuff if we can't grab stuff
        // (for simplicity, just assume that no empty slots mean there's a good chance we won't be able to pick something new up)
        if (Stream.of(hopperCache.cachedInventory).allMatch(item -> item != null && item.getAmount() > 0)) {
            return;
        }

        Integer tick = this.blockTick.get(hopper);
        if (tick == null) {
            this.blockTick.put(hopper, 1);
            return;
        } else if (tick < this.ticksPerBreak) {
            this.blockTick.put(hopper, tick + 1);
            return;
        } else {
            this.blockTick.put(hopper, 0);
        }

        Block above = hopper.getLocation().getBlock().getRelative(0, 1, 0);

        // Don't break farm items from custom containers
        if (EpicHoppersApi.getApi().getContainerManager().getCustomContainer(above) != null) {
            return;
        }

        // don't break blacklisted blocks, fluids, or containers
        if (Settings.BLOCKBREAK_BLACKLIST.getStringList().contains(above.getType().name())
                || above.getType() == Material.WATER
                || above.getType() == Material.LAVA
                || above.getType() == Material.AIR) {
            return;
        }

        if (Settings.ALLOW_BLOCKBREAK_CONTAINERS.getBoolean()
                && above.getState() instanceof InventoryHolder) {
            return;
        }

        // Let's break the block!
        if (ServerVersion.isServerVersionAtLeast(ServerVersion.V1_9)) {
            above.getWorld().playSound(above.getLocation(), Sound.BLOCK_STONE_BREAK, 1F, 1F);
        }
        Location locationAbove = above.getLocation();
        locationAbove.add(.5, .5, .5);

        // fancy break particle effects :}
        if (ServerVersion.isServerVersionAtLeast(ServerVersion.V1_9)) {
            float xx = (float) (0 + (Math.random() * .5));
            float yy = (float) (0 + (Math.random() * .5));
            float zz = (float) (0 + (Math.random() * .5));

            Particle particle = null;
            if (!Settings.BLOCKBREAK_PARTICLE.getString().trim().isEmpty()) {
                try {
                    particle = Particle.valueOf(Settings.BLOCKBREAK_PARTICLE.getString());
                } catch (Exception ignore) {
                    particle = Particle.LAVA;
                }
            }

            if (particle != null) {
                above.getWorld().spawnParticle(particle, locationAbove, 15, xx, yy, zz);
            }
        }

        boolean waterlogged = ServerVersion.isServerVersionAtLeast(ServerVersion.V1_13)
                && above.getBlockData() instanceof org.bukkit.block.data.Waterlogged
                && ((org.bukkit.block.data.Waterlogged) above.getBlockData()).isWaterlogged();

        above.breakNaturally(new ItemStack(Material.DIAMOND_PICKAXE));

        if (waterlogged) {
            above.setType(Material.WATER);
        }
    }

    @Override
    public ItemStack getGUIButton(Hopper hopper) {
        ItemStack block = new ItemStack(Material.IRON_ORE, 1);
        ItemMeta blockMeta = block.getItemMeta();
        blockMeta.setDisplayName(this.plugin.getLocale().getMessage("interface.hopper.blocktitle").getMessage());
        ArrayList<String> loreBlock = new ArrayList<>();
        String[] parts = this.plugin.getLocale()
                .getMessage("interface.hopper.blocklore")
                .processPlaceholder("enabled", isEnabled(hopper)
                        ? this.plugin.getLocale().getMessage("general.word.enabled").getMessage()
                        : this.plugin.getLocale().getMessage("general.word.disabled").getMessage()
                )
                .getMessage()
                .split("\\|");
        for (String line : parts) {
            loreBlock.add(TextUtils.formatText(line));
        }
        blockMeta.setLore(loreBlock);
        block.setItemMeta(blockMeta);
        return block;
    }

    @Override
    public void runButtonPress(Player player, Hopper hopper, ClickType type) {
        setEnabled(hopper, !isEnabled(hopper));
    }

    @Override
    public List<Material> getBlockedItems(Hopper hopper) {
        return null;
    }

    @Override
    public String getDescription() {
        return this.plugin.getLocale().getMessage("interface.hopper.blockbreak")
                .processPlaceholder("ticks", this.ticksPerBreak).getMessage();
    }

    @Override
    public void clearData(Hopper hopper) {
        super.clearData(hopper);
        CACHED_BLOCKS.remove(hopper);
    }

    public void setEnabled(Hopper hopper, boolean enable) {
        saveData(hopper, "blockbreak", enable);
        CACHED_BLOCKS.put(hopper, enable);
    }

    public boolean isEnabled(Hopper hopper) {
        Boolean enabled = CACHED_BLOCKS.get(hopper);
        if (enabled == null) {
            Object isBlockBreaking = getData(hopper, "blockbreak");
            CACHED_BLOCKS.put(hopper, enabled = isBlockBreaking != null && (boolean) isBlockBreaking);
        }
        return enabled;
    }
}
