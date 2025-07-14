package me.f0reach.holofans.earnings.tutorial;

import me.f0reach.holofans.earnings.HolofansEarnings;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;
import java.util.UUID;

public class DataStore {
    private final HolofansEarnings plugin;
    private boolean isAvailable = false;

    public DataStore(HolofansEarnings plugin) {
        this.plugin = plugin;

        prepareDataDirectory();
    }

    private Path getDataFolder() {
        return plugin.getDataFolder().toPath().resolve("tutorial");
    }

    private void prepareDataDirectory() {
        var dataDir = plugin.getDataFolder();
        if (!dataDir.exists() && !dataDir.mkdirs()) {
            plugin.getLogger().severe("Failed to create plugin data directory: " + dataDir.getAbsolutePath());
            return;
        }

        var tutorialUserDirPath = plugin.getDataFolder().toPath().resolve("tutorial");
        var tutorialUserDir = tutorialUserDirPath.toFile();
        if (!tutorialUserDir.exists() && !tutorialUserDir.mkdirs()) {
            plugin.getLogger().severe("Failed to create tutorial user directory: " + tutorialUserDirPath);
            return;
        }

        isAvailable = true;
    }

    public boolean isAvailable() {
        return isAvailable;
    }

    public Set<String> getCompletedTutorials(UUID playerUUID) {
        if (!isAvailable) {
            plugin.getLogger().warning("Tutorial data store is not available.");
            return Set.of();
        }

        var tutorialFile = getDataFolder().resolve(playerUUID + ".yaml").toFile();
        if (!tutorialFile.exists()) {
            return Set.of();
        }

        try {
            var config = YamlConfiguration.loadConfiguration(tutorialFile);
            var list = config.getStringList("completedTutorials");
            return Set.copyOf(list);
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to load tutorial data for player " + playerUUID + ": " + e.getMessage());
            return Set.of();
        }
    }

    /**
     * Adds a completed tutorial for the player.
     *
     * @param playerUUID   The UUID of the player.
     * @param tutorialName The name of the tutorial to add.
     * @return true if the tutorial was added, false if it was already completed.
     */
    public boolean addCompletedTutorial(UUID playerUUID, String tutorialName) {
        if (!isAvailable) {
            plugin.getLogger().warning("Tutorial data store is not available.");
            return false;
        }

        var tutorialFile = getDataFolder().resolve(playerUUID + ".yaml").toFile();
        var config = new YamlConfiguration();
        if (tutorialFile.exists()) {
            try {
                config.load(tutorialFile);
            } catch (IOException e) {
                plugin.getLogger().severe("Failed to load tutorial data for player " + playerUUID + ": " + e.getMessage());
                return false;
            } catch (InvalidConfigurationException e) {
                plugin.getLogger().severe("Invalid data for player " + playerUUID + ": " + e.getMessage());
                return false;
            }
        }

        // TODO: 実態がArrayListであることを保証していない
        var completedTutorials = config.getStringList("completedTutorials");
        if (completedTutorials.contains(tutorialName)) {
            return false;
        }

        completedTutorials.add(tutorialName);
        config.set("completedTutorials", completedTutorials);

        try {
            config.save(tutorialFile);
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to save tutorial data for player " + playerUUID + ": " + e.getMessage());
        }

        return true;
    }
}
