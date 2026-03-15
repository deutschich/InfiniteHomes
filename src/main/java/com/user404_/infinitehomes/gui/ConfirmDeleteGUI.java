package com.user404_.infinitehomes.gui;

import com.user404_.infinitehomes.InfiniteHomes;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class ConfirmDeleteGUI implements InventoryHolder {
    private final InfiniteHomes plugin;
    private final Player viewer;
    private final UUID targetUUID;
    private final String homeName;
    private final boolean admin;
    private final int returnPage;
    private Inventory inventory;

    public ConfirmDeleteGUI(InfiniteHomes plugin, Player viewer, UUID targetUUID, String homeName, boolean admin, int returnPage) {
        this.plugin = plugin;
        this.viewer = viewer;
        this.targetUUID = targetUUID;
        this.homeName = homeName;
        this.admin = admin;
        this.returnPage = returnPage;
        this.inventory = Bukkit.createInventory(this, 27, "Delete " + homeName + "?");
        populate();
    }

    private void populate() {
        // Confirm (green wool)
        ItemStack confirm = new ItemStack(Material.GREEN_WOOL);
        ItemMeta confirmMeta = confirm.getItemMeta();
        confirmMeta.setDisplayName("§a§lCONFIRM");
        confirm.setItemMeta(confirmMeta);
        inventory.setItem(11, confirm);

        // Cancel (red wool)
        ItemStack cancel = new ItemStack(Material.RED_WOOL);
        ItemMeta cancelMeta = cancel.getItemMeta();
        cancelMeta.setDisplayName("§c§lCANCEL");
        cancel.setItemMeta(cancelMeta);
        inventory.setItem(15, cancel);
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
        if (slot == 11) {
            // Confirm delete
            Map<String, ?> homes = plugin.getHomes().get(targetUUID);
            if (homes != null && homes.containsKey(homeName)) {
                homes.remove(homeName);
                plugin.saveHomesToConfig();
                player.sendMessage(plugin.getMessage(player, "home.deleted").replace("{home}", homeName));
            }
            player.closeInventory();
            // Optionally reopen home list
            new HomeListGUI(plugin, player, targetUUID, admin, returnPage).open();
        } else if (slot == 15) {
            // Cancel
            new HomeListGUI(plugin, player, targetUUID, admin, returnPage).open();
        }
    }
}