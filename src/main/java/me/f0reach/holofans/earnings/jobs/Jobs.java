package me.f0reach.holofans.earnings.jobs;

import com.gamingmesh.jobs.api.JobsJoinEvent;
import com.gamingmesh.jobs.api.JobsLeaveEvent;
import me.f0reach.holofans.earnings.HolofansEarnings;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.luckperms.api.node.types.PermissionNode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

public class Jobs implements Listener {
    private final HolofansEarnings plugin;
    private final Config config;

    public Jobs(HolofansEarnings plugin) {
        this.plugin = plugin;
        this.config = new Config(plugin);

        // Register the event listener
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public void reloadConfig() {
        config.reload();
        plugin.getLogger().info("Jobs configuration reloaded successfully.");
    }

    private void dispatchCommand(String command, Player player) {
        // Replace placeholders in the command if necessary
        String finalCommand = command.replace("{player}", player.getName());

        // Dispatch the command as console
        plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), finalCommand);
    }

    private void dispatchCommands(List<String> commands, Player player) {
        for (String command : commands) {
            dispatchCommand(command, player);
        }
    }

    private String durationToString(Duration duration) {
        long seconds = duration.getSeconds();
        long minutes = seconds / 60;
        seconds %= 60;
        long hours = minutes / 60;
        minutes %= 60;

        return String.format("%d時間%02d分%02d秒", hours, minutes, seconds);
    }

    @EventHandler
    public void onJobsJoin(JobsJoinEvent event) {
        String jobName = event.getJob().getName();
        if (jobName == null || jobName.isEmpty()) {
            plugin.getLogger().warning("Job name is null or empty in JobsJoinEvent.");
            return;
        }

        var player = event.getPlayer().getPlayer();
        if (player == null) {
            plugin.getLogger().warning("Player is null in JobsJoinEvent.");
            return;
        }

        var joinCommands = config.getJoinCommandForJob(jobName);
        if (!joinCommands.isEmpty()) {
            dispatchCommands(joinCommands, event.getPlayer().getPlayer());
        }

        if (config.getLeaveCooldown() > 0) {
            var lp = plugin.getLuckPerms();
            if (lp == null) {
                plugin.getLogger().warning("LuckPerms integration not found. Leave cooldown may not work.");
                return;
            }

            // Set the leave cooldown permission
            var precisePermission = "jobs.command.leave." + jobName.toLowerCase();

            // Remove the permission to allow leave
            var preciseNode = PermissionNode.builder().permission(precisePermission)
                    .expiry(Duration.ofSeconds(config.getLeaveCooldown()))
                    .value(false)
                    .build();

            lp.getUserManager().modifyUser(
                    player.getUniqueId(),
                    user -> {
                        user.data().clear(node -> node.getKey().equals(precisePermission));
                        user.data().add(preciseNode);
                    }
            );
        }
    }

    @EventHandler
    public void onJobsLeave(JobsLeaveEvent event) {
        String jobName = event.getJob().getName();
        if (jobName == null || jobName.isEmpty()) {
            plugin.getLogger().warning("Job name is null or empty in JobsLeaveEvent.");
            return;
        }

        var lp = plugin.getLuckPerms();
        if (lp != null) {
            var player = event.getPlayer().getPlayer();
            var precisePermission = "jobs.command.leave." + jobName.toLowerCase();
            var userOverride = lp.getUserManager().getUser(player.getUniqueId());
            if (userOverride != null) {
                var node = userOverride.getNodes()
                        .stream()
                        .filter(n -> n.getKey().equals(precisePermission))
                        .map(n -> (PermissionNode) n)
                        .findFirst()
                        .flatMap(n -> n.getExpiryDuration() != null ? Optional.of(n) : Optional.empty())
                        .orElse(null);
                if (node != null) {
                    var remainingTime = node.getExpiryDuration();
                    var formattedTime = durationToString(remainingTime);
                    player.sendMessage(Component.text(
                            "あと" + formattedTime + "後に退職できます。",
                            TextColor.color(255, 0, 0)
                    ));
                    event.setCancelled(true);
                    return;
                }
            }
        }

        var leaveCommands = config.getLeaveCommandForJob(jobName);
        if (!leaveCommands.isEmpty()) {
            dispatchCommands(leaveCommands, event.getPlayer().getPlayer());
        }
    }
}
