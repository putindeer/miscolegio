package me.putindeer.miscolegio.util;

import me.putindeer.miscolegio.Main;
import me.putindeer.miscolegio.game.ForceStopCommand;
import me.putindeer.miscolegio.game.GameManager;
import me.putindeer.miscolegio.game.StartCommand;
import me.putindeer.miscolegio.question.QuestionCommand;
import me.putindeer.miscolegio.question.QuestionManager;
import me.putindeer.miscolegio.zone.ZoneCommand;
import me.putindeer.miscolegio.zone.ZoneManager;

public class StartThings {
    private final Main plugin;

    public StartThings(Main plugin) {
        this.plugin = plugin;
        plugin.saveDefaultConfig();
        this.enable();
    }

    private void enable() {
        registerManagers();
        registerCommands();
    }

    private void registerManagers() {
        plugin.question = new QuestionManager(plugin);
        plugin.zone = new ZoneManager(plugin);
        plugin.game = new GameManager(plugin);
        plugin.hub = new HubEvents(plugin);
        plugin.board = new ScoreboardManager(plugin);
    }

    private void registerCommands() {
        new QuestionCommand(plugin);
        new ZoneCommand(plugin);
        new StartCommand(plugin);
        new ForceStopCommand(plugin);
        new ReloadCommand(plugin);
    }
}
