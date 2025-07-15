package me.f0reach.holofans.earnings;

import me.f0reach.holofans.earnings.jobs.Jobs;
import me.f0reach.holofans.earnings.tutorial.Tutorial;
import net.luckperms.api.LuckPerms;
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
    private Jobs jobs;

    // Interface
    private LuckPerms luckPerms;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        actionBarDisplay = new ActionBarDisplay(this);
        tutorial = new Tutorial(this);
        jobs = new Jobs(this);

        getLogger().info("HolofansEarnings plugin has been enabled successfully.");
        Objects.requireNonNull(getCommand("earnings")).setExecutor(this);

        var luckPermsService = getServer().getServicesManager().getRegistration(LuckPerms.class);
        if (luckPermsService != null) {
            luckPerms = luckPermsService.getProvider();
            getLogger().info("LuckPerms integration enabled.");
        } else {
            getLogger().warning("LuckPerms integration not found. Some features may not work.");
        }
    }

    public LuckPerms getLuckPerms() {
        return luckPerms;
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
        if (jobs != null) {
            jobs.reloadConfig();
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
