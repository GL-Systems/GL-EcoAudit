package org.glstudio.economy.common.framework.menu;

import org.glstudio.economy.common.framework.menu.button.Button;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class SimpleMenu extends Menu {

    private final Function<Player, Map<Integer, Button>> buttonProvider;

    public SimpleMenu(MenuManager manager, Player p, String title, int size,
                      Function<Player, Map<Integer, Button>> buttonProvider) {
        this(manager, p, title, size, false, buttonProvider);
    }

    public SimpleMenu(MenuManager manager, Player p, String title, int size,
                      boolean update, Function<Player, Map<Integer, Button>> buttonProvider) {
        super(manager, p, title, size, update);
        this.buttonProvider = buttonProvider;
    }

    @Override
    public Map<Integer, Button> getButtons(Player p) {
        return buttonProvider != null ? buttonProvider.apply(p) : new HashMap<>();
    }

    public static SimpleMenuBuilder builder(MenuManager manager, Player p) {
        return new SimpleMenuBuilder(manager, p);
    }

    public static class SimpleMenuBuilder {
        private final MenuManager manager;
        private final Player p;
        private String title = "Menu";
        private int size = 27;
        private boolean update = false;
        private boolean fillBorder = false;
        private Button borderButton = null;
        private final Map<Integer, Button> buttons = new HashMap<>();

        public SimpleMenuBuilder(MenuManager manager, Player p) {
            this.manager = manager;
            this.p = p;
        }

        public SimpleMenuBuilder title(String title) {
            this.title = title;
            return this;
        }

        public SimpleMenuBuilder size(int size) {
            this.size = size;
            return this;
        }

        public SimpleMenuBuilder rows(int rows) {
            this.size = rows * 9;
            return this;
        }

        public SimpleMenuBuilder update(boolean update) {
            this.update = update;
            return this;
        }

        public SimpleMenuBuilder button(int slot, Button button) {
            this.buttons.put(slot, button);
            return this;
        }

        public SimpleMenuBuilder fillBorder(Button button) {
            this.fillBorder = true;
            this.borderButton = button;
            return this;
        }

        public SimpleMenu build() {
            SimpleMenu menu = new SimpleMenu(manager, p, title, size, update, p -> buttons);

            if (fillBorder && borderButton != null) {
                menu.setFillEnabled(false);
            }

            return menu;
        }

        public void openMenu() {
            build().open();
        }
    }
}