package me.f0reach.holofans.earnings.stock;

import me.f0reach.holofans.earnings.HolofansEarnings;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.serialization.ConfigurationSerialization;

import java.util.ArrayList;
import java.util.List;

public class DataStore {
    private final HolofansEarnings plugin;
    private final List<StockEntry> stockEntries;

    public DataStore(HolofansEarnings plugin) {
        this.plugin = plugin;

        ConfigurationSerialization.registerClass(StockEntry.class);

        stockEntries = new ArrayList<>();
        load();
    }

    public void load() {
        stockEntries.clear();

        var path = plugin.getDataFolder().toPath().resolve("stock.yaml");
        if (!path.toFile().exists()) {
            plugin.getLogger().warning("Stock data file does not exist: " + path);
            return;
        }

        try {
            var config = YamlConfiguration.loadConfiguration(path.toFile());
            var stocks = config.getList("stocks");
            if (stocks == null) {
                plugin.getLogger().warning("No 'stocks' section found in the stock data file.");
                return;
            }

            for (Object obj : stocks) {
                if (obj instanceof StockEntry stock) {
                    // Process each stock entry
                    plugin.getLogger().info("Loaded stock: " + stock);
                    stockEntries.add(stock);
                } else {
                    plugin.getLogger().warning("Invalid stock entry: " + obj);
                }
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to load stock data: " + e.getMessage());
        }
    }

    public void save() {
        var path = plugin.getDataFolder().toPath().resolve("stock.yaml");
        var config = new YamlConfiguration();

        List<StockEntry> stocks = new ArrayList<>(stockEntries);
        config.set("stocks", stocks);

        try {
            config.save(path.toFile());
            plugin.getLogger().info("Stock data saved successfully.");
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to save stock data: " + e.getMessage());
        }
    }

    public List<StockEntry> getStockEntries() {
        return stockEntries;
    }

    public void addStockEntry(StockEntry stockEntry) {
        stockEntries.add(stockEntry);
        save();
    }

    public void removeStockEntry(StockEntry stockEntry) {
        stockEntries.remove(stockEntry);
        save();
    }
    
}
