package com.songoda.epichoppers.Hopper;

import com.songoda.arconix.plugin.Arconix;
import com.songoda.epichoppers.EpicHoppers;
import com.songoda.epichoppers.Utils.Debugger;
import com.songoda.epichoppers.Utils.Methods;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Created by songoda on 3/14/2017.
 */
public class Hopper {

    private Location location;
    private Level level;
    private UUID lastPlayer;
    private Block syncedBlock;
    private Filter filter;
    private boolean walkOnTeleport;


    public Hopper(Location location, Level level, UUID lastPlayer, Block syncedBlock, Filter filter, boolean walkOnTeleport) {
        this.location = location;
        this.level = level;
        this.syncedBlock = syncedBlock;
        this.filter = filter;
        this.lastPlayer = lastPlayer;
        this.walkOnTeleport = walkOnTeleport;
    }

    public Hopper(Block block, Level level, UUID lastPlayer, Block syncedBlock, Filter filter, boolean walkOnTeleport) {
        this(block.getLocation(), level, lastPlayer, syncedBlock, filter, walkOnTeleport);
    }

    public void overview(Player player) {
        try {
            EpicHoppers instance = EpicHoppers.getInstance();
            if (!player.hasPermission("epichoppers.overview")) return;

            instance.lastBlock.put(player, location.getBlock());

            Level nextLevel = instance.getLevelManager().getHighestLevel().getLevel() > level.getLevel() ? instance.getLevelManager().getLevel(level.getLevel() + 1) : null;

            Inventory i = Bukkit.createInventory(null, 27, Methods.formatName(level.getLevel(), false));

            ItemStack perl = new ItemStack(Material.ENDER_PEARL, 1);
            ItemMeta perlmeta = perl.getItemMeta();
            perlmeta.setDisplayName(instance.getLocale().getMessage("interface.hopper.perltitle"));
            ArrayList<String> loreperl = new ArrayList<>();
            String[] parts = instance.getLocale().getMessage("interface.hopper.perllore").split("\\|");
            for (String line : parts) {
                loreperl.add(Arconix.pl().getApi().format().formatText(line));
            }
            perlmeta.setLore(loreperl);
            perl.setItemMeta(perlmeta);

            ItemStack filter = new ItemStack(Material.REDSTONE_COMPARATOR, 1);
            ItemMeta filtermeta = filter.getItemMeta();
            filtermeta.setDisplayName(instance.getLocale().getMessage("interface.hopper.filtertitle"));
            ArrayList<String> lorefilter = new ArrayList<>();
            parts = instance.getLocale().getMessage("interface.hopper.filterlore").split("\\|");
            for (String line : parts) {
                lorefilter.add(Arconix.pl().getApi().format().formatText(line));
            }
            filtermeta.setLore(lorefilter);
            filter.setItemMeta(filtermeta);


            ItemStack item = new ItemStack(Material.HOPPER, 1);
            ItemMeta itemmeta = item.getItemMeta();
            itemmeta.setDisplayName(instance.getLocale().getMessage("interface.hopper.currentlevel", level.getLevel()));
            List<String> lore = this.level.getDescription();
            lore.add("");
            if (nextLevel == null) lore.add(instance.getLocale().getMessage("interface.hopper.alreadymaxed"));
            else {
                lore.add(instance.getLocale().getMessage("interface.hopper.nextlevel", nextLevel.getLevel()));
                lore.addAll(nextLevel.getDescription());
            }

            itemmeta.setLore(lore);
            item.setItemMeta(itemmeta);

            ItemStack hook = new ItemStack(Material.TRIPWIRE_HOOK, 1);
            ItemMeta hookmeta = hook.getItemMeta();
            hookmeta.setDisplayName(instance.getLocale().getMessage("interface.hopper.synchopper"));
            ArrayList<String> lorehook = new ArrayList<>();
            parts = instance.getLocale().getMessage("interface.hopper.synclore").split("\\|");
            for (String line : parts) {
                lorehook.add(Arconix.pl().getApi().format().formatText(line));
            }
            hookmeta.setLore(lorehook);
            hook.setItemMeta(hookmeta);

            ItemStack itemXP = new ItemStack(Material.valueOf(instance.getConfig().getString("Interfaces.XP Icon")), 1);
            ItemMeta itemmetaXP = itemXP.getItemMeta();
            itemmetaXP.setDisplayName(instance.getLocale().getMessage("interface.hopper.upgradewithxp"));
            ArrayList<String> loreXP = new ArrayList<>();
            if (nextLevel != null)
                loreXP.add(instance.getLocale().getMessage("interface.hopper.upgradewithxplore", nextLevel.getCostExperience()));
            else
                loreXP.add(instance.getLocale().getMessage("interface.hopper.alreadymaxed"));
            itemmetaXP.setLore(loreXP);
            itemXP.setItemMeta(itemmetaXP);

            ItemStack itemECO = new ItemStack(Material.valueOf(instance.getConfig().getString("Interfaces.Economy Icon")), 1);
            ItemMeta itemmetaECO = itemECO.getItemMeta();
            itemmetaECO.setDisplayName(instance.getLocale().getMessage("interface.hopper.upgradewitheconomy"));
            ArrayList<String> loreECO = new ArrayList<>();
            if (nextLevel != null)
                loreECO.add(instance.getLocale().getMessage("interface.hopper.upgradewitheconomylore", Arconix.pl().getApi().format().formatEconomy(nextLevel.getCostEconomy())));
            else
                loreECO.add(instance.getLocale().getMessage("interface.hopper.alreadymaxed"));
            itemmetaECO.setLore(loreECO);
            itemECO.setItemMeta(itemmetaECO);

            int nu = 0;
            while (nu != 27) {
                i.setItem(nu, Methods.getGlass());
                nu++;
            }

            boolean canFilter = instance.getConfig().getBoolean("Main.Allow Players To use The Hopper Filter") && player.hasPermission("EpicHoppers.Filter");
            boolean canTeleport = instance.getConfig().getBoolean("Main.Allow Players To Teleport Through Hoppers") && player.hasPermission("EpicHoppers.Teleport");
            if (!canFilter && canTeleport)
                i.setItem(4, perl);
            else if (!canTeleport && canFilter)
                i.setItem(4, filter);
            else if (canFilter) {
                i.setItem(3, perl);
                i.setItem(5, filter);
            }

            if (instance.getConfig().getBoolean("Main.Upgrade With XP") && player.hasPermission("EpicHoppers.Upgrade.XP")) {
                i.setItem(11, itemXP);
            }

            i.setItem(13, item);
            i.setItem(22, hook);

            if (instance.getConfig().getBoolean("Main.Upgrade With Economy") && player.hasPermission("EpicHoppers.Upgrade.ECO")) {
                i.setItem(15, itemECO);
            }

            i.setItem(0, Methods.getBackgroundGlass(true));
            i.setItem(1, Methods.getBackgroundGlass(true));
            i.setItem(2, Methods.getBackgroundGlass(false));
            i.setItem(6, Methods.getBackgroundGlass(false));
            i.setItem(7, Methods.getBackgroundGlass(true));
            i.setItem(8, Methods.getBackgroundGlass(true));
            i.setItem(9, Methods.getBackgroundGlass(true));
            i.setItem(10, Methods.getBackgroundGlass(false));
            i.setItem(16, Methods.getBackgroundGlass(false));
            i.setItem(17, Methods.getBackgroundGlass(true));
            i.setItem(18, Methods.getBackgroundGlass(true));
            i.setItem(19, Methods.getBackgroundGlass(true));
            i.setItem(20, Methods.getBackgroundGlass(false));
            i.setItem(24, Methods.getBackgroundGlass(false));
            i.setItem(25, Methods.getBackgroundGlass(true));
            i.setItem(26, Methods.getBackgroundGlass(true));

            player.openInventory(i);
            instance.inShow.put(player.getUniqueId(), this);
        } catch (Exception e) {
            Debugger.runReport(e);
        }
    }

