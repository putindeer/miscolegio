package me.putindeer.miscolegio.game;

import io.papermc.paper.registry.keys.SoundEventKeys;
import lombok.Getter;
import me.putindeer.miscolegio.Main;
import me.putindeer.miscolegio.comodin.ComodinManager;
import me.putindeer.miscolegio.question.Question;
import me.putindeer.miscolegio.question.QuestionLevel;
import me.putindeer.miscolegio.zone.Cuboid;
import me.putindeer.miscolegio.zone.GameZone;
import me.putindeer.miscolegio.zone.ZoneLocation;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import java.util.*;
import net.kyori.adventure.sound.Sound;
import org.bukkit.scoreboard.*;
import org.bukkit.util.Vector;

public class GameManager implements Listener {
    private final Main plugin;
    @Getter
    private GameSession session;
    public ComodinManager comodin;
    private final ItemStack phaseItem;

    public GameManager(Main plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        phaseItem = plugin.utils.ib(Material.BLAZE_POWDER).name("<red>Pasar a la siguiente fase")
                .lore("Usa este item para pasar a la siguiente fase.").build();
    }

    public void prepareGame(Player host) {
        if (session != null && session.getState() != GameState.FINISHED) {
            plugin.utils.message(host, "<red>¡Ya hay una partida en curso!");
            return;
        }

        GameZone zone = plugin.zone.getZone();
        if (zone == null || !zone.isComplete()) {
            plugin.utils.message(host, "<red>¡No hay una zona configurada! Usa /zone setup");
            return;
        }

        Location bathroom = plugin.zone.getBathroomLocation();

        if (bathroom == null) {
            plugin.utils.message(host, "<red>¡No hay una zona del baño configurada! Usa /zone baño");
            return;
        }

        session = new GameSession(host);
        comodin = new ComodinManager(plugin, this, session);

        session.setLives(plugin.getConfig().getInt("game.lives", 3));
        session.setTierCChance(plugin.getConfig().getDouble("game.comodin-chances.C", 40.0));
        session.setTierBChance(plugin.getConfig().getDouble("game.comodin-chances.B", 30.0));
        session.setTierAChance(plugin.getConfig().getDouble("game.comodin-chances.A", 20.0));
        session.setTierSChance(plugin.getConfig().getDouble("game.comodin-chances.S", 10.0));
        session.setRoundsPerComodin(plugin.getConfig().getInt("game.rounds-per-comodin", 3));
        session.setComodinLimit(plugin.getConfig().getInt("game.comodin-limit", 2));

        session.setBathroomLocation(bathroom);

        int kinderQ = plugin.getConfig().getInt("game.questions-per-category.kinder", 7);
        int basicaQ = plugin.getConfig().getInt("game.questions-per-category.basica", 10);
        int mediaQ = plugin.getConfig().getInt("game.questions-per-category.media", 10);
        int uniQ = plugin.getConfig().getInt("game.questions-per-category.universidad", 10);

        List<Question> queue = new ArrayList<>();
        queue.addAll(selectQuestionsByLevel(QuestionLevel.KINDER, kinderQ));
        queue.addAll(selectQuestionsByLevel(QuestionLevel.BASICA, basicaQ));
        queue.addAll(selectQuestionsByLevel(QuestionLevel.MEDIA, mediaQ));
        queue.addAll(selectQuestionsByLevel(QuestionLevel.UNIVERSIDAD, uniQ));

        if (queue.isEmpty()) {
            plugin.utils.message(host, "<red>No hay suficientes preguntas configuradas!");
            session = null;
            return;
        }

        session.setQuestionQueue(queue);
        session.setTotalRounds(queue.size());

        boolean testing = plugin.getConfig().getBoolean("game.testing", false);

        session.setTesting(testing);

        Bukkit.getOnlinePlayers().stream().filter(player -> player.getGameMode() == GameMode.ADVENTURE)
                .filter(player -> testing || player != host).forEach(player -> {
            session.addPlayer(player);
            if (zone.getPlayerZone(player) == ZoneLocation.OUT_OF_BOUNDS) {
                player.teleport(zone.getPlayArea().getCenter());
            }
        });

        if (session.getPlayers().isEmpty()) {
            plugin.utils.message(host, "<red>No hay jugadores disponibles para jugar!");
            session = null;
            return;
        }

        addLifeIndicator();
        createBarrierCapsule();
        giveNextPhaseItem();
    }

