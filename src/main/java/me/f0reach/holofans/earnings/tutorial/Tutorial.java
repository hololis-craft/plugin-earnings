package me.f0reach.holofans.earnings.tutorial;

import me.f0reach.holofans.earnings.HolofansEarnings;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class Tutorial implements CommandExecutor, TabCompleter {
    private final HolofansEarnings plugin;
    private final DataStore dataStore;
    private final Config config;

    private final String LIST_OTHER_PERMISSION = "holofans.tutorial.list.other";

    public Tutorial(HolofansEarnings plugin) {
        this.plugin = plugin;
        this.dataStore = new DataStore(plugin);
        this.config = new Config(plugin);

        Objects.requireNonNull(plugin.getCommand("tutorial")).setExecutor(this);
        Objects.requireNonNull(plugin.getCommand("tutorial")).setTabCompleter(this);
    }

    public void reloadConfig() {
        config.reload();
    }

    private UUID getPlayerUUID(String playerName) {
        Player player = plugin.getServer().getPlayer(playerName);
        if (player != null) {
            return player.getUniqueId();
        }
        return plugin.getServer().getOfflinePlayer(playerName).getUniqueId();
    }

    private void checkComplete(Player player) {
        if (!dataStore.isAvailable() || config.getTutorialList().isEmpty()) {
            return;
        }
        var completedTutorials = dataStore.getCompletedTutorials(player.getUniqueId());
        var allCompleted = completedTutorials.containsAll(config.getTutorialList());

        if (allCompleted) {
            for (String command : config.getCompleteCommands()) {
                // Replace placeholders in the command if necessary
                String finalCommand = command.replace("{player}", player.getName())
                        .replace("{uuid}", player.getUniqueId().toString());
                // Dispatch the command as console
                plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), finalCommand);
            }
        }
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be used by players.");
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage("Usage: /tutorial <complete|list> [tutorialName]");
            return true;
        }

        if (args[0].equalsIgnoreCase("complete")) {
            if (args.length < 2) {
                sender.sendMessage("Usage: /tutorial complete <tutorialName>");
                return true;
            }
            String tutorialName = args[1];
            if (!config.getTutorialList().contains(tutorialName)) {
                sender.sendMessage("Unknown tutorial: " + tutorialName);
                return true;
            }

            // Logic to mark the tutorial as completed
            if (dataStore.addCompletedTutorial(player.getUniqueId(), tutorialName)) {
                checkComplete(player);
            }

            return true;
        } else if (args[0].equalsIgnoreCase("list")) {
            if (args.length == 2 && sender.hasPermission(LIST_OTHER_PERMISSION)) {
                var uuid = getPlayerUUID(args[1]);
                var completedTutorials = dataStore.getCompletedTutorials(uuid);
                sender.sendMessage("Completed Tutorials for " + uuid + ": " + String.join(", ", completedTutorials));
                return true;
            } else {
                var completedTutorials = dataStore.getCompletedTutorials(player.getUniqueId());
                sender.sendMessage("Completed Tutorials: " + String.join(", ", completedTutorials));
                var remainingTutorials = config.getTutorialList().stream()
                        .filter(tutorial -> !completedTutorials.contains(tutorial))
                        .toList();
                sender.sendMessage("Remaining Tutorials: " + String.join(", ", remainingTutorials));
            }
            return true;
        }

        sender.sendMessage("Unknown command. Use /tutorial complete or /tutorial list.");
        return false;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        if (args.length == 1) {
            return List.of("complete", "list");
        } else if (args.length == 2 && args[0].equalsIgnoreCase("complete")) {
            return config.getTutorialList();
        } else if (args.length == 2 && args[0].equalsIgnoreCase("list")) {
            // See other user's completed tutorials
            return List.of(plugin.getServer().getOnlinePlayers().stream()
                    .map(Player::getName)
                    .toArray(String[]::new));
        }
        return null;
    }
}
