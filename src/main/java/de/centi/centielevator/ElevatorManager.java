package de.centi.centielevator;

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.DaylightDetector;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class ElevatorManager {

    private final CentiElevator plugin;
    private final Set<Material> elevatorMaterials = new HashSet<>();
    private final Map<UUID, Long> cooldowns = new HashMap<>();

    private boolean toggleableByInvert;
    private int maxDistance;
    private int minDistance;
    private long cooldownMs;

    private boolean soundEnabled;
    private Sound soundType;
    private float soundVolume;
    private float soundPitch;

    private boolean particlesEnabled;
    private Particle particleType;
    private int particleCount;

    public ElevatorManager(CentiElevator plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        elevatorMaterials.clear();
        List<String> list = plugin.getConfig().getStringList("elevator-blocks");
        for (String name : list) {
            Material mat = Material.matchMaterial(name);
            if (mat != null) {
                elevatorMaterials.add(mat);
            } else {
                plugin.getLogger().warning("Unbekanntes Material in config.yml: " + name);
            }
        }
        if (elevatorMaterials.isEmpty()) {
            elevatorMaterials.add(Material.DAYLIGHT_DETECTOR);
        }

        this.toggleableByInvert = plugin.getConfig().getBoolean("toggleable-by-invert", true);
        this.maxDistance = plugin.getConfig().getInt("max-distance", 64);
        this.minDistance = plugin.getConfig().getInt("min-distance", 2);
        this.cooldownMs = plugin.getConfig().getLong("cooldown-ms", 350);

        this.soundEnabled = plugin.getConfig().getBoolean("sound.enabled", true);
        String sName = plugin.getConfig().getString("sound.type", "ENTITY_ENDERMAN_TELEPORT");
        try {
            this.soundType = Sound.valueOf(sName);
        } catch (IllegalArgumentException e) {
            this.soundType = Sound.ENTITY_ENDERMAN_TELEPORT;
        }
        this.soundVolume = (float) plugin.getConfig().getDouble("sound.volume", 0.8);
        this.soundPitch = (float) plugin.getConfig().getDouble("sound.pitch", 1.4);

        this.particlesEnabled = plugin.getConfig().getBoolean("particles.enabled", true);
        String pName = plugin.getConfig().getString("particles.type", "PORTAL");
        try {
            this.particleType = Particle.valueOf(pName);
        } catch (IllegalArgumentException e) {
            this.particleType = Particle.PORTAL;
        }
        this.particleCount = plugin.getConfig().getInt("particles.count", 20);
    }

    public boolean isElevatorBlock(Block block) {
        return block != null && elevatorMaterials.contains(block.getType());
    }

    // Prueft ob ein Aufzug deaktiviert ist (grauer Nacht-Modus beim Tageslichtsensor)
    public boolean isElevatorDisabled(Block block) {
        if (block == null) return true;
        if (!isElevatorBlock(block)) return true;

        if (toggleableByInvert && block.getBlockData() instanceof DaylightDetector detector) {
            return detector.isInverted(); // true = grau/invertiert = AUSSER BETRIEB!
        }
        return false;
    }

    public Block getStandingElevator(Player player) {
        Location loc = player.getLocation();

        // 1. Direkt unter den Fuessen (fuer Tageslichtsensoren: 0.375 Hoehe)
        Block b1 = loc.clone().subtract(0, 0.2, 0).getBlock();
        if (isElevatorBlock(b1)) return b1;

        // 2. Auf Blockposition
        Block b2 = loc.getBlock();
        if (isElevatorBlock(b2)) return b2;

        // 3. Etwas tiefer pruefen (fuer Halbstufen/Teppiche)
        Block b3 = loc.clone().subtract(0, 0.5, 0).getBlock();
        if (isElevatorBlock(b3)) return b3;

        return null;
    }

    public boolean canUse(Player player) {
        if (!player.hasPermission("centielevator.use")) {
            return false;
        }
        long now = System.currentTimeMillis();
        long last = cooldowns.getOrDefault(player.getUniqueId(), 0L);
        return (now - last) >= cooldownMs;
    }

    public void setCooldown(Player player) {
        cooldowns.put(player.getUniqueId(), System.currentTimeMillis());
    }

    public void triggerUp(Player player) {
        Block current = getStandingElevator(player);
        if (current == null) return;
        if (!canUse(player)) return;

        // Wenn die aktuelle Etage deaktiviert (grau) ist:
        if (isElevatorDisabled(current)) {
            sendDisabledMessage(player);
            setCooldown(player);
            return;
        }

        List<Block> column = getElevatorColumn(current);
        Block target = null;
        for (Block b : column) {
            // Nur Etagen darueber UND nur aktive Etagen (graue ueberspringen!)
            if (b.getY() >= current.getY() + minDistance && !isElevatorDisabled(b)) {
                target = b;
                break;
            }
        }

        if (target == null) return; // Keine aktive Etage darueber

        if (!isSafeLanding(target)) {
            sendBlockedMessage(player);
            return;
        }

        int targetFloor = column.indexOf(target) + 1;
        int totalFloors = column.size();

        teleportToElevator(player, current, target);
        setCooldown(player);

        String format = plugin.getConfig().getString("messages.floor-up", "&8[&5▲&8] &7Etage &d{floor}&8/&f{total}");
        sendFloorActionBar(player, format, targetFloor, totalFloors);
    }

    public void triggerDown(Player player) {
        Block current = getStandingElevator(player);
        if (current == null) return;
        if (!canUse(player)) return;

        // Wenn die aktuelle Etage deaktiviert (grau) ist:
        if (isElevatorDisabled(current)) {
            sendDisabledMessage(player);
            setCooldown(player);
            return;
        }

        List<Block> column = getElevatorColumn(current);
        Block target = null;
        for (int i = column.size() - 1; i >= 0; i--) {
            Block b = column.get(i);
            // Nur Etagen darunter UND nur aktive Etagen (graue ueberspringen!)
            if (b.getY() <= current.getY() - minDistance && !isElevatorDisabled(b)) {
                target = b;
                break;
            }
        }

        if (target == null) return; // Keine aktive Etage darunter

        if (!isSafeLanding(target)) {
            sendBlockedMessage(player);
            return;
        }

        int targetFloor = column.indexOf(target) + 1;
        int totalFloors = column.size();

        teleportToElevator(player, current, target);
        setCooldown(player);

        String format = plugin.getConfig().getString("messages.floor-down", "&8[&5▼&8] &7Etage &d{floor}&8/&f{total}");
        sendFloorActionBar(player, format, targetFloor, totalFloors);
    }

    public void notifyToggle(Player player, boolean isNowInverted) {
        if (!toggleableByInvert) return;
        String key = isNowInverted ? "messages.toggled-off" : "messages.toggled-on";
        String def = isNowInverted
                ? "&8[&c✕&8] &cEtage deaktiviert &7(Ausser Betrieb)"
                : "&8[&d✓&8] &dEtage aktiviert &7(Aufzug bereit)";
        String msg = plugin.getConfig().getString(key, def);
        player.sendActionBar(LegacyComponentSerializer.legacyAmpersand().deserialize(msg));

        // Kleiner Sound-Klick
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.6f, isNowInverted ? 0.8f : 1.2f);
    }

    private List<Block> getElevatorColumn(Block center) {
        List<Block> list = new ArrayList<>();
        World world = center.getWorld();
        int x = center.getX();
        int z = center.getZ();

        int minY = Math.max(world.getMinHeight(), center.getY() - maxDistance);
        int maxY = Math.min(world.getMaxHeight(), center.getY() + maxDistance);

        for (int y = minY; y <= maxY; y++) {
            Block b = world.getBlockAt(x, y, z);
            if (isElevatorBlock(b)) {
                list.add(b);
            }
        }
        return list;
    }

    private boolean isSafeLanding(Block elevator) {
        Block feet = elevator.getRelative(BlockFace.UP, 1);
        Block head = elevator.getRelative(BlockFace.UP, 2);
        return feet.isPassable() && head.isPassable();
    }

    private void teleportToElevator(Player player, Block from, Block to) {
        Location startLoc = player.getLocation();

        // Zielhoehe ermitteln (Tageslichtsensor ist 0.375 hoch, Vollbloecke 1.0)
        double yOffset = to.getType() == Material.DAYLIGHT_DETECTOR ? 0.375 : 1.0;
        Location targetLoc = new Location(
                to.getWorld(),
                to.getX() + 0.5,
                to.getY() + yOffset,
                to.getZ() + 0.5,
                player.getLocation().getYaw(),
                player.getLocation().getPitch()
        );

        // Effekte am Start
        playEffects(startLoc);

        // Teleport
        player.teleport(targetLoc);

        // Effekte am Ziel
        playEffects(targetLoc);
    }

    private void playEffects(Location loc) {
        if (soundEnabled && soundType != null) {
            loc.getWorld().playSound(loc, soundType, soundVolume, soundPitch);
        }
        if (particlesEnabled && particleType != null) {
            loc.getWorld().spawnParticle(particleType, loc.clone().add(0, 0.5, 0), particleCount, 0.25, 0.4, 0.25, 0.02);
        }
    }

    private void sendFloorActionBar(Player player, String format, int floor, int total) {
        String rendered = format
                .replace("{floor}", String.valueOf(floor))
                .replace("{total}", String.valueOf(total));
        player.sendActionBar(LegacyComponentSerializer.legacyAmpersand().deserialize(rendered));
    }

    private void sendBlockedMessage(Player player) {
        String msg = plugin.getConfig().getString("messages.blocked", "&cKein sicherer Platz auf dieser Etage!");
        player.sendActionBar(LegacyComponentSerializer.legacyAmpersand().deserialize(msg));
    }

    private void sendDisabledMessage(Player player) {
        String msg = plugin.getConfig().getString("messages.disabled", "&8[&c✕&8] &cDiese Etage ist ausser Betrieb!");
        player.sendActionBar(LegacyComponentSerializer.legacyAmpersand().deserialize(msg));
    }

    public boolean isToggleableByInvert() {
        return toggleableByInvert;
    }
}
