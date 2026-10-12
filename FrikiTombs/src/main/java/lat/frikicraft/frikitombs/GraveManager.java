package lat.frikicraft.frikitombs;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class GraveManager {

    private final FrikiTombs plugin;
    private final List<GraveData> activeGraves = new ArrayList<>();
    private final File gravesFile;
    private YamlConfiguration gravesConfig;

    public GraveManager(FrikiTombs plugin) {
        this.plugin = plugin;
        this.gravesFile = new File(plugin.getDataFolder(), "graves_data.yml");
    }

    public boolean isWorldAllowed(World world) {
        if (world == null) return false;
        String mode = plugin.getConfig().getString("worlds.mode", "ALL").toUpperCase();
        List<String> allowlist = plugin.getConfig().getStringList("worlds.allowlist");
        List<String> blocklist = plugin.getConfig().getStringList("worlds.blocklist");

        switch (mode) {
            case "ALLOWLIST":
                return allowlist.contains(world.getName());
            case "BLOCKLIST":
                return !blocklist.contains(world.getName());
            case "ALL":
            default:
                return true;
        }
    }

    public void addGrave(GraveData grave) {
        activeGraves.add(grave);
        saveGraves();
    }

    public void removeGrave(GraveData grave) {
        activeGraves.remove(grave);
        grave.removeBlock();
        saveGraves();
    }

    public GraveData getGraveAt(Location loc) {
        for (GraveData grave : activeGraves) {
            if (grave.getLocation().getBlockX() == loc.getBlockX() &&
                grave.getLocation().getBlockY() == loc.getBlockY() &&
                grave.getLocation().getBlockZ() == loc.getBlockZ() &&
                Objects.equals(grave.getLocation().getWorld(), loc.getWorld())) {
                return grave;
            }
        }
        return null;
    }

    public long parseTimeToMillis(String input) {
        if (input == null || input.isEmpty()) return 0;
        long total = 0;
        String number = "";
        for (char c : input.toCharArray()) {
            if (Character.isDigit(c)) {
                number += c;
            } else {
                if (!number.isEmpty()) {
                    long val = Long.parseLong(number);
                    switch (c) {
                        case 's': total += val * 1000; break;
                        case 'm': total += val * 60 * 1000; break;
                        case 'h': total += val * 3600 * 1000; break;
                        case 'd': total += val * 86400 * 1000; break;
                    }
                    number = "";
                }
            }
        }
        return total;
    }

    public String formatTime(long millis) {
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        
        if (hours > 0) return hours + "h " + (minutes % 60) + "m";
        if (minutes > 0) return minutes + "m " + (seconds % 60) + "s";
        return seconds + "s";
    }

    public void saveGraves() {
        gravesConfig = new YamlConfiguration();
        int i = 0;
        for (GraveData grave : activeGraves) {
            gravesConfig.set("graves." + i, grave.serialize());
            i++;
        }
        try {
            gravesConfig.save(gravesFile);
        } catch (IOException e) {
            plugin.getLogger().severe("No se pudieron guardar las tumbas en graves_data.yml");
        }
    }

    public void loadGraves() {
        if (!gravesFile.exists()) return;
        gravesConfig = YamlConfiguration.loadConfiguration(gravesFile);
        activeGraves.clear();

        if (gravesConfig.contains("graves")) {
            for (String key : gravesConfig.getConfigurationSection("graves").getKeys(false)) {
                GraveData grave = GraveData.deserialize(gravesConfig.getConfigurationSection("graves." + key));
                if (grave != null) {
                    activeGraves.add(grave);
                }
            }
        }
    }
}
