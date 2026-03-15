package com.user404_.infinitehomes;

import com.user404_.infinitehomes.gui.GUIListener;
import com.user404_.infinitehomes.gui.HomeListGUI;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.logging.Level;

public class InfiniteHomes extends JavaPlugin implements TabCompleter {

    private Map<UUID, Map<String, HomeData>> homes;
    private Map<UUID, Long> cooldowns;
    private FileConfiguration homesConfig;
    private File homesFile;
    private Map<String, FileConfiguration> translations;
    private File translationsDir;
    private GUIListener guiListener;
    private TeleportManager teleportManager;   // NEW

    @Override
    public void onEnable() {
        // Register HomeData for serialization
        ConfigurationSerialization.registerClass(HomeData.class);

        homes = new HashMap<>();
        cooldowns = new HashMap<>();
        translations = new HashMap<>();

        setupHomesConfig();
        // Force a reload after registration just to be safe
        try {
            homesConfig.load(homesFile);
        } catch (Exception e) {
            getLogger().severe("Could not reload homes.yml");
        }

        loadHomesFromConfig();
        setupTranslations();

        // Standardkonfiguration erstellen, falls nicht vorhanden
        getConfig().addDefault("max-homes", -1);
        getConfig().addDefault("home-cooldown", -1);
        // NEW default values
        getConfig().addDefault("teleport-delay", -1);
        getConfig().addDefault("teleport-delay-cancel-on-move", true);
        getConfig().options().copyDefaults(true);
        saveConfig();

        // TabCompleter registrieren
        getCommand("home").setTabCompleter(this);
        getCommand("delhome").setTabCompleter(this);

        // GUI Listener registrieren
        guiListener = new GUIListener(this);
        getServer().getPluginManager().registerEvents(guiListener, this);

        // NEW: TeleportManager registrieren
        teleportManager = new TeleportManager(this);
        getServer().getPluginManager().registerEvents(teleportManager, this);

        getLogger().info("InfiniteHomes plugin enabled!");
        getLogger().info("Please only use the official Version from User404/User404_/deutschich!");
        getLogger().info("Other Versions may not be safe!");
    }

