package org.glstudio.economy.common.framework.menu.extra;

import org.glstudio.economy.common.framework.menu.Menu;
import org.glstudio.economy.common.framework.menu.MenuManager;
import org.glstudio.economy.common.framework.menu.button.Button;
import org.glstudio.economy.common.framework.menu.button.ButtonPresets;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

public class ConfirmMenu extends Menu {

    private final String message;
    private final Runnable onConfirm;
    private final Runnable onCancel;
    private final Button displayItem;

    public ConfirmMenu(MenuManager manager, Player p, String title, String message,
                       Runnable onConfirm, Runnable onCancel) {
        this(manager, p, title, message, null, onConfirm, onCancel);
    }

    public ConfirmMenu(MenuManager manager, Player p, String title, String message,
                       Button displayItem, Runnable onConfirm, Runnable onCancel) {
        super(manager, p, title, 27, false);
        this.message = message;
        this.displayItem = displayItem;
        this.onConfirm = onConfirm;
        this.onCancel = onCancel;
    }

    @Override
    public Map<Integer, Button> getButtons(Player p) {
        Map<Integer, Button> buttons = new HashMap<>();

        for (int i = 0; i < 27; i++) {
            if (i < 9 || i >= 18 || i % 9 == 0 || i % 9 == 8) {
                buttons.put(i, ButtonPresets.blackGlass());
            }
        }

        if (displayItem != null) {
            buttons.put(13, displayItem);
        } else {
            buttons.put(13, ButtonPresets.info("Confirmation",
                ChatColor.GRAY + message,
                "",
                ChatColor.YELLOW + "Choose an option below"
            ));
        }

        buttons.put(11, ButtonPresets.confirm(() -> {
            p.closeInventory();
            if (onConfirm != null) {
                onConfirm.run();
            }
        }));

        buttons.put(15, ButtonPresets.cancel(() -> {
            p.closeInventory();
            if (onCancel != null) {
                onCancel.run();
            }
        }));

        return buttons;
    }

    public static void create(MenuManager manager, Player p, String title,
                             String message, Runnable onConfirm, Runnable onCancel) {
        new ConfirmMenu(manager, p, title, message, onConfirm, onCancel).open();
    }

    public static void create(MenuManager manager, Player p, String title,
                             String message, Button displayItem, Runnable onConfirm, Runnable onCancel) {
        new ConfirmMenu(manager, p, title, message, displayItem, onConfirm, onCancel).open();
    }
}