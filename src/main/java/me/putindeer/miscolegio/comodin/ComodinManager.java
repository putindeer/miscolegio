package me.putindeer.miscolegio.comodin;

import io.papermc.paper.registry.keys.SoundEventKeys;
import me.putindeer.miscolegio.Main;
import me.putindeer.miscolegio.game.GameManager;
import me.putindeer.miscolegio.game.GamePlayer;
import me.putindeer.miscolegio.game.GameSession;
import me.putindeer.miscolegio.game.GameState;
import net.kyori.adventure.sound.Sound;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class ComodinManager {
    private final Main plugin;
    private final GameManager game;
    private final GameSession session;
    public final ComodinEvents events;
    public ComodinManager(Main plugin, GameManager game, GameSession session) {
        this.plugin = plugin;
        this.game = game;
        this.session = session;
        this.events = new ComodinEvents(plugin, this);
    }

    public void giveComodines() {
        session.setState(GameState.COMODIN_ROULETTE);

        Map<UUID, ComodinType> playerComodines = new HashMap<>();
        for (GamePlayer gp : session.getAlivePlayers()) {
            playerComodines.put(gp.getUniqueId(), selectRandomComodin());
        }

        showRouletteAnimation(playerComodines, () -> {
            session.getAliveOnlinePlayers().forEach(gamePlayer -> {
                ComodinType comodin = playerComodines.get(gamePlayer.getUniqueId());
                gamePlayer.addComodin(comodin);
            });

            session.setState(GameState.WAITING_QUESTION);
            game.giveNextPhaseItem();
        });
    }

    private ComodinType selectRandomComodin() {
        double tierC = session.getTierCChance();
        double tierB = session.getTierBChance();
        double tierA = session.getTierAChance();
        double tierS = session.getTierSChance();

        double total = tierC + tierB + tierA + tierS;
        double random = Math.random() * total;

        ComodinTier selectedTier;
        if (random < tierC) {
            selectedTier = ComodinTier.C;
        } else if (random < tierC + tierB) {
            selectedTier = ComodinTier.B;
        } else if (random < tierC + tierB + tierA) {
            selectedTier = ComodinTier.A;
        } else {
            selectedTier = ComodinTier.S;
        }

        List<ComodinType> tierComodines = Arrays.stream(ComodinType.values())
                .filter(c -> c.getTier() == selectedTier)
                .toList();

        return tierComodines.get(new Random().nextInt(tierComodines.size()));
    }

    public void showRouletteAnimation(Map<UUID, ComodinType> playerComodines, Runnable onComplete) {
        List<ComodinType> allComodines = Arrays.asList(ComodinType.values());

        new BukkitRunnable() {
            int ticks = 0;
            final int totalTicks = 260;
            int currentIndex = 0;

            @Override
            public void run() {
                if (ticks >= totalTicks) {
                    session.getAliveOnlinePlayers().forEach(gamePlayer -> {
                        Player player = gamePlayer.getPlayer();
                        ComodinType winner = playerComodines.get(gamePlayer.getUniqueId());
                        String display = buildRouletteDisplay(winner, winner, winner, winner, winner);
                        plugin.utils.title(player, display, winner.getTier().getColor() + winner.getName(),
                                plugin.utils.timesFromTicks(0, 40, 20));
                        plugin.utils.message(player, Sound.sound(SoundEventKeys.ENTITY_PLAYER_LEVELUP, Sound.Source.MASTER, 1, 1),
                                "<green>Has conseguido el comodín: " + winner.getTier().getColor() + winner.getName());
                    });
                    cancel();
                    Bukkit.getScheduler().runTask(plugin, onComplete);
                    return;
                }

                int speed = ticks < 80 ? 2 :
                        ticks < 140 ? 4 :
                                ticks < 180 ? 8
                                        : 20;

                boolean finalPhase = false;

                if (ticks % speed == 0) {
                    int remaining = totalTicks - ticks;

                    session.getAliveOnlinePlayers().forEach(gamePlayer -> {
                        Player player = gamePlayer.getPlayer();
                        if (player == null || !player.isOnline()) return;

                        ComodinType winner = playerComodines.get(gamePlayer.getUniqueId());
                        ComodinType[] display = new ComodinType[5];

                        for (int i = 0; i < 5; i++) {
                            int index = (currentIndex + i - 2) % allComodines.size();
                            if (index < 0) index += allComodines.size();
                            display[i] = allComodines.get(index);
                        }

                        if (remaining <= 60 && remaining > 40) {
                            display[4] = winner;
                        } else if (remaining <= 40 && remaining > 20) {
                            display[3] = winner;
                        } else if (remaining <= 20) {
                            display[2] = winner;
                        }

                        String rouletteDisplay = buildRouletteDisplay(
                                display[0], display[1], display[2], display[3], display[4]
                        );

                        plugin.utils.title(player, rouletteDisplay, "",
                                Sound.sound(SoundEventKeys.BLOCK_NOTE_BLOCK_HAT, Sound.Source.MASTER, 0.5f, 1),
                                plugin.utils.timesFromTicks(0, 40, 0)
                        );
                    });


                    currentIndex++;
                }

                ticks += 2;
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }

    private String buildRouletteDisplay(ComodinType c1, ComodinType c2, ComodinType center, ComodinType c4, ComodinType c5) {
        String s1 = getComodinSprite(c1);
        String s2 = getComodinSprite(c2);
        String sCenter = getComodinSprite(center);
        String s4 = getComodinSprite(c4);
        String s5 = getComodinSprite(c5);

        return String.format("<!shadow><dark_gray>%s %s</dark_gray> <white>%s</white> <dark_gray>%s %s</dark_gray>",
                s1, s2, sCenter, s4, s5);
    }

    private String getComodinSprite(ComodinType type) {
        String material = type.getMaterial().name().toLowerCase();
        return "<sprite:items:item/" + material + ">";
    }

    public ComodinType getComodinType(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;

        for (ComodinType type : ComodinType.values()) {
            ItemStack expectedItem = type.buildItem();
            if (item.isSimilar(expectedItem)) {
                return type;
            }
        }

        return null;
    }

    public void broadcastComodinUse(GamePlayer player, String comodinName, String message) {
        GameSession session = plugin.game.getSession();

        plugin.utils.message(session.getAliveOnlinePlayersAsPlayerList(),
                Sound.sound(SoundEventKeys.ENTITY_EXPERIENCE_ORB_PICKUP, Sound.Source.MASTER, 1, 1),
                "<gold>" + player.getName() + " <yellow>usó: <white>" + comodinName,
                "<gray>" + message);
    }

    public void consumeItem(GamePlayer gamePlayer, ItemStack item) {
        Player player = gamePlayer.getPlayer();
        if (player == null) return;
        item.setAmount(item.getAmount() - 1);
        gamePlayer.useComodin();
    }
}
