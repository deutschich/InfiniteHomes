package com.user404_.infinitehomes;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

public class InfiniteHomes extends JavaPlugin {

    private Map<UUID, Map<String, Location>> homes;
    private FileConfiguration homesConfig;
    private File homesFile;

    @Override
    public void onEnable() {
        homes = new HashMap<>();
        setupHomesConfig();
        loadHomesFromConfig();
        getLogger().info("InfiniteHomes plugin enabled!");
    }

    @Override
    public void onDisable() {
        saveHomesToConfig();
        getLogger().info("InfiniteHomes plugin disabled!");
    }

    private void setupHomesConfig() {
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }

        homesFile = new File(getDataFolder(), "homes.yml");

        if (!homesFile.exists()) {
            try {
                homesFile.createNewFile();
            } catch (IOException e) {
                getLogger().log(Level.SEVERE, "Could not create homes.yml", e);
            }
        }

        homesConfig = YamlConfiguration.loadConfiguration(homesFile);
    }

    private void loadHomesFromConfig() {
        try {
            for (String playerUuidString : homesConfig.getKeys(false)) {
                UUID playerUuid = UUID.fromString(playerUuidString);
                Map<String, Location> playerHomes = new HashMap<>();

                for (String homeName : homesConfig.getConfigurationSection(playerUuidString).getKeys(false)) {
                    Location location = (Location) homesConfig.get(playerUuidString + "." + homeName);
                    playerHomes.put(homeName, location);
                }

                homes.put(playerUuid, playerHomes);
            }
        } catch (Exception e) {
            getLogger().log(Level.WARNING, "Error loading homes from config", e);
        }
    }

    private void saveHomesToConfig() {
        try {
            // Clear existing data
            for (String key : homesConfig.getKeys(false)) {
                homesConfig.set(key, null);
            }

            // Save all homes
            for (Map.Entry<UUID, Map<String, Location>> playerEntry : homes.entrySet()) {
                String playerUuidString = playerEntry.getKey().toString();

                for (Map.Entry<String, Location> homeEntry : playerEntry.getValue().entrySet()) {
                    String path = playerUuidString + "." + homeEntry.getKey();
                    homesConfig.set(path, homeEntry.getValue());
                }
            }

            homesConfig.save(homesFile);
        } catch (IOException e) {
            getLogger().log(Level.SEVERE, "Could not save homes to config", e);
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be executed by a player.");
            return true;
        }

        Player player = (Player) sender;
        UUID playerUuid = player.getUniqueId();

        if (cmd.getName().equalsIgnoreCase("sethome")) {
            if (args.length != 1) {
                player.sendMessage("Usage: /sethome <name>");
                return true;
            }

            // Check home limit
            int maxHomes = getConfig().getInt("max-homes", -1);
            if (maxHomes != -1) {
                int currentHomes = homes.containsKey(playerUuid) ? homes.get(playerUuid).size() : 0;
                if (currentHomes >= maxHomes) {
                    player.sendMessage("§cYou have reached the maximum number of homes (" + maxHomes + ").");
                    return true;
                }
            }

            String homeName = args[0].toLowerCase();
            if (!homes.containsKey(playerUuid)) {
                homes.put(playerUuid, new HashMap<>());
            }

            homes.get(playerUuid).put(homeName, player.getLocation());
            saveHomesToConfig();
            player.sendMessage("§aHome '" + homeName + "' set!");
            return true;
        }

        if (cmd.getName().equalsIgnoreCase("delhome")) {
            if (args.length != 1) {
                player.sendMessage("Usage: /delhome <name>");
                return true;
            }

            String homeName = args[0].toLowerCase();
            if (homes.containsKey(playerUuid) && homes.get(playerUuid).containsKey(homeName)) {
                homes.get(playerUuid).remove(homeName);
                saveHomesToConfig();
                player.sendMessage("§aHome '" + homeName + "' deleted!");
            } else {
                player.sendMessage("§cHome '" + homeName + "' does not exist.");
            }
            return true;
        }

        if (cmd.getName().equalsIgnoreCase("home")) {
            if (args.length != 1) {
                player.sendMessage("Usage: /home <name>");
                return true;
            }

            String homeName = args[0].toLowerCase();
            if (homes.containsKey(playerUuid) && homes.get(playerUuid).containsKey(homeName)) {
                player.teleport(homes.get(playerUuid).get(homeName));
                player.sendMessage("§aTeleported to home '" + homeName + "'!");
            } else {
                player.sendMessage("§cHome '" + homeName + "' does not exist.");
            }
            return true;
        }

        if (cmd.getName().equalsIgnoreCase("homecount")) {
            if (!player.isOp()) {
                player.sendMessage("§cYou do not have permission to use this command.");
                return true;
            }

            if (args.length != 1) {
                player.sendMessage("Usage: /homecount <number>");
                return true;
            }

            try {
                int newMax = Integer.parseInt(args[0]);
                getConfig().set("max-homes", newMax);
                saveConfig();
                player.sendMessage("§aGlobal home limit set to " + newMax + ".");
            } catch (NumberFormatException e) {
                player.sendMessage("§cPlease provide a valid number.");
            }
            return true;
        }

        return false;
    }
}