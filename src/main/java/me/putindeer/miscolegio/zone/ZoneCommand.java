package me.putindeer.miscolegio.zone;

import me.putindeer.miscolegio.Main;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import java.util.*;
import java.util.stream.Collectors;

public class ZoneCommand implements TabExecutor {
    private final Main plugin;

    public ZoneCommand(Main plugin) {
        this.plugin = plugin;
        Objects.requireNonNull(plugin.getCommand("zone")).setExecutor(this);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!(sender instanceof Player player)) {
            plugin.utils.message(sender, "Este comando solo puede ser usado por jugadores.");
            return true;
        }

        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "setup" -> plugin.zone.startSelection(player);
            case "override" -> plugin.zone.overrideSession(player);
            case "confirm" -> plugin.zone.confirmAndContinue(player);
            case "cancel" -> plugin.zone.cancelSelection(player);
            case "delete" -> handleDelete(player);
            case "reload" -> handleReload(player);
            case "check" -> handleCheck(player);
            case "bano", "baño" -> handleBano(player);
            case "banoconfirm", "bañoconfirm" -> handleBanoConfirm(player);
            default -> sendHelp(player);
        }

        return true;
    }

    private void sendHelp(Player player) {
        plugin.utils.message(player,
                "<gold>=== Sistema de Zonas ===",
                "<yellow>/zone setup <gray>- Iniciar selección",
                "<yellow>/zone override <gray>- Reemplazar la sesión activa de otro jugador",
                "<yellow>/zone confirm <gray>- Confirmar tu selección de zona y continuar con la siguiente",
                "<yellow>/zone cancel <gray>- Cancelar configuración de zona",
                "<yellow>/zone delete <gray>- Eliminar la zona",
                "<yellow>/zone reload <gray>- Recargar desde la config",
                "<yellow>/zone check <gray>- Verificar en que zona estás actualmente",
                "<yellow>/zone baño <gray>- Inicia la selección de la zona del baño",
                "<yellow>/zone bañoconfirm <gray>- Confirma la selección de la zona del baño");
    }

    private void handleDelete(Player player) {
        if (plugin.zone.getZone() == null) {
            plugin.utils.message(player, "<red>No hay zona para eliminar");
            return;
        }

        plugin.getConfig().set("game.zone", null);
        plugin.saveConfig();
        plugin.zone.reloadZone();

        plugin.utils.message(player, "<green>✓ Zona eliminada");
    }

    private void handleReload(Player player) {
        plugin.reloadConfig();
        plugin.zone.reloadZone();

        if (plugin.zone.getZone() != null && plugin.zone.getZone().isComplete()) {
            plugin.utils.message(player, "<green>✓ Zona recargada");
        } else {
            plugin.utils.message(player, "<yellow>Config recargado, sin zona válida");
        }
    }

    private void handleCheck(Player player) {
        if (plugin.zone.getZone() == null) {
            plugin.utils.message(player, "<red>No hay zona para verificar");
            return;
        }

        switch (plugin.zone.getZone().getPlayerZone(player)) {
            case A -> plugin.utils.message(player, "<yellow>Estás en la Zona A.");
            case B -> plugin.utils.message(player, "<yellow>Estás en la Zona B.");
            case C -> plugin.utils.message(player, "<yellow>Estás en la Zona C.");
            case D -> plugin.utils.message(player, "<yellow>Estás en la Zona D.");
            case NONE -> plugin.utils.message(player, "<yellow>No estás dentro de ninguna zona, pero estás dentro de la zona de juego.");
            case OUT_OF_BOUNDS -> plugin.utils.message(player, "<yellow>Estás fuera de la zona de juego.");
        }
    }

    private void handleBano(Player player) {
        ZoneManager zone = plugin.zone;
        if (zone.isInBathroomSession(player)) {
            plugin.utils.message(player, "<red>¡Ya estás en una sesión de establecer la ubicación del baño!");
            return;
        }
        zone.startBathroomSession(player);
    }

    private void handleBanoConfirm(Player player) {
        ZoneManager zone = plugin.zone;

        if (!zone.isInBathroomSession(player)) {
            plugin.utils.message(player, "<red>¡No estás en una sesión de establecer la ubicación del baño!");
            return;
        }

        zone.confirmBathroomLocation(player.getLocation());

        plugin.utils.message(player, "<green>Ubicación del baño guardada correctamente.");
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
        if (args.length == 1) {
            List<String> commands = new ArrayList<>(Arrays.asList("setup", "confirm", "cancel", "delete", "reload", "check", "baño", "bañoconfirm"));

            if (plugin.zone.hasActiveSession()) {
                commands.add("override");
            }

            return commands.stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .sorted()
                    .collect(Collectors.toList());
        }

        return Collections.emptyList();
    }
}