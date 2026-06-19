package org.glstudio.economy.common.framework.menu.button;

import org.glstudio.economy.common.framework.menu.Menu;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;

public class ButtonPresets {

    public static Button close() {
        return Button.builder()
                .material(Material.BARRIER)
                .name(ChatColor.RED + "Close")
                .onClickPlayer(Player::closeInventory)
                .build();
    }

    public static Button back(Menu previousMenu) {
        return Button.builder()
                .material(Material.ARROW)
                .name(ChatColor.YELLOW + "Back")
                .onClickSimple(previousMenu::open)
                .build();
    }

    public static Button nextPage(Runnable action) {
        return Button.builder()
                .material(Material.ARROW)
                .name(ChatColor.GREEN + "Next Page")
                .lore(ChatColor.GRAY + "Click to go to the next page")
                .onClickSimple(action)
                .build();
    }

    public static Button previousPage(Runnable action) {
        return Button.builder()
                .material(Material.ARROW)
                .name(ChatColor.GREEN + "Previous Page")
                .lore(ChatColor.GRAY + "Click to go to the previous page")
                .onClickSimple(action)
                .build();
    }

    public static Button confirm(Runnable action) {
        return Button.builder()
                .material(Material.EMERALD_BLOCK)
                .name(ChatColor.GREEN + "" + ChatColor.BOLD + "CONFIRM")
                .lore(ChatColor.GRAY + "Click to confirm")
                .onClickSimple(action)
                .build();
    }

    public static Button cancel(Runnable action) {
        return Button.builder()
                .material(Material.REDSTONE_BLOCK)
                .name(ChatColor.RED + "" + ChatColor.BOLD + "CANCEL")
                .lore(ChatColor.GRAY + "Click to cancel")
                .onClickSimple(action)
                .build();
    }

    public static Button glassFiller(int data) {
        return Button.builder()
                .material(Material.GLASS_PANE)
                .amount(1)
                .name(" ")
                .hideAll()
                .build();
    }

    public static Button blackGlass() {
        return glassFiller(15);
    }

    public static Button empty() {
        return Button.empty();
    }

    public static Button info(String title, String... lore) {
        return Button.builder()
                .material(Material.BOOK)
                .name(ChatColor.AQUA + title)
                .lore(lore)
                .build();
    }

    public static Button loading() {
        return Button.builder()
                .material(Material.HOPPER)
                .name(ChatColor.YELLOW + "Loading...")
                .lore(ChatColor.GRAY + "Please wait")
                .hideAll()
                .build();
    }
}