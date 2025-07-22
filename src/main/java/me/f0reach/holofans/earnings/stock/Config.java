package me.f0reach.holofans.earnings.stock;

import me.f0reach.holofans.earnings.HolofansEarnings;

import java.util.UUID;

public class Config {
    public final HolofansEarnings plugin;
    private UUID shopId;
    private int stockBuySlotStart, stockSellSlotStart;
    private double stockDiffPrice;
    private double randomCoe1, randomCoe2;
    private double globalUpdateProbability;


    public Config(HolofansEarnings plugin) {
        this.plugin = plugin;

        reload();
    }

    public void reload() {
        var section = plugin.getConfig().getConfigurationSection("stock");
        if (section == null) {
            plugin.getLogger().severe("Stock section is missing in the configuration file.");
            return;
        }

        shopId = UUID.fromString(section.getString("shopId", "00000000-0000-0000-0000-000000000000"));
        stockBuySlotStart = section.getInt("stockBuySlotStart", 9);
        stockSellSlotStart = section.getInt("stockSellSlotStart", 27);
        stockDiffPrice = section.getDouble("stockDiffPrice", 3.0);
        randomCoe1 = section.getDouble("randomCoe1", 0.3);
        randomCoe2 = section.getDouble("randomCoe2", 0.15);
        globalUpdateProbability = section.getDouble("globalUpdateDiff", 0.5);
    }

    public UUID getShopId() {
        return shopId;
    }

    public int getStockBuySlotStart() {
        return stockBuySlotStart;
    }

    public int getStockSellSlotStart() {
        return stockSellSlotStart;
    }

    public double getStockDiffPrice() {
        return stockDiffPrice;
    }

    public double getRandomCoe1() {
        return randomCoe1;
    }

    public double getRandomCoe2() {
        return randomCoe2;
    }

    public double getGlobalUpdateProbability() {
        return globalUpdateProbability;
    }
}
