package lat.frikicraft.frikitombs;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class GraveListener implements Listener {

    private final FrikiTombs plugin;

    public GraveListener(FrikiTombs plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();

        if (!plugin.getGraveManager().isWorldAllowed(player.getWorld())) return;

        if (event.getKeepInventory() && !plugin.getConfig().getBoolean("settings.create-with-keep-inventory", false)) {
            return;
        }

        Location loc = findSafeLocation(player.getLocation());
        if (loc == null) {
            player.sendMessage(plugin.getMessage("no-safe-location"));
            return;
        }

        Map<Integer, ItemStack> items = new HashMap<>();
        ItemStack[] contents = player.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack is = contents[i];
            if (is != null && is.getType() != Material.AIR) {
                items.put(i, is.clone());
            }
        }

        event.getDrops().clear();

        int expToStore = 0;
        if (plugin.getConfig().getBoolean("settings.store-experience", true)) {
            expToStore = event.getDroppedExp();
            event.setDroppedExp(0);
        }

        GraveData grave = new GraveData(player.getUniqueId(), player.getName(), loc, items, expToStore, System.currentTimeMillis());
        grave.createBlock();
        plugin.getGraveManager().addGrave(grave);

        String protectionStr = plugin.getGraveManager().formatTime(
                plugin.getGraveManager().parseTimeToMillis(plugin.getConfig().getString("settings.protection", "10m"))
        );

        String msg = plugin.getMessage("grave-created")
                .replace("%world%", loc.getWorld().getName())
                .replace("%x%", String.valueOf(loc.getBlockX()))
                .replace("%y%", String.valueOf(loc.getBlockY()))
                .replace("%z%", String.valueOf(loc.getBlockZ()))
                .replace("%protection%", protectionStr);
        player.sendMessage(msg);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onGraveInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.LEFT_CLICK_BLOCK) return;
        if (event.getHand() != EquipmentSlot.HAND) return;

        Block block = event.getClickedBlock();
        if (block == null) return;

        GraveData grave = plugin.getGraveManager().getGraveAt(block.getLocation());
        if (grave == null) return;

        Player player = event.getPlayer();

        if (!player.isSneaking()) {
            return;
        }

        event.setCancelled(true);

        boolean isAdmin = player.isOp() || player.hasPermission("frikitombs.admin") || player.hasPermission("frikitombs.bypass");
        boolean isOwner = player.getUniqueId().equals(grave.getOwnerUUID());

        if (!isOwner && !isAdmin) {
            if (grave.isProtected()) {
                String remaining = plugin.getGraveManager().formatTime(grave.getRemainingProtectionTime());
                player.sendMessage(plugin.getMessage("protected").replace("%time%", remaining));
                return;
            }
        }

        if (grave.getStoredXp() > 0) {
            if (isOwner) {
                player.giveExp(grave.getStoredXp());
            } else {
                org.bukkit.entity.ExperienceOrb orb = block.getWorld().spawn(block.getLocation(), org.bukkit.entity.ExperienceOrb.class);
                orb.setExperience(grave.getStoredXp());
            }
        }

        if (grave.getItems() != null) {
            if (isOwner) {
                for (Map.Entry<Integer, ItemStack> entry : grave.getItems().entrySet()) {
                    int slot = entry.getKey();
                    ItemStack item = entry.getValue();
                    
                    if (item != null && item.getType() != Material.AIR) {
                        ItemStack currentItem = player.getInventory().getItem(slot);
                        
                        if (currentItem == null || currentItem.getType() == Material.AIR) {
                            player.getInventory().setItem(slot, item);
                        } else {
                            HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(item);
                            for (ItemStack drop : leftover.values()) {
                                block.getWorld().dropItemNaturally(block.getLocation(), drop);
                            }
                        }
                    }
                }
            } else {
                for (ItemStack item : grave.getItems().values()) {
                    if (item != null && item.getType() != Material.AIR) {
                        block.getWorld().dropItemNaturally(block.getLocation(), item);
                    }
                }
            }
        }

        plugin.getGraveManager().removeGrave(grave);

        if (isOwner) {
            player.sendMessage(plugin.getMessage("opened"));
        } else {
            player.sendMessage(plugin.getMessage("opened-other").replace("%owner%", grave.getOwnerName()));
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onGraveBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        GraveData grave = plugin.getGraveManager().getGraveAt(block.getLocation());
        if (grave == null) return;

        event.setCancelled(true);
        Player player = event.getPlayer();
        if (!player.isSneaking()) {
            player.sendMessage(plugin.getMessage("cant-open-gui"));
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityExplode(EntityExplodeEvent event) {
        Iterator<Block> iterator = event.blockList().iterator();
        while (iterator.hasNext()) {
            Block block = iterator.next();
            if (plugin.getGraveManager().getGraveAt(block.getLocation()) != null) {
                iterator.remove();
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockExplode(BlockExplodeEvent event) {
        Iterator<Block> iterator = event.blockList().iterator();
        while (iterator.hasNext()) {
            Block block = iterator.next();
            if (plugin.getGraveManager().getGraveAt(block.getLocation()) != null) {
                iterator.remove();
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        for (Block block : event.getBlocks()) {
            if (plugin.getGraveManager().getGraveAt(block.getLocation()) != null) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        for (Block block : event.getBlocks()) {
            if (plugin.getGraveManager().getGraveAt(block.getLocation()) != null) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBucketEmpty(PlayerBucketEmptyEvent event) {
        Block block = event.getBlockClicked().getRelative(event.getBlockFace());
        if (plugin.getGraveManager().getGraveAt(block.getLocation()) != null ||
            plugin.getGraveManager().getGraveAt(event.getBlockClicked().getLocation()) != null) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockFromTo(BlockFromToEvent event) {
        if (plugin.getGraveManager().getGraveAt(event.getToBlock().getLocation()) != null) {
            event.setCancelled(true);
        }
    }

    private Location findSafeLocation(Location start) {
        Block b = start.getBlock();
        if (isUnsafeFluid(b.getType()) || b.isLiquid()) {
            for (int yOffset = 0; yOffset <= 15; yOffset++) {
                Block upwardBlock = start.clone().add(0, yOffset, 0).getBlock();
                if (!isUnsafeFluid(upwardBlock.getType()) && !upwardBlock.isLiquid() && 
                    (upwardBlock.getType() == Material.AIR || upwardBlock.getType() == Material.CAVE_AIR)) {
                    return upwardBlock.getLocation();
                }
            }
        }

        if (b.getType() == Material.AIR || b.getType() == Material.CAVE_AIR) {
            return b.getLocation();
        }

        int radius = plugin.getConfig().getInt("settings.placement-radius", 3);
        if (start.getY() < start.getWorld().getMinHeight() && plugin.getConfig().getBoolean("worlds.ignore-void-worlds", true)) {
            return null;
        }

        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    Block rel = b.getRelative(x, y, z);
                    if (!isUnsafeFluid(rel.getType()) && !rel.isLiquid() && 
                        (rel.getType() == Material.AIR || rel.getType() == Material.CAVE_AIR)) {
                        return rel.getLocation();
                    }
                }
            }
        }
        
        return start;
    }

    private boolean isUnsafeFluid(Material mat) {
        return mat == Material.WATER || mat == Material.LAVA || mat == Material.FIRE || mat == Material.CAMPFIRE || mat == Material.SOUL_FIRE;
    }
}
