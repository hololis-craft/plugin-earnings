package me.f0reach.holofans.earnings;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public final class HolofansEarnings extends JavaPlugin implements CommandExecutor {
    private ActionBarDisplay actionBarDisplay;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        actionBarDisplay = new ActionBarDisplay(this);

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
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
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
}
