package com.user404_.infinitehomes;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

public class InfiniteHomes extends JavaPlugin implements TabCompleter {

    private Map<UUID, Map<String, Location>> homes;
    private Map<UUID, Long> cooldowns;
    private FileConfiguration homesConfig;
    private File homesFile;
    private Map<String, FileConfiguration> translations;
    private File translationsDir;

    @Override
    public void onEnable() {
        homes = new HashMap<>();
        cooldowns = new HashMap<>();
        translations = new HashMap<>();

        setupHomesConfig();
        loadHomesFromConfig();
        setupTranslations();

        // Standardkonfiguration erstellen, falls nicht vorhanden
        getConfig().addDefault("max-homes", -1);
        getConfig().addDefault("home-cooldown", -1);
        getConfig().options().copyDefaults(true);
        saveConfig();

        // TabCompleter registrieren
        getCommand("home").setTabCompleter(this);
        getCommand("delhome").setTabCompleter(this);

        getLogger().info("InfiniteHomes plugin by User404 enabled!");
    }

    @Override
    public void onDisable() {
        saveHomesToConfig();
        getLogger().info("InfiniteHomes plugin by User404 disabled!");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        // Nur für Spieler und für die Befehle home und delhome
        if (!(sender instanceof Player) || (!command.getName().equalsIgnoreCase("home") &&
                !command.getName().equalsIgnoreCase("delhome"))) {
            return completions;
        }

        Player player = (Player) sender;
        UUID playerUuid = player.getUniqueId();

        // Wenn der Spieler keine Homes hat, leere Liste zurückgeben
        if (!homes.containsKey(playerUuid) || homes.get(playerUuid).isEmpty()) {
            return completions;
        }

        // Home-Namen des Spielers holen
        Set<String> homeNames = homes.get(playerUuid).keySet();

        // Wenn kein Argument vorhanden ist, alle Home-Namen zurückgeben
        if (args.length == 0 || args[0].isEmpty()) {
            completions.addAll(homeNames);
        } else {
            // Home-Namen filtern, die mit dem eingegebenen Text beginnen
            String input = args[0].toLowerCase();
            for (String home : homeNames) {
                if (home.toLowerCase().startsWith(input)) {
                    completions.add(home);
                }
            }
        }

        return completions;
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

    private void setupTranslations() {
        translationsDir = new File(getDataFolder(), "translations");
        if (!translationsDir.exists()) {
            translationsDir.mkdirs();
        }

        // Standard-Übersetzung (Englisch) aus Ressourcen laden
        saveResource("translations/texts_en.yml", false);

        // Verfügbare Übersetzungen laden
        loadTranslations();
    }

    private void loadTranslations() {
        translations.clear();

        File[] translationFiles = translationsDir.listFiles((dir, name) ->
                name.startsWith("texts_") && name.endsWith(".yml"));

        if (translationFiles != null) {
            for (File file : translationFiles) {
                try {
                    String locale = file.getName().replace("texts_", "").replace(".yml", "");
                    FileConfiguration config = YamlConfiguration.loadConfiguration(file);
                    translations.put(locale, config);
                    getLogger().info("Loaded translation: " + locale);
                } catch (Exception e) {
                    getLogger().log(Level.WARNING, "Error loading translation file: " + file.getName(), e);
                }
            }
        }

        // Fallback: Englische Übersetzung aus Ressourcen laden
        if (!translations.containsKey("en")) {
            try {
                InputStream stream = getResource("translations/texts_en.yml");
                if (stream != null) {
                    FileConfiguration config = YamlConfiguration.loadConfiguration(
                            new InputStreamReader(stream, StandardCharsets.UTF_8));
                    translations.put("en", config);
                }
            } catch (Exception e) {
                getLogger().log(Level.WARNING, "Error loading default English translation", e);
            }
        }
    }

    private String getMessage(Player player, String key) {
        // Sprache des Clients ermitteln
        String clientLanguage = player.getLocale().toLowerCase();

        // Sprache auf Standardformat normalisieren (z.B. "de_de" -> "de")
        String languageCode = clientLanguage.split("_")[0];

        // Passende Übersetzung finden
        FileConfiguration translation = translations.get(languageCode);
        if (translation == null) {
            // Fallback: Englisch
            translation = translations.get("en");
            if (translation == null) {
                return "Translation not found for key: " + key;
            }
        }

        // Nachricht aus der Übersetzung holen
        String message = translation.getString(key);
        if (message == null || message.isEmpty()) {
            // Fallback: Englisch
            FileConfiguration enTranslation = translations.get("en");
            if (enTranslation != null) {
                message = enTranslation.getString(key);
            }

            if (message == null || message.isEmpty()) {
                return "Message not found: " + key;
            }
        }

        return message;
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
                player.sendMessage(getMessage(player, "usage.sethome"));
                return true;
            }

            // Check home limit
            int maxHomes = getConfig().getInt("max-homes", -1);
            if (maxHomes != -1) {
                int currentHomes = homes.containsKey(playerUuid) ? homes.get(playerUuid).size() : 0;
                if (currentHomes >= maxHomes) {
                    player.sendMessage(getMessage(player, "homes.limit.reached").replace("{max}", String.valueOf(maxHomes)));
                    return true;
                }
            }

            String homeName = args[0].toLowerCase();
            if (!homes.containsKey(playerUuid)) {
                homes.put(playerUuid, new HashMap<>());
            }

            homes.get(playerUuid).put(homeName, player.getLocation());
            saveHomesToConfig();
            player.sendMessage(getMessage(player, "home.set").replace("{home}", homeName));
            return true;
        }

