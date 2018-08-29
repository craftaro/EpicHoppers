package com.songoda.epichoppers.hopper.levels.modules;

import com.songoda.arconix.plugin.Arconix;
import com.songoda.epichoppers.EpicHoppersPlugin;
import com.songoda.epichoppers.api.hopper.Hopper;
import com.songoda.epichoppers.api.hopper.levels.modules.Module;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ModuleBlockBreak implements Module {

    private Map<Block, Integer> blockTick = new HashMap<>();

    private final int amount;

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
        if (above.getType() != Material.AIR && !EpicHoppersPlugin.getInstance().getConfig().getStringList("Main.BlockBreak Blacklisted Blocks").contains(above.getType().name())) {
            above.getWorld().playSound(above.getLocation(), Sound.BLOCK_STONE_BREAK, 1F, 1F);
            Location locationAbove = above.getLocation();
            locationAbove.add(.5, .5, .5);

            float ox = (float) (0 + (Math.random() * .5));
            float oy = (float) (0 + (Math.random() * .5));
            float oz = (float) (0 + (Math.random() * .5));
            Arconix.pl().getApi().packetLibrary.getParticleManager().broadcastParticle(locationAbove, ox, oy, oz, 0, EpicHoppersPlugin.getInstance().getConfig().getString("Main.BlockBreak Particle Type"), 15);
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
        return EpicHoppersPlugin.getInstance().getLocale().getMessage("interface.hopper.blockbreak", true);
    }
}