    public void filter(Player player) {
        try {
            EpicHoppers instance = EpicHoppers.getInstance();

            Inventory i = Bukkit.createInventory(null, 54, Arconix.pl().getApi().format().formatText(Methods.formatName(level.getLevel(), false) + " &8-&f Filter"));

            i.setItem(2, Methods.getBackgroundGlass(true));
            i.setItem(3, Methods.getBackgroundGlass(true));
            i.setItem(4, Methods.getBackgroundGlass(true));
            i.setItem(5, Methods.getBackgroundGlass(false));
            i.setItem(6, Methods.getBackgroundGlass(false));

            i.setItem(11, Methods.getBackgroundGlass(true));
            i.setItem(15, Methods.getBackgroundGlass(false));

            i.setItem(20, Methods.getBackgroundGlass(true));
            i.setItem(24, Methods.getBackgroundGlass(true));

            i.setItem(29, Methods.getBackgroundGlass(true));
            i.setItem(33, Methods.getBackgroundGlass(true));

            i.setItem(38, Methods.getBackgroundGlass(false));
            i.setItem(42, Methods.getBackgroundGlass(true));

            i.setItem(47, Methods.getBackgroundGlass(false));
            i.setItem(48, Methods.getBackgroundGlass(false));
            i.setItem(49, Methods.getBackgroundGlass(true));
            i.setItem(50, Methods.getBackgroundGlass(true));
            i.setItem(51, Methods.getBackgroundGlass(true));

            i.setItem(12, Methods.getGlass());
            i.setItem(14, Methods.getGlass());
            i.setItem(21, Methods.getGlass());
            i.setItem(22, Methods.getGlass());
            i.setItem(23, Methods.getGlass());
            i.setItem(30, Methods.getGlass());
            i.setItem(31, Methods.getGlass());
            i.setItem(32, Methods.getGlass());
            i.setItem(39, Methods.getGlass());
            i.setItem(41, Methods.getGlass());

            ItemStack it = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 0);
            ItemMeta itm = it.getItemMeta();
            itm.setDisplayName(instance.getLocale().getMessage("interface.filter.whitelist"));
            it.setItemMeta(itm);
            int[] whiteSlots = {0, 1, 9, 10, 18, 19};
            for (int nu : whiteSlots) {
                i.setItem(nu, it);
            }

            int num = 0;
            for (ItemStack o : filter.getWhiteList()) {
                if (o != null) {
                    i.setItem(whiteSlots[num], o);
                    num++;
                }
            }

            it = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 15);
            itm = it.getItemMeta();
            itm.setDisplayName(instance.getLocale().getMessage("interface.filter.blacklist"));
            it.setItemMeta(itm);
            int[] blackSlots = {27, 28, 36, 37, 45, 46};
            for (int nu : blackSlots) {
                i.setItem(nu, it);
            }

