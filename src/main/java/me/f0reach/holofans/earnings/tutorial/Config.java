package me.f0reach.holofans.earnings.tutorial;

import me.f0reach.holofans.earnings.HolofansEarnings;

import java.util.List;

public class Config {
    public final HolofansEarnings plugin;
    private List<String> tutorialList;
    private List<String> completeCommands;

    public Config(HolofansEarnings plugin) {
        this.plugin = plugin;
        this.tutorialList = List.of();
        this.completeCommands = List.of();

        reload();
    }

    public void reload() {
        plugin.reloadConfig();

        var section = plugin.getConfig().getConfigurationSection("tutorials");
        if (section == null) {
            plugin.getLogger().severe("Tutorials section is missing in the configuration file.");
            return;
        }
        tutorialList = section.getStringList("list");
        completeCommands = section.getStringList("completeCommands");
    }

    public List<String> getTutorialList() {
        return tutorialList;
    }

    public List<String> getCompleteCommands() {
        return completeCommands;
    }
}
