package lat.frikicraft.frikitombs;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class GraveData {

    private final UUID ownerUUID;
    private final String ownerName;
    private final Location location;
    private final Map<Integer, ItemStack> items;
    private final int storedXp;
    private final long creationTime;

    public GraveData(UUID ownerUUID, String ownerName, Location location, Map<Integer, ItemStack> items, int storedXp, long creationTime) {
        this.ownerUUID = ownerUUID;
        this.ownerName = ownerName;
        this.location = location;
        this.items = items != null ? items : new HashMap<>();
        this.storedXp = storedXp;
        this.creationTime = creationTime;
    }

    public void createBlock() {
        Block block = location.getBlock();
        block.setType(Material.HEAVY_CORE);
    }

    public void removeBlock() {
        if (location.getBlock().getType() == Material.HEAVY_CORE) {
            location.getBlock().setType(Material.AIR);
        }
    }

    public boolean isExpired() {
        long lifetime = FrikiTombs.getInstance().getGraveManager().parseTimeToMillis(
                FrikiTombs.getInstance().getConfig().getString("settings.lifetime", "24h")
        );
        return System.currentTimeMillis() - creationTime >= lifetime;
    }

    public boolean isProtected() {
        long protectionTime = FrikiTombs.getInstance().getGraveManager().parseTimeToMillis(
                FrikiTombs.getInstance().getConfig().getString("settings.protection", "10m")
        );
        return System.currentTimeMillis() - creationTime < protectionTime;
    }

    public long getRemainingProtectionTime() {
        long protectionTime = FrikiTombs.getInstance().getGraveManager().parseTimeToMillis(
                FrikiTombs.getInstance().getConfig().getString("settings.protection", "10m")
        );
        long elapsed = System.currentTimeMillis() - creationTime;
        return Math.max(0, protectionTime - elapsed);
    }

    public Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();
        map.put("ownerUUID", ownerUUID.toString());
        map.put("ownerName", ownerName);
        map.put("location", location);
        
        Map<String, ItemStack> serializedItems = new HashMap<>();
        for (Map.Entry<Integer, ItemStack> entry : items.entrySet()) {
            serializedItems.put(String.valueOf(entry.getKey()), entry.getValue());
        }
        map.put("items", serializedItems);
        
        map.put("storedXp", storedXp);
        map.put("creationTime", creationTime);
        return map;
    }

    @SuppressWarnings("unchecked")
    public static GraveData deserialize(ConfigurationSection section) {
        if (section == null) return null;
        UUID ownerUUID = UUID.fromString(section.getString("ownerUUID"));
        String ownerName = section.getString("ownerName");
        Location location = (Location) section.get("location");
        
        Map<Integer, ItemStack> items = new HashMap<>();
        ConfigurationSection itemsSection = section.getConfigurationSection("items");
        if (itemsSection != null) {
            for (String key : itemsSection.getKeys(false)) {
                try {
                    int slot = Integer.parseInt(key);
                    ItemStack item = itemsSection.getItemStack(key);
                    if (item != null) {
                        items.put(slot, item);
                    }
                } catch (NumberFormatException ignored) {}
            }
        } else {
            List<ItemStack> oldList = (List<ItemStack>) section.getList("items");
            if (oldList != null) {
                for (int i = 0; i < oldList.size(); i++) {
                    if (oldList.get(i) != null) {
                        items.put(i, oldList.get(i));
                    }
                }
            }
        }

        int storedXp = section.getInt("storedXp");
        long creationTime = section.getLong("creationTime");

        return new GraveData(ownerUUID, ownerName, location, items, storedXp, creationTime);
    }

    public UUID getOwnerUUID() { return ownerUUID; }
    public String getOwnerName() { return ownerName; }
    public Location getLocation() { return location; }
    public Map<Integer, ItemStack> getItems() { return items; }
    public int getStoredXp() { return storedXp; }
}