            num = 0;
            for (ItemStack o : filter.getBlackList()) {
                if (o != null) {
                    i.setItem(blackSlots[num], o);
                    num++;
                }
            }

            it = new ItemStack(Material.BARRIER);
            itm = it.getItemMeta();
            itm.setDisplayName(instance.getLocale().getMessage("interface.filter.void"));
            it.setItemMeta(itm);
            int[] avoid = {7, 8, 16, 17, 25, 26, 34, 35, 43, 44, 52, 53};
            for (int nu : avoid) {
                i.setItem(nu, it);
            }

            num = 0;
            for (ItemStack o : filter.getVoidList()) {
                if (o != null) {
                    i.setItem(avoid[num], o);
                    num++;
                }

            }

            ItemStack itemInfo = new ItemStack(Material.PAPER, 1);
            ItemMeta itemmetaInfo = itemInfo.getItemMeta();
            itemmetaInfo.setDisplayName(instance.getLocale().getMessage("interface.filter.infotitle"));
            ArrayList<String> loreInfo = new ArrayList<>();
            String[] parts = instance.getLocale().getMessage("interface.filter.infolore").split("\\|");
            for (String line : parts) {
                loreInfo.add(Arconix.pl().getApi().format().formatText(line));
            }
            itemmetaInfo.setLore(loreInfo);
            itemInfo.setItemMeta(itemmetaInfo);

            i.setItem(13, itemInfo);


