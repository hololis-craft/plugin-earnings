package me.f0reach.holofans.earnings.stock;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class StockPlaceholder extends PlaceholderExpansion {
    private final PlayerCollector collector;

    public StockPlaceholder(final PlayerCollector collector) {
        super();
        this.collector = collector;
    }

    @Override
    @NotNull
    public String getAuthor() {
        return "Author"; //
    }

    @Override
    @NotNull
    public String getIdentifier() {
        return "earnings"; //
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    @NotNull
    public String getVersion() {
        return "1.0.0"; //
    }

    @Override
    public @NotNull List<String> getPlaceholders() {
        return List.of(
                "stock_price_user_all",
                "stock_price_user_<stock_name>"
        );
    }

    @Override
    public @Nullable String onPlaceholderRequest(Player player, @NotNull String params) {
        if (params.equals("stock_price_user_all")) {
            var map = collector.collectPlayerStocks(player);
            double totalPrice = 0.0;
            for (var entry : map.entrySet()) {
                var stock = entry.getKey();
                var count = entry.getValue();
                totalPrice += stock.getPrice() * count;
            }
            return String.format("%.0f", totalPrice);
        }

        if (params.startsWith("stock_price_user_")) {
            var map = collector.collectPlayerStocks(player);
            String stockName = params.substring("stock_price_user_".length());
            for (var entry : map.entrySet()) {
                var stock = entry.getKey();
                if (stock.getName().equalsIgnoreCase(stockName)) {
                    return String.format("%.0f", stock.getPrice() * entry.getValue());
                }
            }
            return "0"; // Return 0 if stock not found
        }

        return null;
    }
}
