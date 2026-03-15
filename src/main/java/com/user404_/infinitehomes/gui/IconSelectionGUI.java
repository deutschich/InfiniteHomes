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

public class IconSelectionGUI implements InventoryHolder {
    private final InfiniteHomes plugin;
    private final Player viewer;
    private final UUID targetUUID;
    private final String homeName;
    private final boolean admin;
    private final int returnPage;
    private Inventory inventory;

    // A set of common materials for icons (updated for 1.21)
    private static final List<Material> ICONS = Arrays.asList(
            Material.RED_BED, Material.DIAMOND, Material.EMERALD, Material.GOLD_INGOT,
            Material.IRON_INGOT, Material.NETHERITE_INGOT, Material.OAK_LOG,
            Material.STONE, Material.GRASS_BLOCK, Material.DIRT, Material.COBBLESTONE,
            Material.OAK_PLANKS, Material.BRICK, Material.BOOKSHELF, Material.CHEST,
            Material.FURNACE, Material.CRAFTING_TABLE, Material.ENCHANTING_TABLE,
            Material.ANVIL, Material.BEACON, Material.ENDER_CHEST, Material.SPAWNER,
            Material.DRAGON_EGG, Material.TOTEM_OF_UNDYING, Material.HEART_OF_THE_SEA,
            Material.NAUTILUS_SHELL, Material.TURTLE_SCUTE, Material.PHANTOM_MEMBRANE,
            Material.FEATHER, Material.ARROW, Material.BOW, Material.CROSSBOW,
            Material.TRIDENT, Material.SHIELD, Material.ELYTRA, Material.FIREWORK_ROCKET
    );

    public IconSelectionGUI(InfiniteHomes plugin, Player viewer, UUID targetUUID, String homeName, boolean admin, int returnPage) {
        this.plugin = plugin;
        this.viewer = viewer;
        this.targetUUID = targetUUID;
        this.homeName = homeName;
        this.admin = admin;
        this.returnPage = returnPage;
        this.inventory = Bukkit.createInventory(this, 54, "Select Icon for " + homeName);
        populate();
    }

    private void populate() {
        int slot = 0;
        for (Material mat : ICONS) {
            if (slot >= 54) break;
            ItemStack item = new ItemStack(mat);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName("§e" + mat.name());
            item.setItemMeta(meta);
            inventory.setItem(slot++, item);
        }
        // Back button
        ItemStack back = new ItemStack(Material.BARRIER);
        ItemMeta backMeta = back.getItemMeta();
        backMeta.setDisplayName("§cBack");
        back.setItemMeta(backMeta); // FIXED: use backMeta, not back
        inventory.setItem(53, back);
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

        if (slot == 53) {
            // Back to home list
            new HomeListGUI(plugin, player, targetUUID, admin, returnPage).open();
            return;
        }

        ItemStack clicked = inventory.getItem(slot);
        if (clicked == null || clicked.getType() == Material.AIR) return;

        Material newIcon = clicked.getType();
        // Update home data
        Map<String, HomeData> homes = plugin.getHomes().get(targetUUID);
        if (homes != null && homes.containsKey(homeName)) {
            HomeData data = homes.get(homeName);
            data.setIcon(newIcon);
            plugin.saveHomesToConfig();
            player.sendMessage("§aIcon for home '" + homeName + "' updated.");
        }
        // Return to home list
        new HomeListGUI(plugin, player, targetUUID, admin, returnPage).open();
    }
}