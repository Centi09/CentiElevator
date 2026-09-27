package de.centi.centielevator;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class CentiElevator extends JavaPlugin {

    private static CentiElevator instance;
    private ElevatorManager elevatorManager;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();

        this.elevatorManager = new ElevatorManager(this);

        ElevatorListener listener = new ElevatorListener(this, elevatorManager);
        Bukkit.getPluginManager().registerEvents(listener, this);

        ElevatorCommand command = new ElevatorCommand(this);
        if (getCommand("centielevator") != null) {
            getCommand("centielevator").setExecutor(command);
            getCommand("centielevator").setTabCompleter(command);
        }

        getLogger().info("CentiElevator 1.0.0 wurde erfolgreich aktiviert.");
    }

    @Override
    public void onDisable() {
        getLogger().info("CentiElevator wurde deaktiviert.");
    }

    public void reloadPlugin() {
        reloadConfig();
        elevatorManager.reload();
    }

    public static CentiElevator getInstance() {
        return instance;
    }

    public ElevatorManager getElevatorManager() {
        return elevatorManager;
    }
}
