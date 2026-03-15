package com.user404_.infinitehomes;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TeleportManager implements Listener {
    private final InfiniteHomes plugin;
    private final Map<UUID, PendingTeleport> pendingTeleports = new HashMap<>();

    public TeleportManager(InfiniteHomes plugin) {
        this.plugin = plugin;
    }

    public void requestTeleport(Player player, String homeName, Location target) {
        cancelPending(player.getUniqueId(), "new request");

        int delay = plugin.getConfig().getInt("teleport-delay", -1);
        if (delay <= 0) {
            player.teleport(target);
            player.sendMessage(plugin.getMessage(player, "home.teleport").replace("{home}", homeName));
            return;
        }

        boolean cancelOnMove = plugin.getConfig().getBoolean("teleport-delay-cancel-on-move", true);

        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                if (pendingTeleports.containsKey(player.getUniqueId())) {
                    player.teleport(target);
                    player.sendMessage(plugin.getMessage(player, "home.teleport").replace("{home}", homeName));
                    pendingTeleports.remove(player.getUniqueId());
                }
            }
        }.runTaskLater(plugin, delay * 20L);

        PendingTeleport pending = new PendingTeleport(player.getUniqueId(), target, homeName, cancelOnMove, task);
        pendingTeleports.put(player.getUniqueId(), pending);

        String cancelMsg = cancelOnMove ? plugin.getMessage(player, "teleport.cancel_on_move") : "";
        player.sendMessage(plugin.getMessage(player, "teleport.delayed")
                .replace("{home}", homeName)
                .replace("{time}", String.valueOf(delay))
                .replace("{cancelMessage}", cancelMsg));
    }

    public void cancelPending(UUID playerId, String reason) {
        PendingTeleport pending = pendingTeleports.remove(playerId);
        if (pending != null) {
            pending.task.cancel();
            Player player = plugin.getServer().getPlayer(playerId);
            if (player != null && player.isOnline()) {
                if (reason.equals("move") || reason.equals("damage")) {
                    String msgKey = reason.equals("move") ? "teleport.cancelled.move" : "teleport.cancelled.damage";
                    player.sendMessage(plugin.getMessage(player, "teleport.cancelled-m")
                            .replace("{reason}", plugin.getMessage(player, msgKey)));
                }
            }
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        PendingTeleport pending = pendingTeleports.get(player.getUniqueId());
        if (pending != null && pending.cancelOnMove) {
            Location from = event.getFrom();
            Location to = event.getTo();
            if (from.getBlockX() != to.getBlockX() || from.getBlockY() != to.getBlockY() || from.getBlockZ() != to.getBlockZ()) {
                cancelPending(player.getUniqueId(), "move");
            }
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            PendingTeleport pending = pendingTeleports.get(player.getUniqueId());
            if (pending != null && pending.cancelOnMove) {
                cancelPending(player.getUniqueId(), "damage");
            }
        }
    }

    private static class PendingTeleport {
        UUID playerId;
        Location target;
        String homeName;
        boolean cancelOnMove;
        BukkitTask task;

        PendingTeleport(UUID playerId, Location target, String homeName, boolean cancelOnMove, BukkitTask task) {
            this.playerId = playerId;
            this.target = target;
            this.homeName = homeName;
            this.cancelOnMove = cancelOnMove;
            this.task = task;
        }
    }
}