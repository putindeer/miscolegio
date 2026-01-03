package me.putindeer.miscolegio.zone;

import lombok.Getter;
import me.putindeer.miscolegio.Main;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

import java.util.UUID;

public class ZoneManager implements Listener {
    private final Main plugin;
    @Getter
    private GameZone zone;
    @Getter
    public Location bathroomLocation;
    private SelectionSession session;
    private UUID bathroomSession;

    public ZoneManager(Main plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        loadZone();
        loadBathroom();   
    }

    public void startSelection(Player player) {
        if (session != null && !session.getPlayerId().equals(player.getUniqueId())) {
            plugin.utils.message(player,
                    "<red>¡Ya hay una sesión activa de <yellow>" + session.getPlayerName() + "</yellow>!",
                    "<yellow>Usa <white>/zone override</white> para reemplazarla");
            return;
        }

        session = new SelectionSession(player);

        plugin.utils.message(player,
                "<green>═══════════════════════════════",
                "<gold><bold>CONFIGURACIÓN DE ZONA DE JUEGO",
                "<green>═══════════════════════════════",
                "",
                "<yellow>Orden de configuración:",
                "<gray>1. <aqua>Respuesta A",
                "<gray>2. <aqua>Respuesta B",
                "<gray>3. <aqua>Respuesta C",
                "<gray>4. <aqua>Respuesta D",
                "<gray>5. <aqua>Área de Juego",
                "",
                "<green>Comenzando: <yellow>Respuesta A",
                "<yellow>Click izquierdo <gray>→ Posición 1",
                "<yellow>Click derecho <gray>→ Posición 2");
    }

    public void overrideSession(Player player) {
        if (session == null) {
            plugin.utils.message(player, "<red>No hay ninguna sesión activa para reemplazar.");
            return;
        }

        String previousPlayer = session.getPlayerName();
        session = new SelectionSession(player);

        plugin.utils.message(player,
                "<yellow>Sesión de <white>" + previousPlayer + " <yellow>reemplazada.",
                "<green>Comenzando: <yellow>Respuesta A");
    }

    public void cancelSelection(Player player) {
        if (session == null) {
            plugin.utils.message(player, "<red>No hay ninguna sesión activa.");
            return;
        }

        if (!session.getPlayerId().equals(player.getUniqueId())) {
            plugin.utils.message(player, "<red>No puedes cancelar la sesión de otro jugador.");
            return;
        }

        session = null;
        plugin.utils.message(player, "<yellow>Selección cancelada.");
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (session == null) return;

        Player player = event.getPlayer();
        if (!session.getPlayerId().equals(player.getUniqueId())) return;
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getClickedBlock() == null) return;

        Location clickedLoc = event.getClickedBlock().getLocation();
        ZoneType currentType = session.getCurrentZoneType();
        if (currentType == null) return;

        if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
            event.setCancelled(true);
            session.setPosition1(clickedLoc);

            plugin.utils.message(player,
                    "<green>✓ Posición 1 <gray>[" + currentType.getDisplayName() + "]",
                    "<white>" + formatLocation(clickedLoc));

