package lat.frikicraft.frikitombs;

import org.bukkit.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;

public class FrikiTombs extends JavaPlugin {

    private static FrikiTombs instance;
    private GraveManager graveManager;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        this.graveManager = new GraveManager(this);
        this.graveManager.loadGraves();

        getServer().getPluginManager().registerEvents(new GraveListener(this), this);
        if (getCommand("frikitombs") != null) {
            getCommand("frikitombs").setExecutor(new TombCommand(this));
        }

        getLogger().info("FrikiTombs v1.0.6 ha sido activado exitosamente.");
    }

    @Override
    public void onDisable() {
        if (graveManager != null) {
            graveManager.saveGraves();
        }
    }

    public static FrikiTombs getInstance() {
        return instance;
    }

    public GraveManager getGraveManager() {
        return graveManager;
    }

    public String getMessage(String path) {
        String msg = getConfig().getString("messages." + path, "");
        String prefix = getConfig().getString("messages.prefix", "");
        return ChatColor.translateAlternateColorCodes('&', prefix + msg);
    }
}