        if (cmd.getName().equalsIgnoreCase("delhome")) {
            if (args.length != 1) {
                player.sendMessage(getMessage(player, "usage.delhome"));
                return true;
            }

            String homeName = args[0].toLowerCase();
            if (homes.containsKey(playerUuid) && homes.get(playerUuid).containsKey(homeName)) {
                homes.get(playerUuid).remove(homeName);
                saveHomesToConfig();
                player.sendMessage(getMessage(player, "home.deleted").replace("{home}", homeName));
            } else {
                player.sendMessage(getMessage(player, "home.not_exist").replace("{home}", homeName));
            }
            return true;
        }

        if (cmd.getName().equalsIgnoreCase("home")) {
            if (args.length != 1) {
                player.sendMessage(getMessage(player, "usage.home"));
                return true;
            }

            // Check cooldown
            int cooldown = getConfig().getInt("home-cooldown", -1);
            if (cooldown > 0) {
                long currentTime = System.currentTimeMillis();
                if (cooldowns.containsKey(playerUuid)) {
                    long lastUsed = cooldowns.get(playerUuid);
                    long timeLeft = ((lastUsed / 1000) + cooldown) - (currentTime / 1000);

                    if (timeLeft > 0) {
                        player.sendMessage(getMessage(player, "home.cooldown").replace("{time}", String.valueOf(timeLeft)));
                        return true;
                    }
                }

                // Set cooldown
                cooldowns.put(playerUuid, currentTime);
            }

            String homeName = args[0].toLowerCase();
            if (homes.containsKey(playerUuid) && homes.get(playerUuid).containsKey(homeName)) {
                player.teleport(homes.get(playerUuid).get(homeName));
                player.sendMessage(getMessage(player, "home.teleport").replace("{home}", homeName));
            } else {
                player.sendMessage(getMessage(player, "home.not_exist").replace("{home}", homeName));
            }
            return true;
        }

        if (cmd.getName().equalsIgnoreCase("homes")) {
            // List all homes of the player
            if (!homes.containsKey(playerUuid) || homes.get(playerUuid).isEmpty()) {
                player.sendMessage(getMessage(player, "homes.none"));
                return true;
            }

            Set<String> homeNames = homes.get(playerUuid).keySet();
            int maxHomes = getConfig().getInt("max-homes", -1);
            int currentHomes = homeNames.size();

            String limitText = (maxHomes == -1) ? getMessage(player, "homes.unlimited") : String.valueOf(maxHomes);
            player.sendMessage(getMessage(player, "homes.list.header")
                    .replace("{current}", String.valueOf(currentHomes))
                    .replace("{max}", limitText));

            // List all home names
            StringBuilder homesList = new StringBuilder();
            for (String home : homeNames) {
                if (homesList.length() > 0) {
                    homesList.append(", ");
                }
                homesList.append(home);
            }

            player.sendMessage(getMessage(player, "homes.list.items").replace("{homes}", homesList.toString()));
            return true;
        }

        if (cmd.getName().equalsIgnoreCase("homecount")) {
            if (!player.isOp()) {
                player.sendMessage(getMessage(player, "no_permission"));
                return true;
            }

            if (args.length != 1) {
                player.sendMessage(getMessage(player, "usage.homecount"));
                return true;
            }

            try {
                int newMax = Integer.parseInt(args[0]);
                getConfig().set("max-homes", newMax);
                saveConfig();
                player.sendMessage(getMessage(player, "homes.limit.set").replace("{max}", String.valueOf(newMax)));
            } catch (NumberFormatException e) {
                player.sendMessage(getMessage(player, "invalid_number"));
            }
            return true;
        }

        if (cmd.getName().equalsIgnoreCase("homecooldown")) {
            if (!player.isOp()) {
                player.sendMessage(getMessage(player, "no_permission"));
                return true;
            }

            if (args.length != 1) {
                player.sendMessage(getMessage(player, "usage.homecooldown"));
                return true;
            }

            try {
                int cooldown = Integer.parseInt(args[0]);
                if (cooldown < -1 || cooldown > 60) {
                    player.sendMessage(getMessage(player, "cooldown.range"));
                    return true;
                }

                getConfig().set("home-cooldown", cooldown);
                saveConfig();

                if (cooldown == -1) {
                    player.sendMessage(getMessage(player, "cooldown.disabled"));
                } else {
                    player.sendMessage(getMessage(player, "cooldown.set").replace("{time}", String.valueOf(cooldown)));
                }
            } catch (NumberFormatException e) {
                player.sendMessage(getMessage(player, "invalid_number"));
            }
            return true;
        }

        return false;
    }
}