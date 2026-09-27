package de.centi.centielevator;

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.List;

public class ElevatorCommand implements CommandExecutor, TabCompleter {

    private final CentiElevator plugin;

    public ElevatorCommand(CentiElevator plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission("centielevator.admin")) {
            String noPerm = plugin.getConfig().getString("messages.no-permission", "&cKeine Berechtigung!");
            sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(noPerm));
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            plugin.reloadPlugin();
            String reloaded = plugin.getConfig().getString("messages.reloaded",
                    "&8[&5CentiElevator&8] &dKonfiguration wurde neu geladen.");
            sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(reloaded));
            return true;
        }

        sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(
                "&8[&5CentiElevator&8] &7Version &f" + plugin.getPluginMeta().getVersion() + " &8| &d/ce reload"));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        List<String> list = new ArrayList<>();
        if (args.length == 1 && sender.hasPermission("centielevator.admin")) {
            if ("reload".startsWith(args[0].toLowerCase())) {
                list.add("reload");
            }
        }
        return list;
    }
}
