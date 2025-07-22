package me.f0reach.holofans.earnings.stock;

import org.bukkit.Material;
import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class PlayerCollector {
    private final DataStore dataStore;
    private final Config config;

    public PlayerCollector(final @NotNull DataStore dataStore, final @NotNull Config config) {
        this.dataStore = dataStore;
        this.config = config;
    }

    public Map<StockEntry, Integer> collect(final Inventory inventory, boolean recursive) {
        var map = new HashMap<StockEntry, Integer>();
        for (var item : inventory.getContents()) {
            if (item == null || item.getType() == Material.AIR) continue;
            if (recursive && item.hasItemMeta() && item.getItemMeta() instanceof BlockStateMeta blockMeta &&
                    blockMeta.getBlockState() instanceof ShulkerBox shulkerBox) {
                var inside = shulkerBox.getInventory();
                var innerMap = collect(inside, true);
                for (var entry : innerMap.entrySet()) {
                    map.merge(entry.getKey(), entry.getValue(), Integer::sum);
                }
            }

            var name = StockEntry.getStockName(item);

            var stockEntry = dataStore.getStockEntry(name);
            if (stockEntry == null) continue;

            int amount = item.getAmount();
            map.merge(stockEntry, amount, Integer::sum);
        }
        return map;
    }

    public Map<StockEntry, Integer> collectPlayerStocks(final @NotNull Player player) {
        var inventory = collect(player.getInventory(), true);
        var enderChest = collect(player.getEnderChest(), true);

        var map = new HashMap<StockEntry, Integer>();
        for (var entry : inventory.entrySet()) {
            map.merge(entry.getKey(), entry.getValue(), Integer::sum);
        }
        for (var entry : enderChest.entrySet()) {
            map.merge(entry.getKey(), entry.getValue(), Integer::sum);
        }

        return map;
    }

}