    @Override
    public void onDisable() {
        saveHomesToConfig();
        getLogger().info("InfiniteHomes plugin disabled!");
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

        // Übersetzungen auf den Server spielen
        saveResource("translations/texts_en.yml", false);
        saveResource("translations/texts_de.yml", false);
        saveResource("translations/texts_es.yml", false);
        saveResource("translations/texts_fr.yml", false);
        saveResource("translations/texts_it.yml", false);
        saveResource("translations/texts_nl.yml", false);
        saveResource("translations/texts_pt.yml", false);
        saveResource("translations/texts_ru.yml", false);

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

    public String getMessage(Player player, String key) {
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
        homes.clear(); // Ensure map is empty before loading
        for (String playerUuidString : homesConfig.getKeys(false)) {
            try {
                UUID playerUuid = UUID.fromString(playerUuidString);
                Map<String, HomeData> playerHomes = new HashMap<>();

                if (homesConfig.isConfigurationSection(playerUuidString)) {
                    for (String homeName : homesConfig.getConfigurationSection(playerUuidString).getKeys(false)) {
                        try {
                            String path = playerUuidString + "." + homeName;
                            Object obj = homesConfig.get(path);

                            if (obj instanceof HomeData) {
                                playerHomes.put(homeName, (HomeData) obj);
                            } else if (obj instanceof org.bukkit.configuration.ConfigurationSection) {
                                // If it's a section but not yet a HomeData object,
                                // try to force deserialization from the Map
                                Map<String, Object> values = homesConfig.getConfigurationSection(path).getValues(false);
                                playerHomes.put(homeName, new HomeData(values));
                            }
                        } catch (Exception e) {
                            getLogger().warning("Failed to load home '" + homeName + "' for player " + playerUuidString);
                        }
                    }
                }

                if (!playerHomes.isEmpty()) {
                    homes.put(playerUuid, playerHomes);
                }
            } catch (Exception e) {
                getLogger().log(Level.WARNING, "Failed to load UUID: " + playerUuidString, e);
            }
        }
    }

    public void saveHomesToConfig() {
        try {
            // Clear existing data
            for (String key : homesConfig.getKeys(false)) {
                homesConfig.set(key, null);
            }

            // Save all homes in new format
            for (Map.Entry<UUID, Map<String, HomeData>> playerEntry : homes.entrySet()) {
                String playerUuidString = playerEntry.getKey().toString();
                for (Map.Entry<String, HomeData> homeEntry : playerEntry.getValue().entrySet()) {
                    String path = playerUuidString + "." + homeEntry.getKey();
                    homesConfig.set(path, homeEntry.getValue()); // ConfigurationSerializable will be stored as a section
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

            homes.get(playerUuid).put(homeName, new HomeData(player.getLocation(), Material.RED_BED));
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
            if (args.length == 0) {
                // Open GUI for player's own homes
                new HomeListGUI(this, player, playerUuid, false, 0).open();
                return true;
            }

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
                // NEW: Use TeleportManager instead of direct teleport
                teleportManager.requestTeleport(player, homeName, homes.get(playerUuid).get(homeName).getLocation());
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

        if (cmd.getName().equalsIgnoreCase("homeadmin")) {
            if (!player.hasPermission("infinitehomes.admin")) {
                player.sendMessage(getMessage(player, "no_permission"));
                return true;
            }
            if (args.length != 1) {
                player.sendMessage("§cUsage: /homeadmin <player>");
                return true;
            }
            String targetName = args[0];
            Player target = Bukkit.getPlayer(targetName);
            UUID targetUuid;
            if (target != null) {
                targetUuid = target.getUniqueId();
            } else {
                // Try offline player
                @SuppressWarnings("deprecation")
                OfflinePlayer offline = Bukkit.getOfflinePlayer(targetName);
                if (offline.hasPlayedBefore()) {
                    targetUuid = offline.getUniqueId();
                } else {
                    player.sendMessage("§cPlayer not found.");
                    return true;
                }
            }
            // Check if target has any homes
            if (!homes.containsKey(targetUuid) || homes.get(targetUuid).isEmpty()) {
                player.sendMessage("§cThat player has no homes.");
                return true;
            }
            new HomeListGUI(this, player, targetUuid, true, 0).open();
            return true;
        }

        // NEW: /htp and /htpc commands
        if (cmd.getName().equalsIgnoreCase("htp") || cmd.getName().equalsIgnoreCase("htpc")) {
            if (!player.hasPermission("infinitehomes.admin")) {
                player.sendMessage(getMessage(player, "no_permission"));
                return true;
            }
            if (args.length == 0) {
                player.sendMessage("§cUsage: /htp <seconds> [true|false]");
                return true;
            }
            try {
                int seconds = Integer.parseInt(args[0]);
                if (seconds < -1 || seconds > 60) {
                    player.sendMessage("§cSeconds must be between -1 and 60.");
                    return true;
                }
                boolean cancelOnMove = getConfig().getBoolean("teleport-delay-cancel-on-move", true);
                if (args.length >= 2) {
                    String bool = args[1].toLowerCase();
                    if (bool.equals("true") || bool.equals("false")) {
                        cancelOnMove = Boolean.parseBoolean(bool);
                    } else {
                        player.sendMessage("§cSecond argument must be true or false.");
                        return true;
                    }
                }
                // Save to config
                getConfig().set("teleport-delay", seconds);
                getConfig().set("teleport-delay-cancel-on-move", cancelOnMove);
                saveConfig();

                if (seconds == -1) {
                    player.sendMessage(getMessage(player, "teleport.set.disabled"));
                } else {
                    player.sendMessage(getMessage(player, "teleport.setup")
                            .replace("{time}", String.valueOf(seconds))
                            .replace("{cancel}", String.valueOf(cancelOnMove)));
                }
            } catch (NumberFormatException e) {
                player.sendMessage(getMessage(player, "invalid_number"));
            }
            return true;
        }

        return false;
    }

    // Getters for other classes
    public Map<UUID, Map<String, HomeData>> getHomes() {
        return homes;
    }

    public Map<UUID, Long> getCooldowns() {
        return cooldowns;
    }

    public GUIListener getGUIListener() {
        return guiListener;
    }

    // NEW getter for TeleportManager
    public TeleportManager getTeleportManager() {
        return teleportManager;
    }
}