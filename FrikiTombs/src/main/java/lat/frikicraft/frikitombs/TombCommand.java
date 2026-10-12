package lat.frikicraft.frikitombs;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class TombCommand implements CommandExecutor {

    private final FrikiTombs plugin;

    public TombCommand(FrikiTombs plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("frikitombs.admin")) {
                sender.sendMessage(plugin.getMessage("no-permission"));
                return true;
            }
            plugin.reloadConfig();
            plugin.getGraveManager().loadGraves();
            sender.sendMessage(plugin.getMessage("reloaded"));
            return true;
        }
        sender.sendMessage("§dFrikiTombs §7- Usa §f/ftombs reload §7para recargar la configuración.");
        return true;
    }
}
