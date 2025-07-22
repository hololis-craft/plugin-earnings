package me.f0reach.holofans.earnings.stock;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

import java.util.ArrayList;

public class StockUpdateManager {
    private interface UpdateEntry {
        Component getUpdateMessage();
    }

    private static class AddEntry implements UpdateEntry {
        private final StockEntry stockEntry;

        public AddEntry(StockEntry stockEntry) {
            this.stockEntry = stockEntry;
        }

        @Override
        public Component getUpdateMessage() {
            return Component.text("[新規銘柄] ", NamedTextColor.YELLOW)
                    .append(Component.text(stockEntry.getName(), NamedTextColor.GOLD, TextDecoration.BOLD))
                    .append(Component.text(" "))
                    .append(Component.text("価格: ", NamedTextColor.GREEN))
                    .append(Component.text(stockEntry.getPrice() + "円", NamedTextColor.GREEN, TextDecoration.ITALIC));
        }
    }

    private static class PriceEntry implements UpdateEntry {
        private final StockEntry stockEntry;
        private final double oldPrice;

        public PriceEntry(StockEntry stockEntry, double oldPrice) {
            this.stockEntry = stockEntry;
            this.oldPrice = oldPrice;
        }

        @Override
        public Component getUpdateMessage() {
            var diff = stockEntry.getPrice() - oldPrice;
            var diffText = (diff > 0 ? "+" + diff : String.valueOf(diff)) + "円";
            return Component.text("[価格変動] ", NamedTextColor.AQUA)
                    .append(Component.text(stockEntry.getName(), NamedTextColor.GOLD, TextDecoration.BOLD))
                    .append(Component.text(" "))
                    .append(Component.text("価格: ", NamedTextColor.GREEN))
                    .append(Component.text(oldPrice + "円", NamedTextColor.WHITE, TextDecoration.ITALIC))
                    .append(Component.text(" -> ", NamedTextColor.GRAY))
                    .append(Component.text(stockEntry.getPrice() + "円", NamedTextColor.WHITE, TextDecoration.ITALIC))
                    .append(Component.text(" (", NamedTextColor.GRAY))
                    .append(Component.text(diffText, diff >= 0 ? NamedTextColor.GREEN : NamedTextColor.RED, TextDecoration.BOLD))
                    .append(Component.text(")", NamedTextColor.GRAY));

        }
    }

    private static class RemoveEntry implements UpdateEntry {
        private final StockEntry stockEntry;

        public RemoveEntry(StockEntry stockEntry) {
            this.stockEntry = stockEntry;
        }

        @Override
        public Component getUpdateMessage() {
            return Component.text("[銘柄削除] ", NamedTextColor.RED)
                    .append(Component.text(stockEntry.getName(), NamedTextColor.GOLD, TextDecoration.BOLD));
        }
    }

    private final ArrayList<UpdateEntry> updateEntries;

    public StockUpdateManager() {
        this.updateEntries = new ArrayList<>();
    }

    public void addAddEntry(StockEntry stockEntry) {
        updateEntries.add(new AddEntry(stockEntry));
    }

    public void addPriceEntry(StockEntry stockEntry, double oldPrice) {
        // Combine if the stock already exists
        for (UpdateEntry entry : updateEntries) {
            if (entry instanceof PriceEntry priceEntry
                    && priceEntry.stockEntry.equals(stockEntry)) {
                // Update existing price entry
                updateEntries.remove(entry);
                updateEntries.add(new PriceEntry(stockEntry, priceEntry.oldPrice));
                return;
            }
        }
        updateEntries.add(new PriceEntry(stockEntry, oldPrice));
    }

    public void addRemoveEntry(StockEntry stockEntry) {
        updateEntries.add(new RemoveEntry(stockEntry));
    }

    public boolean hasUpdates() {
        return !updateEntries.isEmpty();
    }

    public ArrayList<Component> getUpdateMessages() {
        ArrayList<Component> messages = new ArrayList<>();
        for (UpdateEntry entry : updateEntries) {
            messages.add(entry.getUpdateMessage());
        }
        return messages;
    }

    public void clearUpdates() {
        updateEntries.clear();
    }
}
