package me.putindeer.miscolegio.game;

import lombok.Data;
import me.putindeer.miscolegio.Main;
import me.putindeer.miscolegio.question.Question;
import me.putindeer.miscolegio.zone.ZoneLocation;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;

import javax.annotation.Nullable;
import java.util.*;

@Data
public class GameSession {
    private final Main plugin = Main.getInstance();
    private final UUID host;
    private final Map<UUID, GamePlayer> players;
    private GameState state;
    private Question currentQuestion;
    private int currentRound;
    private int totalRounds;
    private List<Question> questionQueue;
    private int taskId;
    private int lives;
    private int roundsPerComodin;
    private int comodinLimit;
    private boolean testing;
    private double tierCChance;
    private double tierBChance;
    private double tierAChance;
    private double tierSChance;
    private Location bathroomLocation;

    public GameSession(Player host) {
        this.host = host.getUniqueId();
        this.players = new HashMap<>();
        this.state = GameState.WAITING;
        this.currentRound = 0;
        this.questionQueue = new ArrayList<>();
        this.taskId = -1;
    }

    public void addPlayer(Player player) {
        players.put(player.getUniqueId(), new GamePlayer(player, lives, comodinLimit));
        Objects.requireNonNull(player.getAttribute(Attribute.MAX_HEALTH)).setBaseValue(lives * 2);
        player.setHealth(lives * 2);
        plugin.utils.message(player, "<green>¡Te has unido a la partida!");
    }

    public void removePlayer(UUID uuid) {
        players.remove(uuid);
    }

    public GamePlayer getPlayer(UUID uuid) {
        return players.get(uuid);
    }

    public List<GamePlayer> getAlivePlayers() {
        return players.values().stream()
                .filter(GamePlayer::isAlive)
                .toList();
    }

    public List<GamePlayer> getAliveOnlinePlayers() {
        return players.values().stream()
                .filter(GamePlayer::isAlive)
                .filter(gp -> Bukkit.getOfflinePlayer(gp.getUniqueId()).isOnline())
                .filter(gp -> Bukkit.getPlayer(gp.getUniqueId()) != null)
                .toList();
    }

    public List<GamePlayer> getAliveOnlinePlayersWithoutBathroom() {
        return players.values().stream()
                .filter(GamePlayer::isAlive)
                .filter(gp -> Bukkit.getOfflinePlayer(gp.getUniqueId()).isOnline())
                .filter(gp -> Bukkit.getPlayer(gp.getUniqueId()) != null)
                .filter(gP -> gP.getCurrentAnswer() != ZoneLocation.BATHROOM)
                .toList();
    }

    public List<Player> getAliveOnlinePlayersAsPlayerList() {
        return players.values().stream()
                .filter(GamePlayer::isAlive)
                .filter(gp -> Bukkit.getOfflinePlayer(gp.getUniqueId()).isOnline())
                .filter(gp -> Bukkit.getPlayer(gp.getUniqueId()) != null)
                .map(GamePlayer::getPlayer)
                .filter(Objects::nonNull)
                .toList();
    }

    /**
     * Nombre comicamente largo
     * @return Solo eso
     */
    public List<Player> getAliveOnlinePlayersWithoutBathroomAsPlayerList() {
        return players.values().stream()
                .filter(GamePlayer::isAlive)
                .filter(gp -> Bukkit.getOfflinePlayer(gp.getUniqueId()).isOnline())
                .filter(gp -> Bukkit.getPlayer(gp.getUniqueId()) != null)
                .filter(gP -> gP.getCurrentAnswer() != ZoneLocation.BATHROOM)
                .map(GamePlayer::getPlayer)
                .filter(Objects::nonNull)
                .toList();
    }

    public List<GamePlayer> getDeadPlayers() {
        return players.values().stream()
                .filter(gp -> !gp.isAlive())
                .toList();
    }

    public boolean hasPlayer(UUID uuid) {
        return players.containsKey(uuid);
    }

    public @Nullable Player getHostPlayer() {
        return Bukkit.getPlayer(host);
    }

    public int getAliveCount() {
        return (int) players.values().stream().filter(GamePlayer::isAlive).count();
    }

    public void nextRound() {
        currentRound++;
        players.values().forEach(GamePlayer::clearAnswer);
    }

    public boolean isFinished() {
        return currentRound >= totalRounds || (!testing && getAliveCount() <= 1);
    }
}
