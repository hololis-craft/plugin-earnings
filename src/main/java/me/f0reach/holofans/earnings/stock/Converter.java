package me.f0reach.holofans.earnings.stock;

import net.kyori.adventure.text.TextComponent;
import org.bukkit.Material;
import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.jetbrains.annotations.NotNull;

public class Converter {
    private final DataStore dataStore;

    public Converter(final @NotNull DataStore dataStore) {
        this.dataStore = dataStore;
    }

    private String getStockNameFromOldItem(ItemStack item) {
        if (item.getType() != Material.FLOWER_BANNER_PATTERN) return null;
        var meta = item.getItemMeta();
        if (meta == null || !meta.hasCustomName() || !meta.hasEnchantmentGlintOverride()) return null;

        var component = meta.customName();
        var name = component instanceof TextComponent textComponent ? textComponent.content() : null;
        if (name == null || !name.startsWith("株: ")) return null;
        return name.replace("株: ", "");
    }

    public int migrateItem(final Inventory inventory, boolean recursive) {
        int migratedCount = 0;
        for (var i = 0; i < inventory.getSize(); i++) {
            var item = inventory.getItem(i);
            if (item == null || item.getType() == Material.AIR) continue;
            if (recursive && item.hasItemMeta() && item.getItemMeta() instanceof BlockStateMeta blockMeta &&
                    blockMeta.getBlockState() instanceof ShulkerBox shulkerBox) {
                var inside = shulkerBox.getInventory();
                migratedCount += migrateItem(inside, true);
                blockMeta.setBlockState(shulkerBox);
                item.setItemMeta(blockMeta);
                inventory.setItem(i, item);
                continue;
            }

            var name = getStockNameFromOldItem(item);
            if (name == null) continue;

            var stockEntry = dataStore.getStockEntry(name);
            if (stockEntry == null) continue;

            int amount = item.getAmount();
            var newItem = stockEntry.getStockItem();
            newItem.setAmount(amount);

            inventory.setItem(i, newItem);
            migratedCount += amount;
        }
        return migratedCount;
    }

    public int migrateItem(final @NotNull Player player) {
        var inventory = player.getInventory();
        var enderChest = player.getEnderChest();

        var count = migrateItem(inventory, true);
        count += migrateItem(enderChest, true);

        // Update the player's inventory to reflect changes
        player.updateInventory();
        return count;
    }
}
