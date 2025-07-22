package me.f0reach.holofans.earnings.stock;

import me.f0reach.holofans.earnings.HolofansEarnings;
import net.bestemor.villagermarket.VillagerMarketAPI;
import net.bestemor.villagermarket.shop.ItemMode;
import net.bestemor.villagermarket.shop.ShopManager;
import net.bestemor.villagermarket.shop.VillagerShop;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class Stock implements CommandExecutor, TabCompleter {
    private final HolofansEarnings plugin;
    private final Config config;
    private final DataStore dataStore;
    private final ShopManager shopManager;
    private final StockUpdateManager stockUpdateManager;
    private final PlayerCollector playerCollector;

    public Stock(HolofansEarnings plugin) {
        this.plugin = plugin;
        this.config = new Config(plugin);
        this.dataStore = new DataStore(plugin);
        this.stockUpdateManager = new StockUpdateManager();
        this.playerCollector = new PlayerCollector(dataStore, config);

        shopManager = VillagerMarketAPI.getShopManager();

        if (shopManager == null) {
            plugin.getLogger().severe("VillagerMarketAPI ShopManager is not available. Stock commands will not work.");
            return;
        }

        Objects.requireNonNull(plugin.getCommand("kabu")).setExecutor(this);
        Objects.requireNonNull(plugin.getCommand("kabu")).setTabCompleter(this);

        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) { //
            new StockPlaceholder(playerCollector).register(); //
        }

        try {
            reloadConfig();
            applyToShop();
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to apply stock data: " + e.getMessage());
        }
    }

    public void reloadConfig() {
        config.reload();
        dataStore.load();
    }

    private VillagerShop getShop() {
        var shop = shopManager.getShop(config.getShopId());
        if (shop == null) {
            throw new IllegalStateException("Shop with ID " + config.getShopId() + " not found.");
        }
        return shop;
    }

    private void removeAllStockItems() {
        var shop = getShop();
        var itemList = shop.getShopfrontHolder().getItemList();

        var toBeRemoved = itemList.entrySet()
                .stream()
                .filter(entry -> StockEntry.isStockItem(entry.getValue().getRawItem()))
                .map(Map.Entry::getKey)
                .toList();
        for (var slotId : toBeRemoved) {
            shop.getShopfrontHolder().removeItem(slotId);
        }
    }

    private void registerStockItem(@NotNull StockEntry entry, boolean isSell) {
        if (!entry.hasSlotId()) {
            entry.setSlotId(getNextSlotId());
        }

        var holder = getShop().getShopfrontHolder();
        var actualSlot = isSell
                ? config.getStockSellSlotStart() + entry.getSlotId()
                : config.getStockBuySlotStart() + entry.getSlotId();
        if (holder.getItemList().containsKey(actualSlot)) {
            throw new IllegalStateException("Slot ID " + actualSlot + " is already occupied in the shop.");
        }

        var item = entry.getStockItem();
        var shopItem = holder.addItem(actualSlot, item);
        shopItem.setMode(isSell ? ItemMode.BUY : ItemMode.SELL);
        shopItem.setAmount(1);
        var price = isSell
                ? BigDecimal.valueOf(entry.getPrice() - config.getStockDiffPrice())
                : BigDecimal.valueOf(entry.getPrice());
        shopItem.setSellPrice(price);
    }

    private int getNextSlotId() {
        var occupiedSlotSet = dataStore.getStockEntries()
                .stream()
                .mapToInt(StockEntry::getSlotId)
                .boxed().collect(Collectors.toCollection(HashSet::new));

        for (int i = 0; i < 1000; i++) {
            if (!occupiedSlotSet.contains(i)) {
                return i;
            }
        }

        throw new IllegalStateException("No available slot found for new stock entry.");
    }

    private void applyToShop() {
        removeAllStockItems();
        for (var entry : dataStore.getStockEntries()) {
            try {
                registerStockItem(entry, false);
                registerStockItem(entry, true);
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to register stock item: " + e.getMessage());
            }
        }
        shopManager.saveAll();
    }


    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        if (args.length == 0) {
            sender.sendMessage(Component.text("Usage: /kabu <list|set|create|update>", NamedTextColor.YELLOW));
            return true;
        }

        if ("list".equalsIgnoreCase(args[0])) {
            if (!sender.hasPermission("holofans.stock.list")) {
                sender.sendMessage(Component.text("You do not have permission to list stock entries.", NamedTextColor.RED));
                return true;
            }

            Map<StockEntry, Integer> countMap = sender instanceof Player p
                    ? playerCollector.collectPlayerStocks(p) : Map.of();

            sender.sendMessage(Component.text("Stock Entries:", NamedTextColor.WHITE, TextDecoration.BOLD));
            for (var entry : dataStore.getStockEntries()) {
                var text = entry.getListChatText(config.getStockDiffPrice());
                if (countMap.containsKey(entry)) {
                    var count = countMap.get(entry);
                    text = text.append(Component.text(
                            " (保有" + String.format("%.0f", entry.getPrice() * count)
                                    + "円 / " + count + "株)", NamedTextColor.YELLOW));
                }
                sender.sendMessage(text);
            }

            return true;
        }

        if ("set".equalsIgnoreCase(args[0])) {
            if (args.length < 3) {
                sender.sendMessage(Component.text("Usage: /kabu set <name> <buyPrice>", NamedTextColor.YELLOW));
                return true;
            }

            if (!sender.hasPermission("holofans.stock.set")) {
                sender.sendMessage(Component.text("You do not have permission to set stock entries.", NamedTextColor.RED));
                return true;
            }

            String name = args[1];
            var entry = dataStore.getStockEntries()
                    .stream()
                    .filter(e -> e.getName().equalsIgnoreCase(name))
                    .findFirst()
                    .orElse(null);

            if (entry == null) {
                sender.sendMessage(Component.text("Stock entry with name '" + name + "' not found.", NamedTextColor.RED));
                return true;
            }

            try {
                double buyPrice = Double.parseDouble(args[2]);
                if (buyPrice < 0) {
                    sender.sendMessage(Component.text("Buy price must be a non-negative integer.", NamedTextColor.RED));
                    return true;
                }

                var oldPrice = entry.getPrice();
                entry.setPrice(buyPrice);
                stockUpdateManager.addPriceEntry(entry, oldPrice);

                dataStore.save();

                sender.sendMessage(Component.text("Stock entry '" + name + "' updated with buy price: " + buyPrice, NamedTextColor.GREEN));
            } catch (NumberFormatException e) {
                sender.sendMessage(Component.text("Invalid buy price. Please enter a valid integer.", NamedTextColor.RED));
            }

            return true;
        }

        if ("create".equalsIgnoreCase(args[0])) {
            if (args.length < 3) {
                sender.sendMessage(Component.text("Usage: /kabu create <name> <isRandom> <initialPrice>", NamedTextColor.YELLOW));
                return true;
            }

            if (!sender.hasPermission("holofans.stock.create")) {
                sender.sendMessage(Component.text("You do not have permission to create stock entries.", NamedTextColor.RED));
                return true;
            }

            String name = args[1];
            boolean isRandom;
            try {
                isRandom = Boolean.parseBoolean(args[2]);
            } catch (Exception e) {
                sender.sendMessage(Component.text("Invalid value for isRandom. Please enter true or false.", NamedTextColor.RED));
                return true;
            }

            double initialPrice;
            try {
                initialPrice = Double.parseDouble(args[3]);
                if (initialPrice < 0) {
                    sender.sendMessage(Component.text("Initial price must be a non-negative integer.", NamedTextColor.RED));
                    return true;
                }
            } catch (NumberFormatException e) {
                sender.sendMessage(Component.text("Invalid initial price. Please enter a valid integer.", NamedTextColor.RED));
                return true;
            }

            var hasEntry = dataStore.getStockEntries()
                    .stream()
                    .anyMatch(e -> e.getName().equalsIgnoreCase(name));
            if (hasEntry) {
                sender.sendMessage(Component.text("Stock entry with name '" + name + "' already exists.", NamedTextColor.RED));
                return true;
            }

            var entry = new StockEntry(name);
            entry.setRandom(isRandom);
            entry.setPrice(initialPrice);

            try {
                dataStore.addStockEntry(entry);
                registerStockItem(entry, false);
                registerStockItem(entry, true);
            } catch (Exception e) {
                sender.sendMessage(Component.text("Failed to create stock entry: " + e.getMessage(), NamedTextColor.RED));
                return true;
            }

            stockUpdateManager.addAddEntry(entry);

            sender.sendMessage(Component.text("Stock entry '" + name + "' created with initial price: " + initialPrice, NamedTextColor.GREEN));
            return true;
        }

        if ("update".equalsIgnoreCase(args[0])) {
            // Update all random stock entries
            if (!sender.hasPermission("holofans.stock.update")) {
                sender.sendMessage(Component.text("You do not have permission to update stock entries.", NamedTextColor.RED));
                return true;
            }

            var stockEntries = dataStore.getStockEntries()
                    .stream()
                    .filter(StockEntry::isRandom)
                    .toList();

            for (var entry : stockEntries) {
                var shouldUpdate = Math.random() < config.getGlobalUpdateProbability();
                if (!shouldUpdate) {
                    continue; // Skip this entry
                }
                var oldPrice = entry.getPrice();
                var nextPrice = entry.calculateNextPrice(config.getRandomCoe1(), config.getRandomCoe2(), config.getStockDiffPrice());
                entry.setPrice(nextPrice);
                entry.resetOverrideCoefficients();
                sender.sendMessage(Component.text("Updated stock entry: " + entry.getName() + " | Old Price: " + oldPrice + " -> New Price: " + nextPrice, NamedTextColor.YELLOW));
                stockUpdateManager.addPriceEntry(entry, oldPrice);
            }

            dataStore.save();

            sender.sendMessage(Component.text("All random stock entries updated (not applied).", NamedTextColor.GREEN));
            return true;
        }

        if ("apply".equalsIgnoreCase(args[0])) {
            if (!sender.hasPermission("holofans.stock.apply")) {
                sender.sendMessage(Component.text("You do not have permission to apply stock changes.", NamedTextColor.RED));
                return true;
            }

            try {
                dataStore.save();
                applyToShop();
                sender.sendMessage(Component.text("Stock items applied to the shop successfully.", NamedTextColor.GREEN));
            } catch (Exception e) {
                sender.sendMessage(Component.text("Failed to apply stock items: " + e.getMessage(), NamedTextColor.RED));
            }

            if (args.length == 2 && "broadcast".equalsIgnoreCase(args[1])) {
                // Broadcast stock updates
                if (stockUpdateManager.hasUpdates()) {
                    var title = Component.text("【株情報更新】", NamedTextColor.AQUA, TextDecoration.BOLD);
                    plugin.getServer().broadcast(title);
                    for (var update : stockUpdateManager.getUpdateMessages()) {
                        plugin.getServer().broadcast(update);
                    }
                    stockUpdateManager.clearUpdates();
                }
            }
            return true;
        }

        if ("override".equalsIgnoreCase(args[0])) {
            if (args.length < 4) {
                sender.sendMessage(Component.text("Usage: /kabu override <name> <overrideCoe1> <overrideCoe2>", NamedTextColor.YELLOW));
                return true;
            }

            if (!sender.hasPermission("holofans.stock.override")) {
                sender.sendMessage(Component.text("You do not have permission to override stock coefficients.", NamedTextColor.RED));
                return true;
            }

            String name = args[1];
            var entry = dataStore.getStockEntries()
                    .stream()
                    .filter(e -> e.getName().equalsIgnoreCase(name))
                    .findFirst()
                    .orElse(null);

            if (entry == null) {
                sender.sendMessage(Component.text("Stock entry with name '" + name + "' not found.", NamedTextColor.RED));
                return true;
            }

            try {
                double overrideCoe1 = Double.parseDouble(args[2]);
                double overrideCoe2 = Double.parseDouble(args[3]);
                entry.setOverrideCoe1(overrideCoe1);
                entry.setOverrideCoe2(overrideCoe2);
                sender.sendMessage(Component.text("Stock entry '" + name + "' coefficients overridden: " + overrideCoe1 + ", " + overrideCoe2, NamedTextColor.GREEN));
                dataStore.save();
            } catch (NumberFormatException e) {
                sender.sendMessage(Component.text("Invalid coefficients. Please enter valid numbers.", NamedTextColor.RED));
            }

            return true;
        }
        return false;
    }

    @Override
    public @Nullable List<String> onTabComplete(
            @NotNull CommandSender sender, @NotNull Command command,
            @NotNull String label, @NotNull String @NotNull [] args) {
        if (args.length == 1) {
            return List.of("list", "set", "create", "update", "apply", "override");
        }

        if (args.length == 2) {
            if ("list".equalsIgnoreCase(args[0]) || "update".equalsIgnoreCase(args[0])) {
                return List.of();
            }

            if ("set".equalsIgnoreCase(args[0]) || "override".equalsIgnoreCase(args[0])) {
                return dataStore.getStockEntries()
                        .stream()
                        .map(StockEntry::getName)
                        .toList();
            }

            if ("apply".equalsIgnoreCase(args[0])) {
                return List.of("broadcast");
            }
        }

        return null; // No suggestions for other cases
    }
}
