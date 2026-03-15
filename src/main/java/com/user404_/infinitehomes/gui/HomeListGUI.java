package com.user404_.infinitehomes.gui;

import com.user404_.infinitehomes.InfiniteHomes;
import com.user404_.infinitehomes.HomeData;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class HomeListGUI implements InventoryHolder {
    private final InfiniteHomes plugin;
    private final Player viewer;
    private final UUID targetUUID;
    private final boolean admin;
    private int page;
    private Inventory inventory;

    private static final int ITEMS_PER_PAGE = 45; // slots 0-44 for homes, last row for controls
    private static final int PREV_BUTTON_SLOT = 48;
    private static final int NEXT_BUTTON_SLOT = 50;
    private static final int CREATE_BUTTON_SLOT = 49;

    public HomeListGUI(InfiniteHomes plugin, Player viewer, UUID targetUUID, boolean admin, int page) {
        this.plugin = plugin;
        this.viewer = viewer;
        this.targetUUID = targetUUID;
        this.admin = admin;
        this.page = page;
        this.inventory = Bukkit.createInventory(this, 54, (admin ? "Admin Homes" : "Your Homes") + " - Page " + (page+1));
        populate();
    }

    private void populate() {
        Map<String, HomeData> homes = plugin.getHomes().get(targetUUID);
        if (homes == null) homes = new HashMap<>();
        List<String> homeNames = new ArrayList<>(homes.keySet());
        // Sort alphabetically
        homeNames.sort(String.CASE_INSENSITIVE_ORDER);

        int start = page * ITEMS_PER_PAGE;
        int end = Math.min(start + ITEMS_PER_PAGE, homeNames.size());

        for (int i = start; i < end; i++) {
            String name = homeNames.get(i);
            HomeData data = homes.get(name);
            ItemStack item = new ItemStack(data.getIcon());
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName("§e" + name);
            List<String> lore = new ArrayList<>();
            lore.add("§7Left-click to teleport");
            lore.add("§7Right-click to delete");
            lore.add("§7Shift-click to change icon");
            if (admin) {
                lore.add("§cAdmin mode");
            }
            meta.setLore(lore);
            item.setItemMeta(meta);
            inventory.setItem(i - start, item);
        }

        // Navigation buttons
        if (page > 0) {
            ItemStack prev = new ItemStack(Material.ARROW);
            ItemMeta prevMeta = prev.getItemMeta();
            prevMeta.setDisplayName("§aPrevious Page");
            prev.setItemMeta(prevMeta);
            inventory.setItem(PREV_BUTTON_SLOT, prev);
        }
        if (end < homeNames.size()) {
            ItemStack next = new ItemStack(Material.ARROW);
            ItemMeta nextMeta = next.getItemMeta();
            nextMeta.setDisplayName("§aNext Page");
            next.setItemMeta(nextMeta);
            inventory.setItem(NEXT_BUTTON_SLOT, next);
        }

        // Create home button
        ItemStack create = new ItemStack(Material.EMERALD);
        ItemMeta createMeta = create.getItemMeta();
        createMeta.setDisplayName("§aCreate New Home");
        createMeta.setLore(Collections.singletonList("§7Click then type name in chat"));
        create.setItemMeta(createMeta);
        inventory.setItem(CREATE_BUTTON_SLOT, create);

        // Fill empty slots with glass panes (optional)
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = filler.getItemMeta();
        fillerMeta.setDisplayName(" ");
        filler.setItemMeta(fillerMeta);
        for (int i = 45; i < 54; i++) {
            if (inventory.getItem(i) == null) {
                inventory.setItem(i, filler);
            }
        }
    }

    public void open() {
        viewer.openInventory(inventory);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void handleClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= inventory.getSize()) return;

        if (slot < ITEMS_PER_PAGE) {
            // Home item clicked
            int index = page * ITEMS_PER_PAGE + slot;
            Map<String, HomeData> homes = plugin.getHomes().get(targetUUID);
            if (homes == null) return;
            List<String> homeNames = new ArrayList<>(homes.keySet());
            homeNames.sort(String.CASE_INSENSITIVE_ORDER);
            if (index >= homeNames.size()) return;
            String homeName = homeNames.get(index);
            HomeData data = homes.get(homeName);

            if (event.isLeftClick() && !event.isShiftClick()) {
                // Teleport
                // Check cooldown for non-admin? Or admin ignores cooldown? We'll let admin bypass cooldown.
                if (!admin) {
                    int cooldown = plugin.getConfig().getInt("home-cooldown", -1);
                    if (cooldown > 0) {
                        long currentTime = System.currentTimeMillis();
                        Map<UUID, Long> cooldowns = plugin.getCooldowns();
                        if (cooldowns.containsKey(player.getUniqueId())) {
                            long lastUsed = cooldowns.get(player.getUniqueId());
                            long timeLeft = ((lastUsed / 1000) + cooldown) - (currentTime / 1000);
                            if (timeLeft > 0) {
                                player.sendMessage(plugin.getMessage(player, "home.cooldown").replace("{time}", String.valueOf(timeLeft)));
                                player.closeInventory();
                                return;
                            }
                        }
                        cooldowns.put(player.getUniqueId(), currentTime);
                    }
                }
                // NEW: Use TeleportManager
                plugin.getTeleportManager().requestTeleport(player, homeName, data.getLocation());
                player.closeInventory();
            } else if (event.isRightClick() && !event.isShiftClick()) {
                // Delete confirmation
                new ConfirmDeleteGUI(plugin, player, targetUUID, homeName, admin, page).open();
            } else if (event.isShiftClick()) {
                // Change icon
                new IconSelectionGUI(plugin, player, targetUUID, homeName, admin, page).open();
            }
        } else if (slot == PREV_BUTTON_SLOT) {
            new HomeListGUI(plugin, player, targetUUID, admin, page - 1).open();
        } else if (slot == NEXT_BUTTON_SLOT) {
            new HomeListGUI(plugin, player, targetUUID, admin, page + 1).open();
        } else if (slot == CREATE_BUTTON_SLOT) {
            // Create new home via chat prompt
            player.closeInventory();
            player.sendMessage("§aEnter the name of your new home in chat (30s timeout):");
            plugin.getGUIListener().addPendingCreation(player, targetUUID, admin);
            // schedule timeout (optional)
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                // remove if still pending (implement later)
            }, 600L); // 30 seconds
        }
    }
}