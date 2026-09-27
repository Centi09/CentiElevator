package de.centi.centielevator;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.DaylightDetector;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;

public class ElevatorListener implements Listener {

    private final CentiElevator plugin;
    private final ElevatorManager manager;

    public ElevatorListener(CentiElevator plugin, ElevatorManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerSneak(PlayerToggleSneakEvent event) {
        if (!event.isSneaking()) return;

        Player player = event.getPlayer();
        if (player.isFlying() || player.isInsideVehicle() || player.isGliding()) {
            return;
        }

        manager.triggerDown(player);
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null) return;

        double deltaY = to.getY() - from.getY();
        // Ein Sprung hat mindestens 0.1 Aufwaertsbewegung
        if (deltaY < 0.1) return;

        Player player = event.getPlayer();
        if (player.isFlying() || player.isInsideVehicle() || player.isGliding()) {
            return;
        }

        // Pruefen ob der Absprungpunkt auf einem Aufzug war
        Block elevator = manager.getStandingElevator(player);
        if (elevator != null) {
            manager.triggerUp(player);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (!manager.isToggleableByInvert()) return;

        Block clicked = event.getClickedBlock();
        if (clicked == null) return;
        if (!manager.isElevatorBlock(clicked)) return;

        // Wenn es ein Tageslichtsensor ist, aendert Minecraft beim Rechtsklick den Modus
        if (clicked.getBlockData() instanceof DaylightDetector) {
            Player player = event.getPlayer();
            // 1 Tick spaeter pruefen welcher Modus jetzt aktiv ist
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (clicked.getBlockData() instanceof DaylightDetector currentDetector) {
                    manager.notifyToggle(player, currentDetector.isInverted());
                }
            }, 1L);
        }
    }
}
