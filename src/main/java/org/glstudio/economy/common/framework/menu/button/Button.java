package org.glstudio.economy.common.framework.menu.button;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.function.Consumer;

@Getter
@Setter
public class Button {

    protected ItemStack itemStack;
    protected Consumer<InventoryClickEvent> clickAction;
    protected boolean cancelClick;

    public Button() {
        this.itemStack = new ItemStack(Material.AIR);
        this.cancelClick = true;
    }

    public Button(ItemStack itemStack) {
        this.itemStack = itemStack;
        this.cancelClick = true;
    }

    public Button(ItemStack itemStack, Consumer<InventoryClickEvent> clickAction) {
        this.itemStack = itemStack;
        this.clickAction = clickAction;
        this.cancelClick = true;
    }

    public void onClick(InventoryClickEvent e) {
        if (cancelClick) {
            e.setCancelled(true);
        }

        if (clickAction != null) {
            clickAction.accept(e);
        }
    }

    public static ButtonBuilder builder() {
        return new ButtonBuilder();
    }

    public static ButtonBuilder builder(ItemStack itemStack) {
        return new ButtonBuilder().item(itemStack);
    }

    public static Button display(ItemStack itemStack) {
        return new Button(itemStack);
    }

    public static Button empty() {
        return new Button(new ItemStack(Material.AIR));
    }

    public static Button filler(Material material) {
        return new Button(new ItemStack(material));
    }
}