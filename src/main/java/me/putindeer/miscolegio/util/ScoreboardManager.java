package me.putindeer.miscolegio.util;

import fr.mrmicky.fastboard.adventure.FastBoard;
import me.putindeer.miscolegio.Main;
import me.putindeer.miscolegio.game.GamePlayer;
import me.putindeer.miscolegio.game.GameSession;
import me.putindeer.miscolegio.game.GameState;
import net.kyori.adventure.text.Component;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class ScoreboardManager implements Listener {
    private final Main plugin;
    private final Map<UUID, FastBoard> boards = new HashMap<>();

    public ScoreboardManager(Main plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        startUpdateTask();
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        FastBoard board = new FastBoard(player);
        boards.put(player.getUniqueId(), board);
        board.updateTitle(plugin.utils.chat("<light_purple><bold>MISCOLEGIO"));
        updateBoard(board);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        FastBoard board = boards.remove(player.getUniqueId());
        if (board != null) {
            board.delete();
        }
    }

    private void startUpdateTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (FastBoard board : boards.values()) {
                    if (board.getPlayer().isOnline()) {
                        updateBoard(board);
                        updateTabList(board.getPlayer());
                    }
                }
            }
        }.runTaskTimerAsynchronously(plugin, 0L, 20L); // Update every second
    }

    public void removeBoard(Player player) {
        FastBoard board = boards.remove(player.getUniqueId());
        if (board != null) {
            board.delete();
        }
    }

    public FastBoard getBoard(Player player) {
        return boards.get(player.getUniqueId());
    }

    private void updateBoard(FastBoard board) {
        Player player = board.getPlayer();

        Component separator = plugin.utils.chat("<gray><st>" + " ".repeat(35) + "</st></gray>");

        Component footer = plugin.utils.chat("<light_purple>elpueblo.holy.gg <gray>/ <yellow>holy.gg");

        if (plugin.game.isGameInactive()) {
            updateLobbyBoard(board, separator, footer);
        } else if (plugin.game.isGameFinished()) {
            updateFinishedBoard(board, player, separator, footer);
        } else {
            updateGameBoard(board, player, separator, footer);
        }
    }

    private void updateGameBoard(FastBoard board, Player player, Component separator, Component footer) {
        GameSession session = plugin.game.getSession();

        if (session == null) {
            updateLobbyBoard(board, separator, footer);
            return;
        }

        GamePlayer gamePlayer = session.getPlayer(player.getUniqueId());

        String stateText = getStateText(session.getState());

        String questionInfo = session.getCurrentRound() + "/" + session.getTotalRounds();
        String levelInfo;
        if (session.getCurrentQuestion() != null) {
            levelInfo = capitalizeFirst(session.getCurrentQuestion().getLevel().name());
        } else {
            levelInfo = "Kinder";
        }

        String livesDisplay;
        if (gamePlayer != null && gamePlayer.isAlive()) {
            livesDisplay = "<gray>Tus vidas: " + getHeartsDisplay(gamePlayer.getLives(), session);
        } else if (Objects.equals(session.getHostPlayer(), player)) {
            livesDisplay = "<gold>Hosteando...";
        } else {
            livesDisplay = "<red>✗ Eliminado";
        }

        int aliveCount = session.getAliveCount();

        board.updateLines(
                separator,
                Component.empty(),
                plugin.utils.chat("<gray>Estado: <yellow>" + stateText),
                plugin.utils.chat("<gray>Pregunta: <white>" + questionInfo),
                plugin.utils.chat("<gray>Nivel: <aqua>" + levelInfo),
                Component.empty(),
                plugin.utils.chat(livesDisplay),
                plugin.utils.chat("<gray>Jugadores: <green>" + aliveCount),
                Component.empty(),
                separator,
                footer
        );
    }

    private void updateFinishedBoard(FastBoard board, Player player, Component separator, Component footer) {
        GameSession session = plugin.game.getSession();

        if (session == null) {
            updateLobbyBoard(board, separator, footer);
            return;
        }

        GamePlayer gamePlayer = session.getPlayer(player.getUniqueId());

        boolean isWinner = gamePlayer != null && gamePlayer.isAlive();

        int correctAnswers = gamePlayer != null ? gamePlayer.getCorrectAnswers() : 0;
        int totalQuestions = session.getTotalRounds();

        int winners = session.getAlivePlayers().size();

        if (isWinner) {
            board.updateLines(
                    separator,
                    Component.empty(),
                    plugin.utils.chat("<gold><bold>¡GANASTE!"),
                    Component.empty(),
                    plugin.utils.chat("<gray>Respuestas: <green>" + correctAnswers + "<gray>/" + totalQuestions),
                    plugin.utils.chat("<gray>Vidas restantes: " + getHeartsDisplay(gamePlayer.getLives(), session)),
                    Component.empty(),
                    plugin.utils.chat("<gray>Ganadores: <yellow>" + winners),
                    Component.empty(),
                    separator,
                    footer
            );
        } else {
            String eliminationRound = "---";
            if (gamePlayer != null) {
                eliminationRound = String.valueOf(gamePlayer.getEliminationRound());
            }

            assert gamePlayer != null;
            board.updateLines(
                    separator,
                    Component.empty(),
                    plugin.utils.chat("<red><bold>ELIMINADO"),
                    Component.empty(),
                    plugin.utils.chat("<gray>Respuestas: <green>" + correctAnswers + "<gray>/" + totalQuestions),
                    plugin.utils.chat("<gray>Eliminado en: <white>Ronda " + eliminationRound),
                    Component.empty(),
                    plugin.utils.chat("<gray>Ganadores: <yellow>" + winners),
                    Component.empty(),
                    separator,
                    footer
            );
        }
    }

    private void updateLobbyBoard(FastBoard board, Component separator, Component footer) {
        board.updateLines(
                separator,
                Component.empty(),
                plugin.utils.chat("<gray>Estado: <yellow>Esperando..."),
                plugin.utils.chat("<gray>Jugadores: <white>" + plugin.getServer().getOnlinePlayers().stream()
                        .filter(player -> player.getGameMode() == GameMode.ADVENTURE).toList().size()),
                Component.empty(),
                separator,
                footer
        );
    }

    private void updateTabList(Player player) {
        String tps = new DecimalFormat("##").format(plugin.getServer().getTPS()[0]);

        String header = "<light_purple><bold>MISCOLEGIO";

        String misco = "<gradient:#FF6FB1:#D94C9A>Misco_Jhonnes</gradient>";
        String vane = "<gradient:#F2F2F2:#BDBDBD>SlenderVane</gradient>";
        String junko = "<gradient:#5FE0E6:#3BB4C1>Junko05</gradient>";
        String putindeer = "<gradient:#F0B400:#EDDE67>putindeer</gradient>";

        String footer = String.format(
                """
                        <white>Ping: <light_purple>%d <dark_gray>| <white>Tps: <light_purple>%s
                       \s
                        <gray>Evento organizado por %s
                          <gray>Con la ayuda de: %s, %s y %s \s
                       \s
                        <gray>Servidor patrocinado por <yellow><bold>HolyHosting</bold>
                        <white>Adquiere un servidor en <yellow>holy.gg""",
                player.getPing(),
                tps,
                misco, vane, junko, putindeer
        );

        player.sendPlayerListHeaderAndFooter(plugin.utils.chat(header), plugin.utils.chat(footer));
    }

    private String getStateText(GameState state) {
        return switch (state) {
            case WAITING -> "Esperando";
            case STARTING -> "Iniciando";
            case ANSWERING -> "Respondiendo";
            case CHECKING -> "Verificando";
            case WAITING_QUESTION -> "Esperando pregunta";
            case WAITING_COMODIN -> "Esperando comodin";
            case FINISHED -> "Finalizado";
            case COMODIN_ROULETTE -> "En la ruleta";
        };
    }

    public String getHeartsDisplay(int lives, GameSession session) {
        if (lives <= 0) return "<red>✗";

        int initialLives = session.getLives();
        StringBuilder hearts = new StringBuilder();
        int displayHearts = Math.min(lives, 10);

        for (int i = 0; i < displayHearts; i++) {
            if (lives > initialLives) {
                hearts.append("<aqua>❤");
            } else if (lives > initialLives * 2.0 / 3.0) {
                hearts.append("<green>❤");
            } else if (lives > initialLives * 1.0 / 3.0) {
                hearts.append("<yellow>❤");
            } else {
                hearts.append("<red>❤");
            }
        }

        if (lives > 10) {
            String color = lives > initialLives ? "<aqua>" : "<white>";
            hearts.append(" ").append(color).append("x").append(lives);
        }

        return hearts.toString();
    }

    private String capitalizeFirst(String text) {
        if (text == null || text.isEmpty()) return text;
        return text.substring(0, 1).toUpperCase() + text.substring(1).toLowerCase();
    }
}