            if (session.getPos2() != null) {
                showVolumeInfo(player);
            }
        }
        else if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            event.setCancelled(true);
            session.setPosition2(clickedLoc);

            plugin.utils.message(player,
                    "<green>✓ Posición 2 <gray>[" + currentType.getDisplayName() + "]",
                    "<white>" + formatLocation(clickedLoc));

            if (session.getPos1() != null) {
                showVolumeInfo(player);
                plugin.utils.message(player,
                        "<green>✓ Listo!",
                        "<yellow>Usa <white>/zone confirm <yellow>para continuar");
            }
        }
    }

    private void showVolumeInfo(Player player) {
        if (session.doesntHasPositions()) return;

        Cuboid area = session.createCuboid(session.getPos1(), session.getPos2());
        plugin.utils.message(player, "<gray>Área: <white>" + area.getArea() + " bloques²");
    }

    public void confirmAndContinue(Player player) {
        if (session == null) {
            plugin.utils.message(player, "<red>No hay ninguna sesión activa.");
            return;
        }

        if (!session.getPlayerId().equals(player.getUniqueId())) {
            plugin.utils.message(player, "<red>No puedes confirmar la sesión de otro jugador.");
            return;
        }

        if (session.doesntHasPositions()) {
            plugin.utils.message(player, "<red>Debes seleccionar ambas posiciones primero.");
            return;
        }

        session.confirmCurrentZone();

        if (session.isComplete()) {
            finalizeZone(player);
            return;
        }

        ZoneType nextType = session.getCurrentZoneType();
        plugin.utils.message(player,
                "<green>═══════════════════════════════",
                "<green>Siguiente: <yellow>" + nextType.getDisplayName(),
                "<yellow>Click izquierdo <gray>→ Posición 1",
                "<yellow>Click derecho <gray>→ Posición 2");

    }

    private void finalizeZone(Player player) {
        GameZone newZone = new GameZone();
        newZone.setAnswerA(session.getAnswerA());
        newZone.setAnswerB(session.getAnswerB());
        newZone.setAnswerC(session.getAnswerC());
        newZone.setAnswerD(session.getAnswerD());
        newZone.setPlayArea(session.getPlayArea());

        saveZone(newZone);
        zone = newZone;

        plugin.utils.message(player,
                "<green>═══════════════════════════════",
                "<gold><bold>✓ ZONA COMPLETADA",
                "<green>═══════════════════════════════",
                "",
                "<green>¡La zona está lista para usar!");

        session = null;
    }

    private void saveZone(GameZone gameZone) {
        String path = "zone";

        if (gameZone.getAnswerA() != null) {
            gameZone.getAnswerA().saveToConfig(plugin.getConfig().createSection(path + ".answer-a"));
        }
        if (gameZone.getAnswerB() != null) {
            gameZone.getAnswerB().saveToConfig(plugin.getConfig().createSection(path + ".answer-b"));
        }
        if (gameZone.getAnswerC() != null) {
            gameZone.getAnswerC().saveToConfig(plugin.getConfig().createSection(path + ".answer-c"));
        }
        if (gameZone.getAnswerD() != null) {
            gameZone.getAnswerD().saveToConfig(plugin.getConfig().createSection(path + ".answer-d"));
        }
        if (gameZone.getPlayArea() != null) {
            gameZone.getPlayArea().saveToConfig(plugin.getConfig().createSection(path + ".play-area"));
        }

        plugin.saveConfig();
    }

    private void loadZone() {
        zone = null;

        ConfigurationSection section = plugin.getConfig().getConfigurationSection("zone");
        if (section == null) return;

        String worldName = section.getString("answer-a.world");
        if (worldName == null) return;

        World world = plugin.getServer().getWorld(worldName);
        if (world == null) return;

        GameZone newZone = new GameZone();
        newZone.setAnswerA(Cuboid.fromConfig(section.getConfigurationSection("answer-a"), world));
        newZone.setAnswerB(Cuboid.fromConfig(section.getConfigurationSection("answer-b"), world));
        newZone.setAnswerC(Cuboid.fromConfig(section.getConfigurationSection("answer-c"), world));
        newZone.setAnswerD(Cuboid.fromConfig(section.getConfigurationSection("answer-d"), world));
        newZone.setPlayArea(Cuboid.fromConfig(section.getConfigurationSection("play-area"), world));

        if (newZone.isComplete()) {
            zone = newZone;
            plugin.utils.log("Zona de juego cargada correctamente.");
        } else {
            plugin.utils.severe("La zona de juego está incompleta en el config.");
        }
    }

    public void reloadZone() {
        loadZone();
    }

    public boolean hasActiveSession() {
        return session != null;
    }

    public String getActiveSessionPlayer() {
        return session != null ? session.getPlayerName() : null;
    }

    private String formatLocation(Location location) {
        return String.format("(%d, %d, %d)", location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }

    public void startBathroomSession(Player player) {
        if (bathroomSession != null) return;
        bathroomSession = player.getUniqueId();
        plugin.utils.message(player, "<yellow>Has iniciado una sesión de configuración de la zona del baño.",
                "<yellow>Utiliza '/zone bañoconfirm' una vez estés en el lugar al que quieres que teletransporte.");
    }

    public boolean isInBathroomSession(Player player) {
        return bathroomSession != null && bathroomSession.equals(player.getUniqueId());
    }

    public void confirmBathroomLocation(Location location) {
        bathroomLocation = location.clone();

        String path = "game.bathroom";
        ConfigurationSection section = plugin.getConfig().getConfigurationSection(path);
        if (section == null) {
            section = plugin.getConfig().createSection("game.bathroom");
        }

        section.set("world", location.getWorld().getName());
        section.set("x", location.getX());
        section.set("y", location.getY());
        section.set("z", location.getZ());
        section.set("yaw", location.getYaw());
        section.set("pitch", location.getPitch());

        plugin.saveConfig();
        bathroomSession = null;
    }

    private void loadBathroom() {
        if (bathroomLocation != null) return;

        ConfigurationSection section = plugin.getConfig().getConfigurationSection("game.bathroom");
        if (section == null) return;

        String worldName = section.getString("world");
        if (worldName == null) return;

        World world = plugin.getServer().getWorld(worldName);
        if (world == null) return;

        bathroomLocation = new Location(world,
                section.getDouble("x"),
                section.getDouble("y"),
                section.getDouble("z"),
                (float) section.getDouble("yaw"),
                (float) section.getDouble("pitch")
        );

        plugin.utils.log("Zona del baño cargada correctamente.");
    }
}