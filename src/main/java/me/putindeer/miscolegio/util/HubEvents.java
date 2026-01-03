package me.putindeer.miscolegio.util;

import me.putindeer.miscolegio.Main;
import org.bukkit.GameMode;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.Objects;

public class HubEvents implements Listener {
    private final Main plugin;
    public HubEvents(Main plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.CREATIVE) return;
        if (!plugin.game.isGameInactive()) return;
        player.getInventory().clear();
        player.setGameMode(GameMode.ADVENTURE);
        Objects.requireNonNull(player.getAttribute(Attribute.MAX_HEALTH)).setBaseValue(20);
        plugin.utils.setMaxHealth(player);
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player) {
            if (!(event instanceof EntityDamageByEntityEvent)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPvP(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof Player) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onHunger(FoodLevelChangeEvent event) {
        if (event.getEntity() instanceof Player player) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onItemDrop(PlayerDropItemEvent event) {
        if (event.getPlayer().getGameMode().equals(GameMode.CREATIVE)) return;

        event.setCancelled(true);
    }
}
