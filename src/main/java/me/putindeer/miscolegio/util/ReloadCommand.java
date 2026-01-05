package me.putindeer.miscolegio.util;

import me.putindeer.miscolegio.Main;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class ReloadCommand implements CommandExecutor {
    private final Main plugin;

    public ReloadCommand(Main plugin) {
        this.plugin = plugin;
        Objects.requireNonNull(plugin.getCommand("reload")).setExecutor(this);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        plugin.reloadConfig();
        plugin.utils.message(sender, "<green>¡Configuración recargada exitosamente!");

        return true;
    }
}