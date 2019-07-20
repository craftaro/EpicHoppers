package com.songoda.epichoppers.hopper.levels.modules;

import com.songoda.epichoppers.EpicHoppers;
import com.songoda.epichoppers.hopper.Hopper;
import com.songoda.epichoppers.utils.Methods;
import com.songoda.epichoppers.utils.ServerVersion;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ModuleBlockBreak extends Module {

    private final int amount;
    private Map<Block, Integer> blockTick = new HashMap<>();

    public ModuleBlockBreak(EpicHoppers plugin, int amount) {
        super(plugin);
        this.amount = amount;
    }

    @Override
    public String getName() {
        return "BlockBreak";
    }

    @Override
    public void run(Hopper hopper, Inventory hopperInventory) {
        Block block = hopper.getLocation().getBlock();

        if (!isEnabled(hopper))
            return;

        if (!blockTick.containsKey(block)) {
            blockTick.put(block, 1);
            return;
        }
        int tick = blockTick.get(block);
        int put = tick + 1;
        blockTick.put(block, put);
        if (tick < amount)
            return;

        Block above = block.getRelative(0, 1, 0);
        if (above.getType() == Material.WATER
                || above.getType() == Material.LAVA
                || above.getType() == Material.AIR
                || above.getState() instanceof InventoryHolder)
            return;

        // Don't break farm items from EpicFarming
        if (plugin.isEpicFarming() && com.songoda.epicfarming.EpicFarmingPlugin.getInstance().getFarmManager().getFarm(above) != null)
            return;

        if (!plugin.getConfig().getStringList("Main.BlockBreak Blacklisted Blocks").contains(above.getType().name())) {
            if (plugin.isServerVersionAtLeast(ServerVersion.V1_9))
                above.getWorld().playSound(above.getLocation(), Sound.BLOCK_STONE_BREAK, 1F, 1F);
            Location locationAbove = above.getLocation();
            locationAbove.add(.5, .5, .5);

            float xx = (float) (0 + (Math.random() * .5));
            float yy = (float) (0 + (Math.random() * .5));
            float zz = (float) (0 + (Math.random() * .5));
            if (plugin.isServerVersionAtLeast(ServerVersion.V1_9))
                above.getWorld().spawnParticle(Particle.valueOf(plugin.getConfig().getString("Main.BlockBreak Particle Type")), locationAbove, 15, xx, yy, zz);

            boolean waterlogged = false;
            if (plugin.isServerVersionAtLeast(ServerVersion.V1_13)
                    && above.getBlockData() instanceof org.bukkit.block.data.Waterlogged
                    && ((org.bukkit.block.data.Waterlogged)above.getBlockData()).isWaterlogged()) {
                waterlogged = true;
            }

            above.breakNaturally();

            if (waterlogged)
                above.setType(Material.WATER);
        }
        blockTick.remove(block);
    }

    @Override
    public ItemStack getGUIButton(Hopper hopper) {
        ItemStack block = new ItemStack(Material.IRON_ORE, 1);
        ItemMeta blockmeta = block.getItemMeta();
        blockmeta.setDisplayName(plugin.getLocale().getMessage("interface.hopper.blocktitle").getMessage());
        ArrayList<String> loreblock = new ArrayList<>();
        String[] parts = plugin.getLocale().getMessage("interface.hopper.blocklore")
                .processPlaceholder("enabled", isEnabled(hopper)
                        ? plugin.getLocale().getMessage("general.word.enabled").getMessage()
                        : plugin.getLocale().getMessage("general.word.disabled").getMessage())
                .getMessage().split("\\|");
        for (String line : parts) {
            loreblock.add(Methods.formatText(line));
        }
        blockmeta.setLore(loreblock);
        block.setItemMeta(blockmeta);
        return block;
    }

    @Override
    public void runButtonPress(Player player, Hopper hopper, ClickType type) {
        saveData(hopper,"blockbreak", !isEnabled(hopper));
    }

    @Override
    public List<Material> getBlockedItems(Hopper hopper) {
        return null;
    }

    @Override
    public String getDescription() {
        return plugin.getLocale().getMessage("interface.hopper.blockbreak")
                .processPlaceholder("ticks", amount).getMessage();
    }

    public boolean isEnabled(Hopper hopper) {
        Object isBlockBreaking = getData(hopper, "blockbreak");
        return isBlockBreaking != null && (boolean) isBlockBreaking;
    }
}
