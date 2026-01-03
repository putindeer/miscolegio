package me.putindeer.miscolegio.comodin;

import io.papermc.paper.registry.keys.SoundEventKeys;
import me.putindeer.miscolegio.Main;
import me.putindeer.miscolegio.game.GamePlayer;
import me.putindeer.miscolegio.game.GameSession;
import me.putindeer.miscolegio.game.GameState;
import me.putindeer.miscolegio.zone.ZoneLocation;
import net.kyori.adventure.sound.Sound;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

public class ComodinEvents implements Listener {
    private final Main plugin;
    private final ComodinManager comodin;
    public ComodinEvents(Main plugin, ComodinManager comodin) {
        this.plugin = plugin;
        this.comodin = comodin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public void disable() {
        HandlerList.unregisterAll(this);
    }

    @EventHandler
    public void onPvP(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;
        if (!(event.getDamager() instanceof Player attacker)) return;
        if (plugin.game.isGameInactive() || plugin.game.isGameFinished()) return;
        GameSession session = plugin.game.getSession();
        GamePlayer gamePlayer = session.getPlayer(attacker.getUniqueId());
        if (gamePlayer == null || !gamePlayer.isAlive()) return;
        if (session.getState() != GameState.ANSWERING) return;
        if (gamePlayer.isComodinCooldown()) return;

        ItemStack itemInHand = attacker.getInventory().getItemInMainHand();

        if (itemInHand.isSimilar(ComodinType.EMPUJAR.buildItem())) {
            event.setDamage(0);
            event.setCancelled(false);
            plugin.utils.delay(2, () -> comodin.consumeItem(gamePlayer, itemInHand));
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (item == null) return;

        ComodinType type = comodin.getComodinType(item);
        if (type == null) return;

        event.setCancelled(true);

        if (plugin.game.isGameInactive() || plugin.game.isGameFinished()) {
            plugin.utils.message(player, "<red>¡Solo puedes usar comodines durante una partida!");
            return;
        }

        GameSession session = plugin.game.getSession();
        GamePlayer gamePlayer = session.getPlayer(player.getUniqueId());

        if (gamePlayer == null || !gamePlayer.isAlive()) {
            plugin.utils.message(player, "<red>¡No estás en la partida o ya fuiste eliminado!");
            return;
        }

        if (session.getState() != GameState.ANSWERING) {
            plugin.utils.message(player, "<red>¡Solo puedes usar comodines mientras respondes una pregunta!");
            return;
        }

        if (gamePlayer.isComodinCooldown()) {
            plugin.utils.message(player, "<red>¡Espera un poco antes de usar otro comodín!");
            return;
        }

        switch (type) {
            case SILENCIO -> useSilencio(gamePlayer, item);
            case COPIAR -> useCopiar(gamePlayer, item, session);
            case CAMBIO_ASIENTO -> useCambioAsiento(gamePlayer, item, session);
            case BANO -> useBano(gamePlayer, item, session);
            case RECUPERATIVA -> useRecuperativa(gamePlayer, item);
        }
    }

    private void useSilencio(GamePlayer gamePlayer, ItemStack item) {
        GameSession session = plugin.game.getSession();

        session.getAliveOnlinePlayersWithoutBathroomAsPlayerList().forEach(
                p -> p.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 200, 0, false, false)));

        comodin.broadcastComodinUse(gamePlayer, "Silencio en la clase", "¡Todos son invisibles por 10 segundos!");
        comodin.consumeItem(gamePlayer, item);
    }

    private void useCopiar(GamePlayer gamePlayer, ItemStack item, GameSession session) {
        Player player = gamePlayer.getPlayer();
        if (player == null) return;
        GamePlayer maxLivesPlayer = session.getAliveOnlinePlayersWithoutBathroom().stream()
                .filter(gp -> !gp.getUniqueId().equals(player.getUniqueId()))
                .max(Comparator.comparingInt(GamePlayer::getLives))
                .orElse(null);

        if (maxLivesPlayer == null) {
            plugin.utils.message(player, "<red>No hay otros jugadores para copiar!");
            return;
        }

        Player target = maxLivesPlayer.getPlayer();
        if (target == null) {
            plugin.utils.message(player, "<red>Algo salió mal. Intenta usar el comodín de nuevo.");
            return;
        }

        player.teleport(target.getLocation());

        plugin.utils.message(player, "<green>¡Has copiado la posición de " + target.getName() + "!");
        plugin.utils.message(target, "<yellow>" + player.getName() + " te está copiando!");

        comodin.consumeItem(gamePlayer, item);
    }

    private void useCambioAsiento(GamePlayer gamePlayer, ItemStack item, GameSession session) {
        Player player = gamePlayer.getPlayer();
        if (player == null) return;
        List<Player> alivePlayers = session.getAliveOnlinePlayersWithoutBathroom().stream().map(GamePlayer::getPlayer).toList();

        if (alivePlayers.size() < 2) {
            plugin.utils.message(player, "<red>No hay suficientes jugadores para intercambiar!");
            return;
        }

        Random random = new Random();
        Player player1 = alivePlayers.get(random.nextInt(alivePlayers.size()));
        Player player2 = alivePlayers.stream().filter(newPlayer -> newPlayer != player1).toList().get(random.nextInt(alivePlayers.size() - 1));

        if (player1 == null || player2 == null) {
            plugin.utils.message(player, "<red>Error al intercambiar jugadores!");
            return;
        }

        Location loc1 = player1.getLocation().clone();
        Location loc2 = player2.getLocation().clone();

        player1.teleport(loc2);
        player2.teleport(loc1);

        comodin.broadcastComodinUse(gamePlayer, "Cambio de asiento",
                player1.getName() + " y " + player2.getName() + " han intercambiado posiciones!");

        comodin.consumeItem(gamePlayer, item);
    }

    private void useBano(GamePlayer gamePlayer, ItemStack item, GameSession session) {
        Player player = gamePlayer.getPlayer();
        if (player == null) return;

        player.teleport(session.getBathroomLocation());

        gamePlayer.setCurrentAnswer(ZoneLocation.BATHROOM);

        plugin.utils.message(player, "<green>¡Has evitado esta pregunta! Volverás en la siguiente ronda.");

        comodin.consumeItem(gamePlayer, item);
    }

    private void useRecuperativa(GamePlayer gamePlayer, ItemStack item) {
        Player player = gamePlayer.getPlayer();
        if (player == null) return;
        gamePlayer.addLife();
        plugin.utils.message(player.getPlayer(), Sound.sound(SoundEventKeys.ENTITY_PLAYER_LEVELUP, Sound.Source.MASTER, 1, 1),
                "<green>✓ ¡Has ganado una vida adicional!",
                "<yellow>Vidas actuales: <white>" + gamePlayer.getLives());

        comodin.consumeItem(gamePlayer, item);
    }
}