    public void giveNextPhaseItem() {
        Player host = session.getHostPlayer();
        if (host == null) return;

        host.getInventory().addItem(phaseItem);
    }

    private boolean phaseOnCooldown = false;

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (session == null || session.getHostPlayer() != player) return;

        if (event.getItem() == null) return;
        if (!event.getItem().isSimilar(phaseItem)) return;
        if (phaseOnCooldown) {
            plugin.utils.message(player, "<red>¡Espera un poco antes de cambiar de fase otra vez!");
            return;
        }

        event.setCancelled(true);
        event.getItem().setAmount(0);
        phaseOnCooldown = true;

        switch (session.getState()) {
            case WAITING -> startGame();
            case CHECKING -> checkAnswers();
            case WAITING_COMODIN -> comodin.giveComodines();
            case WAITING_QUESTION -> startNextQuestion();
            case FINISHED -> trulyEndGame();
        }

        plugin.utils.delay(100, () -> phaseOnCooldown = false);
    }

    private void trulyEndGame() {
        removeBarrierCapsule();
        removeLifeIndicator();
        session = null;
    }

    private int healthTaskId = -1;

    private void addLifeIndicator() {
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        if (scoreboard.getObjective("LivesPL") == null) {
            scoreboard.registerNewObjective("LivesPL", Criteria.DUMMY, plugin.utils.chat("<red>❤")).setDisplaySlot(DisplaySlot.BELOW_NAME);
        }

        healthTaskId = plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, () -> Bukkit.getServer().getOnlinePlayers().forEach(player -> {
            GamePlayer gamePlayer = session.getPlayer(player.getUniqueId());
            if (gamePlayer == null || !gamePlayer.isAlive()) return;
            Objective objective = scoreboard.getObjective("LivesPL");
            Score score = Objects.requireNonNull(objective).getScore(player.getName());

            int lives = gamePlayer.getLives();
            score.setScore(lives);
        }),0,5);
    }

    public void removeLifeIndicator() {
        if (healthTaskId != -1) {
            Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
            Objective objective = scoreboard.getObjective("LivesPL");
            assert objective != null;
            objective.unregister();
            plugin.getServer().getScheduler().cancelTask(healthTaskId);
            healthTaskId = -1;
        }
    }

    private void startGame() {
        int lives = session.getLives();
        session.setState(GameState.STARTING);
        plugin.utils.broadcast("<green>═══════════════════════════════",
                "<gold><bold>¡PARTIDA INICIADA!",
                "<green>═══════════════════════════════",
                "",
                "<yellow>Jugadores: <white>" + session.getPlayers().size(),
                "<yellow>Vidas: <white>" + lives,
                "<yellow>Preguntas: <white>" + session.getTotalRounds(),
                "",
                "<gray>La partida comenzará en 5 segundos...");

        new BukkitRunnable() {
            int count = 5;

            @Override
            public void run() {
                if (count <= 0) {
                    cancel();
                    startNextQuestion();
                    return;
                }

                plugin.utils.broadcast(Sound.sound(SoundEventKeys.BLOCK_NOTE_BLOCK_HAT, Sound.Source.MASTER, 1, 1),
                        "<gold>" + count + "...");
                count--;
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    private void startNextQuestion() {
        if (session == null || session.isFinished()) {
            endGame();
            return;
        }

        session.nextRound();

        Question question = session.getQuestionQueue().get(session.getCurrentRound() - 1);
        session.setCurrentQuestion(question);
        session.setState(GameState.ANSWERING);

        displayQuestion(question);

        int questionTime = plugin.getConfig().getInt("game.question-time", 20);

        new BukkitRunnable() {
            int timeLeft = questionTime;

            @Override
            public void run() {
                if (session == null || session.getState() != GameState.ANSWERING) {
                    cancel();
                    return;
                }

                if (timeLeft <= 0) {
                    cancel();
                    preAnswers();
                    return;
                }

                if (timeLeft % 5 == 0 || timeLeft <= 3) {
                    plugin.utils.broadcast(Sound.sound(SoundEventKeys.BLOCK_NOTE_BLOCK_HAT, Sound.Source.MASTER, 1, 1),
                            "<yellow>Tiempo restante: <white>" + timeLeft + "s");
                }

                timeLeft--;
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    private void displayQuestion(Question question) {
        String colorA = "<" + plugin.getConfig().getString("game.colors.A", "RED").toLowerCase() + ">";
        String colorB = "<" + plugin.getConfig().getString("game.colors.B", "BLUE").toLowerCase() + ">";
        String colorC = "<" + plugin.getConfig().getString("game.colors.C", "YELLOW").toLowerCase() + ">";
        String colorD = "<" + plugin.getConfig().getString("game.colors.D", "LIME").toLowerCase() + ">";

        plugin.utils.broadcast(Sound.sound(SoundEventKeys.ENTITY_PLAYER_LEVELUP, Sound.Source.MASTER, 1, 1),
                "",
                "<green>═══════════════════════════════",
                "<gold><bold>PREGUNTA " + session.getCurrentRound() + "/" + session.getTotalRounds(),
                "<green>═══════════════════════════════",
                "",
                "<white>" + question.getQuestion(),
                "",
                colorA + "A) " + question.getOptionA(),
                colorB + "B) " + question.getOptionB(),
                colorC + "C) " + question.getOptionC(),
                colorD + "D) " + question.getOptionD(),
                "",
                "<gray>¡Muévete a la zona de tu respuesta!");
    }

    private void preAnswers() {
        GameZone zone = plugin.zone.getZone();
        for (GamePlayer gamePlayer : session.getAlivePlayers()) {
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(gamePlayer.getUniqueId());
            Player player = offlinePlayer.getPlayer();
            if (!offlinePlayer.isOnline() || player == null) {
                gamePlayer.loseLife(session);
                continue;
            }

            ZoneLocation playerZone = zone.getPlayerZone(player);

            if (playerZone == ZoneLocation.NONE) {
                teleportToNearestZone(player);
            }

            if (gamePlayer.getCurrentAnswer() == null || gamePlayer.getCurrentAnswer() != ZoneLocation.BATHROOM) {
                gamePlayer.setCurrentAnswer(playerZone);
            }
        }
        plugin.utils.broadcast(Sound.sound(SoundEventKeys.BLOCK_VAULT_OPEN_SHUTTER, Sound.Source.MASTER, 1, 1),
                "<gray>¡Se acabó el tiempo!");
        fillWithBarriers();
        session.setState(GameState.CHECKING);
        giveNextPhaseItem();
    }

    private void checkAnswers() {
        if (session == null) return;

        GameZone zone = plugin.zone.getZone();
        ZoneLocation correctAnswer = session.getCurrentQuestion().getAnswerLocation();

        List<GamePlayer> correct = new ArrayList<>();
        List<GamePlayer> incorrect = new ArrayList<>();
        List<GamePlayer> skip = new ArrayList<>();

        removeBarriers();

        for (GamePlayer gamePlayer : session.getAlivePlayers()) {
            Player player = gamePlayer.getPlayer();

            ZoneLocation playerAnswer = gamePlayer.getCurrentAnswer();

            if (playerAnswer.equals(correctAnswer)) {
                giveCorrectAnswer(gamePlayer);
                correct.add(gamePlayer);
            } else if (playerAnswer == ZoneLocation.BATHROOM) {
                skipAnswer(gamePlayer);
                skip.add(gamePlayer);
            } else {
                giveIncorrectAnswer(gamePlayer);
                incorrect.add(gamePlayer);
            }
        }

        plugin.utils.broadcast(Sound.sound(SoundEventKeys.ENTITY_EXPERIENCE_ORB_PICKUP, Sound.Source.MASTER, 1, 1),
                "",
                "<green>═══════════════════════════════",
                "<gold><bold>RESULTADOS",
                "<green>═══════════════════════════════",
                "",
                "<yellow>Respuesta correcta:",
                "<green>" + correctAnswer.name() + ") " + getOptionText(correctAnswer),
                "",
                "<green>✓ Correctos: <white>" + correct.size(),
                "<red>✗ Incorrectos: <white>" + incorrect.size());
        if (!skip.isEmpty()) {
            plugin.utils.broadcast("<gold>⊘ Evitaron pregunta: <white>" + skip.size());
        }

        List<GamePlayer> eliminated = incorrect.stream().filter(gp -> !gp.isAlive()).toList();
        int eliminatedDelay = (int) plugin.getConfig().getDouble("game.eliminated-delay", 3) * 20;
        if (!eliminated.isEmpty()) {
            plugin.utils.delay(eliminatedDelay, () -> {
                plugin.utils.broadcast(Sound.sound(SoundEventKeys.ENTITY_ZOMBIE_DEATH, Sound.Source.MASTER, 1, 1), "",
                        "<red><bold>ELIMINADOS:");
                int i = 0;
                for (GamePlayer gamePlayer : eliminated) {
                    i++;
                    float pitch = (float) Math.max(1 - ((double) i / 10), 0.5);
                    plugin.utils.delay(eliminatedDelay * (i - 1) + eliminatedDelay / 3, () -> {
                        Player player = gamePlayer.getPlayer();
                        if (player != null) {
                            player.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, eliminatedDelay, 9, true, true));
                        }
                    });
                    plugin.utils.delay(eliminatedDelay * i, () -> {
                        plugin.utils.broadcast(Sound.sound(SoundEventKeys.ENTITY_WARDEN_DEATH, Sound.Source.MASTER, 1, pitch),
                                "<gray>- <white>" + gamePlayer.getName() + " <head:" + gamePlayer.getName() + ":false>");
                        Player player = gamePlayer.getPlayer();
                        if (player != null) {
                            blueFireExplosion(player.getLocation());
                            player.setGameMode(GameMode.SPECTATOR);
                        }
                    });
                }
            });
        }

        plugin.utils.delay(eliminatedDelay * (eliminated.size() + 1), () -> {
            if (session.getCurrentRound() % session.getRoundsPerComodin() == 0) {
                session.setState(GameState.WAITING_COMODIN);
            } else {
                session.setState(GameState.WAITING_QUESTION);
            }
            giveNextPhaseItem();
        });
    }

    public void blueFireExplosion(Location loc) {
        World world = loc.getWorld();
        if (world == null) return;

        world.spawnParticle(Particle.FLASH, loc, 1, 0, 0, 0, 0.05, Color.BLUE);
        world.spawnParticle(Particle.SOUL_FIRE_FLAME, loc, 120, 0.8, 0.8, 0.8, 0.05);
        world.spawnParticle(Particle.SOUL, loc, 80, 0.6, 0.6, 0.6, 0.1);

        for (int i = 0; i < 360; i += 15) {
            double rad = Math.toRadians(i);
            Vector dir = new Vector(Math.cos(rad), 0, Math.sin(rad)).multiply(0.6);

            world.spawnParticle(Particle.SOUL_FIRE_FLAME, loc.clone().add(dir), 4, 0, 0, 0, 0.02);
        }

        world.spawnParticle(Particle.WHITE_SMOKE, loc, 30, 0.5, 0.5, 0.5, 0);
    }

    private void endGame() {
        if (session == null) return;

        session.setState(GameState.FINISHED);

        List<GamePlayer> alive = session.getAlivePlayers();

        plugin.utils.broadcast(Sound.sound(SoundEventKeys.UI_TOAST_CHALLENGE_COMPLETE, Sound.Source.MASTER, 1, 1),
                "",
                "<gold>═══════════════════════════════",
                "<gold><bold>¡JUEGO TERMINADO!",
                "<gold>═══════════════════════════════",
                ""
        );

        if (alive.isEmpty()) {
            plugin.utils.broadcast("<yellow>¡No hay ganadores!");
        } else if (alive.size() == 1) {
            GamePlayer winner = alive.getFirst();
            plugin.utils.broadcast("<green><bold>GANADOR: " + winner.getName(),
                    "<yellow>Vidas restantes: <white>" + winner.getLives(),
                    "<yellow>Respuestas correctas: <white>" + winner.getCorrectAnswers());
        } else {
            plugin.utils.broadcast("<green><bold>GANADORES:");
            alive.forEach(gamePlayer -> plugin.utils.broadcast("<white>- " + gamePlayer.getName() + " <gray>(" + gamePlayer.getLives() + " vidas)"));
        }

        session.getAliveOnlinePlayersAsPlayerList().forEach(player -> spawnFireworks(player.getLocation()));
        giveNextPhaseItem();
    }

    private void giveCorrectAnswer(GamePlayer gamePlayer) {
        gamePlayer.setCorrectAnswers(gamePlayer.getCorrectAnswers() + 1);
        Player player = gamePlayer.getPlayer();
        if (player == null || !player.isOnline()) return;
        plugin.utils.title(player, "<green>✓", "<gray>¡Respuesta correcta!",
                Sound.sound(SoundEventKeys.ENTITY_PLAYER_LEVELUP, Sound.Source.MASTER, 1, 1));
    }

    private void skipAnswer(GamePlayer gamePlayer) {
        Player player = gamePlayer.getPlayer();
        if (player == null || !player.isOnline()) return;
        plugin.utils.title(player, "<gold>⊘", "<gray>¡Pregunta skipeada!",
                Sound.sound(SoundEventKeys.BLOCK_ANVIL_LAND, Sound.Source.MASTER, 1, 2));
        player.teleport(plugin.zone.getZone().getPlayArea().getCenter());
        plugin.utils.message(player, "<green>¡Has vuelto al juego!");
    }

    private void giveIncorrectAnswer(GamePlayer gamePlayer) {
        gamePlayer.loseLife(session);
        Player player = gamePlayer.getPlayer();
        if (player == null || !player.isOnline()) return;
        plugin.utils.title(player, "<red>✗", "<gray>¡Respuesta incorrecta!",
                Sound.sound(SoundEventKeys.BLOCK_BELL_USE, Sound.Source.MASTER, 1, 0.1f));
    }

    public void stopGame(Player stopper) {
        if (session == null) {
            plugin.utils.message(stopper, "<red>No hay ninguna partida en curso!");
            return;
        }

        plugin.utils.broadcast("<red>¡La partida ha sido detenida por un administrador!");

        session.getAliveOnlinePlayersAsPlayerList().forEach(player -> {
            player.setGameMode(GameMode.ADVENTURE);
            Objects.requireNonNull(player.getAttribute(Attribute.MAX_HEALTH)).setBaseValue(20.0);
        });

        removeBarriers();
        removeBarrierCapsule();
        removeLifeIndicator();
        session = null;
        comodin.events.disable();
        comodin = null;
    }

    private List<Question> selectQuestionsByLevel(QuestionLevel level, int amount) {
        List<Question> guaranteed = new ArrayList<>();
        List<Question> normal = new ArrayList<>();

        plugin.question.getQuestions().forEach(question -> {
            if (question.getLevel() != level) return;

            if (question.isGuaranteed()) {
                guaranteed.add(question);
            } else {
                normal.add(question);
            }
        });

        Collections.shuffle(normal);

        List<Question> result = new ArrayList<>(guaranteed);

        Collections.shuffle(result);
        int remaining = amount - guaranteed.size();
        if (remaining > 0) {
            result.addAll(normal.subList(0, Math.min(remaining, normal.size())));
        }

        Collections.shuffle(result);
        return result;
    }


    private String getOptionText(ZoneLocation answer) {
        Question q = session.getCurrentQuestion();
        return switch (answer.name()) {
            case "A" -> q.getOptionA();
            case "B" -> q.getOptionB();
            case "C" -> q.getOptionC();
            case "D" -> q.getOptionD();
            default -> "";
        };
    }

    private void createBarrierCapsule() {
        GameZone zone = plugin.zone.getZone();
        World world = zone.getPlayArea().getWorld();
        Cuboid area = zone.getPlayArea();

        int minX = area.getMinX() - 1;
        int maxX = area.getMaxX() + 1;
        int minZ = area.getMinZ() - 1;
        int maxZ = area.getMaxZ() + 1;
        int minY = area.getMinY() + 1;
        int maxY = area.getMaxY();

        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                world.getBlockAt(x, y, minZ).setType(Material.BARRIER);
                world.getBlockAt(x, y, maxZ).setType(Material.BARRIER);
            }
            for (int z = minZ; z <= maxZ; z++) {
                world.getBlockAt(minX, y, z).setType(Material.BARRIER);
                world.getBlockAt(maxX, y, z).setType(Material.BARRIER);
            }
        }
    }

    public void removeBarrierCapsule() {
        GameZone zone = plugin.zone.getZone();
        World world = zone.getPlayArea().getWorld();
        Cuboid area = zone.getPlayArea();

        int minX = area.getMinX() - 1;
        int maxX = area.getMaxX() + 1;
        int minZ = area.getMinZ() - 1;
        int maxZ = area.getMaxZ() + 1;
        int minY = area.getMinY() + 1;
        int maxY = area.getMaxY();

        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                clearIfBarrier(world, x, y, minZ);
                clearIfBarrier(world, x, y, maxZ);
            }
            for (int z = minZ; z <= maxZ; z++) {
                clearIfBarrier(world, minX, y, z);
                clearIfBarrier(world, maxX, y, z);
            }
        }
    }

    private void clearIfBarrier(World world, int x, int y, int z) {
        Block block = world.getBlockAt(x, y, z);
        if (block.getType() == Material.BARRIER) {
            block.setType(Material.AIR);
        }
    }


    private void fillWithBarriers() {
        GameZone zone = plugin.zone.getZone();
        World world = zone.getAnswerA().getWorld();
        Cuboid playArea = zone.getPlayArea();

        for (int x = playArea.getMinX(); x <= playArea.getMaxX(); x++) {
            for (int y = playArea.getMinY() + 1; y <= playArea.getMaxY(); y++) {
                for (int z = playArea.getMinZ(); z <= playArea.getMaxZ(); z++) {
                    Location loc = new Location(world, x, y, z);

                    boolean inAnswerZone = false;

                    if (zone.getAnswerA().contains(loc)) inAnswerZone = true;
                    else if (zone.getAnswerB().contains(loc)) inAnswerZone = true;
                    else if (zone.getAnswerC().contains(loc)) inAnswerZone = true;
                    else if (zone.getAnswerD().contains(loc)) inAnswerZone = true;

                    if (!inAnswerZone) {
                        loc.getBlock().setType(Material.BARRIER);
                    }
                }
            }
        }
    }

    public void removeBarriers() {
        GameZone zone = plugin.zone.getZone();
        World world = zone.getAnswerA().getWorld();
        Cuboid playArea = zone.getPlayArea();

        for (int x = playArea.getMinX(); x <= playArea.getMaxX(); x++) {
            for (int y = playArea.getMinY() + 1; y <= playArea.getMaxY(); y++) {
                for (int z = playArea.getMinZ(); z <= playArea.getMaxZ(); z++) {
                    Location loc = new Location(world, x, y, z);

                    if (loc.getBlock().getType() == Material.BARRIER) {
                        loc.getBlock().setType(Material.AIR);
                    }
                }
            }
        }
    }

    private void teleportToNearestZone(Player player) {
        GameZone zone = plugin.zone.getZone();
        Location loc = player.getLocation();

        Cuboid nearest = getNearestZone(loc, zone.getAnswerA(), zone.getAnswerB(), zone.getAnswerC(), zone.getAnswerD());

        Location target = getClosestPointInside(loc, nearest);
        target.setYaw(loc.getYaw());
        target.setPitch(loc.getPitch());

        player.teleport(target);
        plugin.utils.message(player, Sound.sound(SoundEventKeys.ENTITY_ENDERMAN_TELEPORT, Sound.Source.MASTER, 1, 1),
                "Has sido devuelto a la zona más cercana."
        );
    }

    private Cuboid getNearestZone(Location loc, Cuboid... zones) {
        Cuboid nearest = zones[0];
        double min = distanceToCuboid(loc, zones[0]);

        for (int i = 1; i < zones.length; i++) {
            double dist = distanceToCuboid(loc, zones[i]);
            if (dist < min) {
                min = dist;
                nearest = zones[i];
            }
        }
        return nearest;
    }

    private double distanceToCuboid(Location loc, Cuboid c) {
        double x = clamp(loc.getX(), c.getMinX(), c.getMaxX());
        double y = clamp(loc.getY(), c.getMinY(), c.getMaxY());
        double z = clamp(loc.getZ(), c.getMinZ(), c.getMaxZ());

        return loc.distance(new Location(loc.getWorld(), x, y, z));
    }

    private Location getClosestPointInside(Location loc, Cuboid c) {
        double x = clamp(loc.getX(), c.getMinX(), c.getMaxX());
        double y = clamp(loc.getY(), c.getMinY(), c.getMaxY());
        double z = clamp(loc.getZ(), c.getMinZ(), c.getMaxZ());

        return new Location(loc.getWorld(), x, y, z);
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private void spawnFireworks(Location location){
        Firework firework = (Firework) location.getWorld().spawnEntity(location, EntityType.FIREWORK_ROCKET);
        FireworkMeta meta = firework.getFireworkMeta();

        meta.setPower(0);
        meta.addEffect(FireworkEffect.builder().withColor(Color.FUCHSIA).flicker(true).build());

        firework.setFireworkMeta(meta);
        firework.detonate();

        for (int i = 0; i < 3; i++){
            plugin.utils.delay(i * 10, () -> {
                Firework spawnFirework = (Firework) location.getWorld().spawnEntity(location, EntityType.FIREWORK_ROCKET);
                spawnFirework.setFireworkMeta(meta);
            });
        }
    }

    public boolean isGameInactive() {
        return session == null;
    }

    public boolean isGameFinished() {
        return session.getState() == GameState.FINISHED;
    }
}