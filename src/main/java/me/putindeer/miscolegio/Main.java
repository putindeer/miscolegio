package me.putindeer.miscolegio;

import lombok.Getter;
import me.putindeer.api.util.PluginUtils;
import me.putindeer.miscolegio.util.ScoreboardManager;
import me.putindeer.miscolegio.util.StartThings;
import me.putindeer.miscolegio.game.GameManager;
import me.putindeer.miscolegio.util.HubEvents;
import me.putindeer.miscolegio.question.QuestionManager;
import me.putindeer.miscolegio.zone.ZoneManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class Main extends JavaPlugin {
    @Getter
    public static Main instance;
    public PluginUtils utils;
    public StartThings start;
    public QuestionManager question;
    public ZoneManager zone;
    public GameManager game;
    public HubEvents hub;
    public ScoreboardManager board;

    @Override
    public void onEnable() {
        instance = this;
        utils = new PluginUtils(this, "<light_purple>[</light_purple><head:99dec942-8940-4ce6-b3c4-f5ac7c23f4ba:true><light_purple>]</light_purple> <reset>");
        start = new StartThings(this);
    }

    @Override
    public void onDisable() {
        start.disable();
    }
}
