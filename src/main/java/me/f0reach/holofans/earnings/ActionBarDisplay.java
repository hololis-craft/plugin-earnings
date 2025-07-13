package me.f0reach.holofans.earnings;

import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.scheduler.BukkitTask;

public class ActionBarDisplay {
    private final HolofansEarnings plugin;
    private String message;
    private double interval;
    private BukkitTask task;

    public ActionBarDisplay(HolofansEarnings plugin) {
        this.plugin = plugin;
        this.message = "";
        this.interval = 0.0;

        reloadConfig();
    }

    public void reloadConfig() {
        var section = plugin.getConfig().getConfigurationSection("actionBar");
        if (section == null) {
            plugin.getLogger().warning("No 'actionBar' section found in config.yml");
            return;
        }

        this.message = section.getString("message", "");
        this.interval = section.getDouble("interval", 0.0);

        registerTask();
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    private void registerTask() {
        if (task != null) {
            task.cancel();
            task = null;
        }

        if (this.interval <= 0) {
            return;
        }

        task = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (message.isEmpty()) return;
            
            displayActionBar();
        }, 0L, (long) (interval * 20)); // Convert seconds to ticks
    }

    private void displayActionBar() {
        plugin.getServer().getOnlinePlayers().forEach(player -> {
            var placeholderMessage = PlaceholderAPI.setPlaceholders(player, this.message);
            var components = MiniMessage.miniMessage().deserialize(placeholderMessage);
            player.sendActionBar(components);
        });
    }
}
