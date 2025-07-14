package me.f0reach.holofans.earnings;

import me.f0reach.holofans.earnings.tutorial.Tutorial;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Objects;

public final class HolofansEarnings extends JavaPlugin implements CommandExecutor {
    private ActionBarDisplay actionBarDisplay;
    private Tutorial tutorial;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        actionBarDisplay = new ActionBarDisplay(this);
        tutorial = new Tutorial(this);

        getLogger().info("HolofansEarnings plugin has been enabled successfully.");
        Objects.requireNonNull(getCommand("earnings")).setExecutor(this);
    }

    @Override
    public void onDisable() {
        actionBarDisplay.stop();
    }

    @Override
    public void reloadConfig() {
        super.reloadConfig();
        if (actionBarDisplay != null) {
            actionBarDisplay.reloadConfig();
        }
        if (tutorial != null) {
            tutorial.reloadConfig();
        }
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, Command command, @NotNull String label, String @NotNull [] args) {
        if (command.getName().equalsIgnoreCase("earnings")) {
            if (args.length == 0) {
                sender.sendMessage("Usage: /earnings reload");
                return true;
            }

            if (args[0].equalsIgnoreCase("reload") && sender.hasPermission("holofans.earnings.reload")) {
                reloadConfig();
                sender.sendMessage("Earnings action bar configuration reloaded.");
                return true;
            }

            sender.sendMessage("Unknown subcommand. Use /earnings reload to reload the configuration.");
            return true;
        }
        return false;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, Command command, @NotNull String alias, String @NotNull [] args) {
        if (command.getName().equalsIgnoreCase("earnings")) {
            if (args.length == 1) {
                return List.of("reload");
            }
        }
        return null; // No tab completion for other commands
    }
}
