package me.f0reach.holofans.earnings.stock;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class StockEntry implements ConfigurationSerializable {
    private String name;
    private int slotId;
    private boolean isRandom;
    private double price;
    private double overrideCoe1, overrideCoe2;

    public StockEntry(String name) {
        this.name = name;
        this.slotId = -1; // Default slot ID if not specified
        this.isRandom = false; // Default to not random
        this.price = 0.0; // Default price
        this.overrideCoe1 = Double.NaN; // Default coefficient for random price calculation
        this.overrideCoe2 = Double.NaN; // Default coefficient for random price calculation
    }

    public StockEntry(String name, int slotId, boolean isRandom, double price,
                      double overrideCoe1, double overrideCoe2) {
        this.name = name;
        this.slotId = slotId;
        this.isRandom = isRandom;
        this.price = price;
        this.overrideCoe1 = overrideCoe1;
        this.overrideCoe2 = overrideCoe2;
    }

    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof StockEntry other)) return false;
        return name.equals(other.name);
    }

    public String getName() {
        return name;
    }

    public int getSlotId() {
        return slotId;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean hasSlotId() {
        return slotId >= 0;
    }

    public void setSlotId(int slotId) {
        this.slotId = slotId;
    }

    public boolean isRandom() {
        return isRandom;
    }

    public void setRandom(boolean random) {
        isRandom = random;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public ItemStack getStockItem() {
        var item = new ItemStack(Material.FLOWER_BANNER_PATTERN, 1);
        var meta = item.getItemMeta();
        meta.setEnchantmentGlintOverride(true);
        meta.customName(getItemDisplayName());
        meta.lore(List.of(
                Component.text("株として売買できるよ", NamedTextColor.GREEN)
        ));
        meta.setMaxStackSize(64);
        item.setItemMeta(meta);
        return item;
    }

    public static boolean isStockItem(@NotNull ItemStack item) {
        if (item.getType() != Material.FLOWER_BANNER_PATTERN) {
            return false;
        }
        var meta = item.getItemMeta();
        if (meta == null || !meta.hasEnchantmentGlintOverride()) {
            return false;
        }

        var customName = meta.customName();
        return customName != null && customName.toString().contains("株: ");
    }

    public Component getItemDisplayName() {
        return Component.text("株: " + getName(), NamedTextColor.GOLD, TextDecoration.BOLD);
    }

    public Component getListChatText(double sellFee) {
        var sellPrice = price - sellFee;
        return Component.empty()
                .append(Component.text("株: " + getName(), NamedTextColor.GOLD, TextDecoration.BOLD))
                .append(Component.text(" | ", NamedTextColor.WHITE))
                .append(Component.text("買値: " + price, NamedTextColor.GREEN))
                .append(Component.text(" | ", NamedTextColor.WHITE))
                .append(Component.text("売値: " + sellPrice, NamedTextColor.RED));
    }

    public double calculateNextPrice(double defaultCoe1, double defaultCoe2, double minPrice) {
        if (Double.isFinite(overrideCoe1)) defaultCoe1 = overrideCoe1;

        if (Double.isFinite(overrideCoe2)) defaultCoe2 = overrideCoe2;

        if (isRandom) {
            var newPrice = price + price * (Math.random() * defaultCoe1 - defaultCoe2);
            return Math.round(Math.max(newPrice, minPrice));
        } else {
            return price;
        }
    }

    public void setOverrideCoe1(double overrideCoe1) {
        this.overrideCoe1 = overrideCoe1;
    }

    public void setOverrideCoe2(double overrideCoe2) {
        this.overrideCoe2 = overrideCoe2;
    }

    public void resetOverrideCoefficients() {
        this.overrideCoe1 = Double.NaN;
        this.overrideCoe2 = Double.NaN;
    }

    public @NotNull Map<String, Object> serialize() {
        Map<String, Object> result = new LinkedHashMap<String, Object>();

        result.put("name", name);
        result.put("slotId", slotId);
        result.put("isRandom", isRandom);
        result.put("price", price);

        if (!Double.isNaN(overrideCoe1)) {
            result.put("overrideCoe1", overrideCoe1);
        }

        if (!Double.isNaN(overrideCoe2)) {
            result.put("overrideCoe2", overrideCoe2);
        }

        return result;
    }

    public static StockEntry deserialize(Map<String, Object> args) {
        String name = "Unknown";
        int slotId = -1;
        boolean isRandom = false;
        double price = 0.0;
        double overrideCoe1 = Double.NaN;
        double overrideCoe2 = Double.NaN;

        if (args.containsKey("name")) {
            name = (String) args.get("name");
        }

        if (args.containsKey("slotId")) {
            slotId = (int) args.get("slotId");
        }

        if (args.containsKey("isRandom")) {
            isRandom = (boolean) args.get("isRandom");
        }

        if (args.containsKey("price")) {
            price = (double) args.get("price");
        }

        if (args.containsKey("overrideCoe1")) {
            overrideCoe1 = (double) args.get("overrideCoe1");
        }

        if (args.containsKey("overrideCoe2")) {
            overrideCoe2 = (double) args.get("overrideCoe2");
        }

        return new StockEntry(name, slotId, isRandom, price, overrideCoe1, overrideCoe2);
    }
}
