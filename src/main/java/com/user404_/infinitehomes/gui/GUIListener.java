package com.user404_.infinitehomes.gui;

import com.user404_.infinitehomes.InfiniteHomes;
import com.user404_.infinitehomes.HomeData;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class GUIListener implements Listener {
    private final InfiniteHomes plugin;
    private final Map<UUID, PendingCreation> pendingCreations = new HashMap<>(); // player -> (targetUUID, admin)

    public GUIListener(InfiniteHomes plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        Inventory inv = event.getInventory();
        InventoryHolder holder = inv.getHolder();

        if (holder instanceof HomeListGUI) {
            event.setCancelled(true);
            ((HomeListGUI) holder).handleClick(event);
        } else if (holder instanceof IconSelectionGUI) {
            event.setCancelled(true);
            ((IconSelectionGUI) holder).handleClick(event);
        } else if (holder instanceof ConfirmDeleteGUI) {
            event.setCancelled(true);
            ((ConfirmDeleteGUI) holder).handleClick(event);
        }
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        PendingCreation pending = pendingCreations.get(player.getUniqueId());
        if (pending == null) return;

        event.setCancelled(true);
        String homeName = event.getMessage().trim().toLowerCase();
        if (homeName.isEmpty()) {
            player.sendMessage("§cHome name cannot be empty.");
            pendingCreations.remove(player.getUniqueId());
            return;
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                // Check limit
                int maxHomes = plugin.getConfig().getInt("max-homes", -1);
                Map<String, HomeData> playerHomes = plugin.getHomes().get(pending.targetUUID);
                if (playerHomes == null) playerHomes = new HashMap<>();
                if (maxHomes != -1 && playerHomes.size() >= maxHomes) {
                    player.sendMessage(plugin.getMessage(player, "homes.limit.reached").replace("{max}", String.valueOf(maxHomes)));
                    pendingCreations.remove(player.getUniqueId());
                    return;
                }
                // Check if name already exists
                if (playerHomes.containsKey(homeName)) {
                    player.sendMessage("§cA home with that name already exists.");
                    pendingCreations.remove(player.getUniqueId());
                    return;
                }
                // Create home at player's current location (must be sync)
                playerHomes.put(homeName, new HomeData(player.getLocation(), Material.RED_BED));
                plugin.getHomes().put(pending.targetUUID, playerHomes);
                plugin.saveHomesToConfig();
                player.sendMessage(plugin.getMessage(player, "home.set").replace("{home}", homeName));

                // Reopen GUI for the target player (if viewer is same as target, or admin)
                if (pending.admin) {
                    new HomeListGUI(plugin, player, pending.targetUUID, true, 0).open();
                } else {
                    new HomeListGUI(plugin, player, player.getUniqueId(), false, 0).open();
                }
                pendingCreations.remove(player.getUniqueId());
            }
        }.runTask(plugin);
    }

    public void addPendingCreation(Player player, UUID targetUUID, boolean admin) {
        pendingCreations.put(player.getUniqueId(), new PendingCreation(targetUUID, admin));
    }

    private static class PendingCreation {
        UUID targetUUID;
        boolean admin;
        PendingCreation(UUID targetUUID, boolean admin) {
            this.targetUUID = targetUUID;
            this.admin = admin;
        }
    }
}