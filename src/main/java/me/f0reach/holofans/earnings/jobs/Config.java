package me.f0reach.holofans.earnings.jobs;

import me.f0reach.holofans.earnings.HolofansEarnings;

import java.util.List;
import java.util.Objects;

public class Config {
    private final HolofansEarnings plugin;
    private Long leaveCooldown;

    public Config(HolofansEarnings plugin) {
        this.plugin = plugin;

        reload();
    }

    public void reload() {
        var section = plugin.getConfig().getConfigurationSection("jobs");
        if (section == null) {
            plugin.getLogger().warning("No 'jobs' section found in the config file.");
            return;
        }

        leaveCooldown = section.getLong("leaveCooldown", 0L);
    }

    public List<String> getJoinCommandForJob(String jobName) {
        Objects.requireNonNull(jobName, "Job name cannot be null");
        return plugin.getConfig().getStringList("jobs." + jobName + ".joinCommands");
    }

    public List<String> getLeaveCommandForJob(String jobName) {
        Objects.requireNonNull(jobName, "Job name cannot be null");
        return plugin.getConfig().getStringList("jobs." + jobName + ".leaveCommands");
    }

    public Long getLeaveCooldown() {
        return leaveCooldown;
    }
}
