package me.putindeer.miscolegio.game;

import me.putindeer.miscolegio.Main;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class StartCommand implements CommandExecutor {
    private final Main plugin;
    public StartCommand(Main plugin) {
        this.plugin = plugin;
        Objects.requireNonNull(plugin.getCommand("start")).setExecutor(this);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!(sender instanceof Player player)) {
            plugin.utils.message(sender, "Este comando solo puede ser usado por jugadores");
            return true;
        }

        plugin.game.prepareGame(player);

        return true;
    }
}