            ItemStack hook = new ItemStack(Material.TRIPWIRE_HOOK, 1);
            ItemMeta hookmeta = hook.getItemMeta();
            hookmeta.setDisplayName(instance.getLocale().getMessage("interface.hopper.rejectsync"));
            ArrayList<String> lorehook = new ArrayList<>();
            parts = instance.getLocale().getMessage("interface.hopper.synclore").split("\\|");
            for (String line : parts) {
                lorehook.add(Arconix.pl().getApi().format().formatText(line));
            }
            hookmeta.setLore(lorehook);
            hook.setItemMeta(hookmeta);
            i.setItem(40, hook);

            player.openInventory(i);
            instance.inFilter.put(player.getUniqueId(), this);
        } catch (Exception e) {
            Debugger.runReport(e);
        }
    }

    public void compile(Player p) {
        try {
            ItemStack[] items2 = p.getOpenInventory().getTopInventory().getContents();

            List<ItemStack> owhite = new ArrayList<>();
            List<ItemStack> oblack = new ArrayList<>();
            List<ItemStack> ovoid = new ArrayList<>();

            int[] awhite = {0, 1, 9, 10, 18, 19};
            int[] ablack = {27, 28, 36, 37, 45, 46};
            int[] avoid = {7, 8, 16, 17, 25, 26, 34, 35, 43, 44, 52, 53};

            int num = 0;
            for (ItemStack item : items2) {
                for (int aa : awhite) {
                    if (aa == num) {
                        if (items2[num] != null && !items2[num].getType().equals(Material.STAINED_GLASS_PANE))
                            owhite.add(items2[num]);
                    }
                }
                for (int aa : ablack) {
                    if (aa == num) {
                        if (items2[num] != null && !items2[num].getType().equals(Material.STAINED_GLASS_PANE))
                            oblack.add(items2[num]);
                    }
                }
                for (int aa : avoid) {
                    if (aa == num) {
                        if (items2[num] != null && !items2[num].getType().equals(Material.BARRIER))
                            ovoid.add(items2[num]);
                    }
                }
                num++;
            }
            filter.setWhiteList(owhite);
            filter.setBlackList(oblack);
            filter.setVoidList(ovoid);
        } catch (Exception e) {
            Debugger.runReport(e);
        }
    }

    public void upgrade(String type, Player player) {
        try {
            EpicHoppers instance = EpicHoppers.getInstance();
            if (instance.getLevelManager().getLevels().containsKey(this.level.getLevel() + 1)) {

                Level level = instance.getLevelManager().getLevel(this.level.getLevel() + 1);
                int cost;
                if (type.equals("XP")) {
                    cost = level.getCostExperience();
                } else {
                    cost = level.getCostEconomy();
                }

                if (type.equals("ECO")) {
                    if (instance.getServer().getPluginManager().getPlugin("Vault") != null) {
                        RegisteredServiceProvider<Economy> rsp = instance.getServer().getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class);
                        net.milkbowl.vault.economy.Economy econ = rsp.getProvider();
                        if (econ.has(player, cost)) {
                            econ.withdrawPlayer(player, cost);
                            upgradeFinal(level, player);
                        } else {
                            player.sendMessage(instance.getLocale().getMessage("event.upgrade.cannotafford"));
                        }
                    } else {
                        player.sendMessage("Vault is not installed.");
                    }
                } else if (type.equals("XP")) {
                    if (player.getLevel() >= cost || player.getGameMode() == GameMode.CREATIVE) {
                        if (player.getGameMode() != GameMode.CREATIVE) {
                            player.setLevel(player.getLevel() - cost);
                        }
                        upgradeFinal(level, player);
                    } else {
                        player.sendMessage(instance.getLocale().getMessage("event.upgrade.cannotafford"));
                    }
                }
            }
        } catch (Exception ex) {
            Debugger.runReport(ex);
        }
    }

    public void upgradeFinal(Level level, Player player) {
        try {
            EpicHoppers instance = EpicHoppers.getInstance();
            this.level = level;
            if (instance.getLevelManager().getHighestLevel() != level) {
                player.sendMessage(instance.getLocale().getMessage("event.upgrade.success", level.getLevel()));
            } else {
                player.sendMessage(instance.getLocale().getMessage("event.upgrade.maxed", level.getLevel()));
            }
            Location loc = location.clone().add(.5, .5, .5);
            if (!instance.v1_8 && !instance.v1_7) {
                player.getWorld().spawnParticle(org.bukkit.Particle.valueOf(instance.getConfig().getString("Main.Upgrade Particle Type")), loc, 200, .5, .5, .5);
            } else {
                player.getWorld().playEffect(loc, org.bukkit.Effect.valueOf(instance.getConfig().getString("Main.Upgrade Particle Type")), 1, 0);
                //Doesn't resolve --Nova
                //player.getWorld().spigot().playEffect(loc, org.bukkit.Effect.valueOf(instance.getConfig().getString("Main.Upgrade Particle Type")), 1, 0, (float) 1, (float) 1, (float) 1, 1, 200, 10);
            }
            if (instance.getConfig().getBoolean("Main.Sounds Enabled")) {
                if (instance.getLevelManager().getHighestLevel() != level) {
                    if (!instance.v1_8 && !instance.v1_7) {
                        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 0.6F, 15.0F);
                    } else {
                        player.playSound(player.getLocation(), org.bukkit.Sound.valueOf("LEVEL_UP"), 2F, 15.0F);
                    }
                } else {
                    if (!instance.v1_10 && !instance.v1_9 && !instance.v1_8 && !instance.v1_7) {
                        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 2F, 25.0F);
                        player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_NOTE_CHIME, 2F, 25.0F);
                        Bukkit.getScheduler().scheduleSyncDelayedTask(instance, () -> player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_NOTE_CHIME, 1.2F, 35.0F), 5L);
                        Bukkit.getScheduler().scheduleSyncDelayedTask(instance, () -> player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_NOTE_CHIME, 1.8F, 35.0F), 10L);
                    } else {
                        player.playSound(player.getLocation(), org.bukkit.Sound.valueOf("LEVEL_UP"), 2F, 25.0F);
                    }
                }
            }
        } catch (Exception ex) {
            Debugger.runReport(ex);
        }
    }


    public void timeout(Player player) {
        try {
            EpicHoppers instance = EpicHoppers.getInstance();
            Bukkit.getScheduler().scheduleSyncDelayedTask(instance, () -> {
                if (instance.sync.containsKey(player)) {
                    player.sendMessage(instance.getLocale().getMessage("event.hopper.synctimeout"));
                    instance.sync.remove(player);
                    instance.bsync.remove(player);
                }
            }, instance.getConfig().getLong("Main.Timeout When Syncing Hoppers"));
        } catch (Exception e) {
            Debugger.runReport(e);
        }
    }

    public void sync(Block block2, boolean black, Player player) {
        try {
            EpicHoppers instance = EpicHoppers.getInstance();

            if (location.getWorld().equals(block2.getLocation().getWorld()) && !player.hasPermission("EpicHoppers.Override") && !player.hasPermission("EpicHoppers.Admin")) {
                if (location.distance(block2.getLocation()) > level.getRange() && !player.hasPermission("EpicHoppers.Override") && !player.hasPermission("EpicHoppers.Admin")) {
                    player.sendMessage(instance.getLocale().getMessage("event.hopper.syncoutofrange"));
                    return;
                }
            }
            player.sendMessage(instance.getLocale().getMessage("event.hopper.syncsuccess"));

            if (!black)
                this.syncedBlock = block2;
            else
                this.filter.setEndPoint(block2);
            this.lastPlayer = player.getUniqueId();
        } catch (Exception e) {
            Debugger.runReport(e);
        }
    }

    public Location getLocation() {
        return location.clone();
    }

    public Level getLevel() {
        return level;
    }

    public UUID getLastPlayer() {
        return lastPlayer;
    }

    public boolean isWalkOnTeleport() {
        return walkOnTeleport;
    }

    public void setWalkOnTeleport(boolean walkOnTeleport) {
        this.walkOnTeleport = walkOnTeleport;
    }

    public Block getSyncedBlock() {
        return syncedBlock;
    }

    public void setSyncedBlock(Block syncedBlock) {
        this.syncedBlock = syncedBlock;
    }

    public Filter getFilter() {
        return filter;
    }
}
