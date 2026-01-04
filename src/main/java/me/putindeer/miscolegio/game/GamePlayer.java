package me.putindeer.miscolegio.game;

import io.papermc.paper.registry.keys.SoundEventKeys;
import lombok.Data;
import me.putindeer.miscolegio.Main;
import me.putindeer.miscolegio.comodin.ComodinType;
import me.putindeer.miscolegio.zone.ZoneLocation;
import net.kyori.adventure.sound.Sound;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;

import javax.annotation.Nullable;
import java.util.*;

@Data
public class GamePlayer {
    private final Main plugin = Main.getInstance();
    private final UUID uniqueId;
    private final String name;
    private int lives;
    private boolean alive = true;
    private boolean eliminated = false;
    private ZoneLocation currentAnswer;
    private int correctAnswers = 0;
    private int comodines = 0;
    private int eliminationRound;
    private int comodinLimit;
    private boolean comodinCooldown = false;

    public GamePlayer(Player player, int startingLives, int comodinLimit) {
        this.uniqueId = player.getUniqueId();
        this.name = player.getName();
        this.lives = startingLives;
        this.comodinLimit = comodinLimit;
    }

    public void loseLife(GameSession session) {
        lives--;
        Player player = getPlayer();
        if (lives <= 0) {
            alive = false;
            eliminationRound = session.getCurrentRound();
        }
        if (player != null && player.isOnline()) {
            if (alive) {
                if (lives * 2 > Objects.requireNonNull(player.getAttribute(Attribute.MAX_HEALTH)).getBaseValue()) {
                    Objects.requireNonNull(player.getAttribute(Attribute.MAX_HEALTH)).setBaseValue(lives * 2);
                }
                player.setHealth(lives * 2);
            } else {
                player.setHealth(0.0001);
            }
        }
    }

    public void addLife() {
        lives++;
        Player player = getPlayer();
        if (player != null && player.isOnline()) {
            if (lives * 2 > Objects.requireNonNull(player.getAttribute(Attribute.MAX_HEALTH)).getBaseValue()) {
                Objects.requireNonNull(player.getAttribute(Attribute.MAX_HEALTH)).setBaseValue(lives * 2);
            }
            player.setHealth(lives * 2);
        }
    }

    public void clearAnswer() {
        this.currentAnswer = null;
    }

    public @Nullable Player getPlayer() {
        return Bukkit.getPlayer(uniqueId);
    }

    public void addComodin(ComodinType type) {
        Player player = getPlayer();
        if (player == null || !player.isOnline()) return;
        if (comodines >= comodinLimit) {
            plugin.utils.message(player, Sound.sound(SoundEventKeys.ENTITY_ITEM_BREAK, Sound.Source.MASTER, 1, 1),
                    "<red>Tienes " + comodines + " comodines, no puedes tener más por ahora.");
            return;
        }
        player.getInventory().addItem(type.buildItem());
        player.playSound(Sound.sound(SoundEventKeys.ENTITY_ITEM_PICKUP, Sound.Source.MASTER, 1, 1));
        comodines++;
    }

    public void useComodin() {
        comodines--;
        comodinCooldown = true;
        plugin.utils.delay(60, () -> comodinCooldown = false);
    }
}