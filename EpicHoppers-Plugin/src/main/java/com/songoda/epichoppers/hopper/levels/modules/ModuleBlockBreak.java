package com.songoda.epichoppers.hopper.levels.modules;

import com.songoda.epichoppers.EpicHoppersPlugin;
import com.songoda.epichoppers.api.hopper.Hopper;
import com.songoda.epichoppers.api.hopper.levels.modules.Module;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ModuleBlockBreak implements Module {

    private final int amount;
    private Map<Block, Integer> blockTick = new HashMap<>();

    public ModuleBlockBreak(int amount) {
        this.amount = amount;
    }

    @Override
    public String getName() {
        return "BlockBreak";
    }

    @Override
    public void run(Hopper hopper) {
        Block block = hopper.getLocation().getBlock();

        if (!blockTick.containsKey(block)) {
            blockTick.put(block, 1);
            return;
        }
        int tick = blockTick.get(block);
        int put = tick + 1;
        blockTick.put(block, put);
        if (tick < amount) return;
        Block above = block.getRelative(0, 1, 0);
        if (above.getType() == Material.WATER || above.getType() == Material.LAVA) return;

        if (above.getType() != Material.AIR && above.getType() != Material.HOPPER && !EpicHoppersPlugin.getInstance().getConfig().getStringList("Main.BlockBreak Blacklisted Blocks").contains(above.getType().name())) {
            above.getWorld().playSound(above.getLocation(), Sound.BLOCK_STONE_BREAK, 1F, 1F);
            Location locationAbove = above.getLocation();
            locationAbove.add(.5, .5, .5);

            float xx = (float) (0 + (Math.random() * .5));
            float yy = (float) (0 + (Math.random() * .5));
            float zz = (float) (0 + (Math.random() * .5));
            above.getWorld().spawnParticle(Particle.valueOf(EpicHoppersPlugin.getInstance().getConfig().getString("Main.BlockBreak Particle Type")), locationAbove, 15, xx, yy, zz);

            above.breakNaturally();
        }
        blockTick.remove(block);
    }

    @Override
    public List<Material> getBlockedItems(Hopper hopper) {
        return null;
    }

    @Override
    public String getDescription() {
        return EpicHoppersPlugin.getInstance().getLocale().getMessage("interface.hopper.blockbreak", amount);
    }
}
