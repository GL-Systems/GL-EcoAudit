package org.glstudio.economy.common.framework.menu.button;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

public class ButtonBuilder {

    private ItemStack itemStack;
    private Consumer<InventoryClickEvent> clickAction;
    private boolean cancelClick = true;

    public ButtonBuilder() {
        this.itemStack = new ItemStack(Material.STONE);
    }

    public ButtonBuilder item(ItemStack itemStack) {
        this.itemStack = itemStack;
        return this;
    }

    public ButtonBuilder material(Material material) {
        this.itemStack.setType(material);
        return this;
    }

    public ButtonBuilder amount(int amount) {
        this.itemStack.setAmount(amount);
        return this;
    }

    public ButtonBuilder name(String name) {
        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            itemStack.setItemMeta(meta);
        }
        return this;
    }

    public ButtonBuilder lore(List<String> lore) {
        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null) {
            meta.setLore(lore);
            itemStack.setItemMeta(meta);
        }
        return this;
    }

    public ButtonBuilder lore(String... lore) {
        return lore(Arrays.asList(lore));
    }

    public ButtonBuilder addLore(String line) {
        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null) {
            List<String> lore = meta.hasLore() ? meta.getLore() : new ArrayList<>();
            lore.add(line);
            meta.setLore(lore);
            itemStack.setItemMeta(meta);
        }
        return this;
    }

    public ButtonBuilder enchant(Enchantment enchantment, int level) {
        itemStack.addUnsafeEnchantment(enchantment, level);
        return this;
    }

    public ButtonBuilder glow() {
        itemStack.addUnsafeEnchantment(Enchantment.LURE, 1);
        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null) {
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            itemStack.setItemMeta(meta);
        }
        return this;
    }

    public ButtonBuilder flags(ItemFlag... flags) {
        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null) {
            meta.addItemFlags(flags);
            itemStack.setItemMeta(meta);
        }
        return this;
    }

    public ButtonBuilder hideAll() {
        return flags(ItemFlag.values());
    }

    public ButtonBuilder customModelData(int data) {
        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null) {
            meta.setCustomModelData(data);
            itemStack.setItemMeta(meta);
        }
        return this;
    }

    public ButtonBuilder tooltipStyle(String tooltipStyle) {
        if (tooltipStyle == null || tooltipStyle.trim().isEmpty()) {
            return this;
        }

        ItemMeta meta = itemStack.getItemMeta();
        if (meta != null) {
            try {
                NamespacedKey key = NamespacedKey.fromString(tooltipStyle);
                if (key != null) {
                    meta.setTooltipStyle(key);
                    itemStack.setItemMeta(meta);
                }
            } catch (Exception ignored) {
            }
        }
        return this;
    }

    public ButtonBuilder onClick(Consumer<InventoryClickEvent> action) {
        this.clickAction = action;
        return this;
    }

    public ButtonBuilder onClickPlayer(Consumer<Player> action) {
        this.clickAction = e -> action.accept((Player) e.getWhoClicked());
        return this;
    }

    public ButtonBuilder onClickSimple(Runnable action) {
        this.clickAction = e -> action.run();
        return this;
    }

    public ButtonBuilder cancelClick(boolean cancel) {
        this.cancelClick = cancel;
        return this;
    }

    public Button build() {
        Button button = new Button(itemStack, clickAction);
        button.setCancelClick(cancelClick);
        return button;
    }
}