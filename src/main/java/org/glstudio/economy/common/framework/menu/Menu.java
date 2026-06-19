package org.glstudio.economy.common.framework.menu;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.DoubleChestInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.glstudio.economy.common.framework.menu.button.Button;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
public abstract class Menu {

    private final JavaPlugin plugin;
    private final MenuManager manager;

    protected Map<Integer, Button> buttons;
    protected Player p;
    protected Inventory inventory;
    protected BukkitTask updater;
    protected String title;
    protected int size;

    protected ItemStack filler;
    protected boolean fillEnabled;
    protected boolean allowInteract;

    public Menu(MenuManager manager, Player p, String title, int size, boolean update) {
        this(manager, manager.getPlugin(), p, title, size, update);
    }

    public Menu(MenuManager manager, JavaPlugin plugin, Player p, String title, int size, boolean update) {
        this.manager = manager;
        this.plugin = plugin;
        this.p = p;
        this.inventory = Bukkit.createInventory(null, size, title);
        this.updater = (update ? Bukkit.getScheduler().runTaskTimer(plugin, this::update, 0L, 10L) : null);
        this.title = title;
        this.size = size;
        this.fillEnabled = false;
        this.allowInteract = false;
    }

    public Menu(MenuManager manager, JavaPlugin plugin, Player p, String title, boolean update, Inventory inventory) {
        this.manager = manager;
        this.plugin = plugin;
        this.p = p;
        this.inventory = (inventory instanceof DoubleChestInventory ?
                Bukkit.createInventory(null, 54, title) :
                Bukkit.createInventory(null, inventory.getType(), title));
        this.updater = (update ? Bukkit.getScheduler().runTaskTimer(plugin, this::update, 0L, 10L) : null);
        this.title = title;
        this.size = inventory.getSize();
        this.fillEnabled = false;
        this.allowInteract = false;
    }

    public void open() {
        this.buttons = getButtons(p);

        for (Map.Entry<Integer, Button> entry : buttons.entrySet()) {
            inventory.setItem(entry.getKey(), entry.getValue().getItemStack());
        }

        if (fillEnabled) {
            for (int i = 0; i < inventory.getSize(); i++) {
                ItemStack item = inventory.getItem(i);

                if (item == null || item.getType() == Material.AIR) {
                    inventory.setItem(i, filler);
                }
            }
        }

        p.openInventory(inventory);
        getManager().getMenus().put(p.getUniqueId(), this);
    }

    public void update() {
        if (!p.isOnline()) {
            destroy();
            return;
        }

        Map<Integer, Button> buttons = (this.buttons = getButtons(p));

        for (Map.Entry<Integer, Button> entry : buttons.entrySet()) {
            inventory.setItem(entry.getKey(), entry.getValue().getItemStack());
        }

        if (fillEnabled) {
            for (int i = 0; i < inventory.getSize(); i++) {
                ItemStack item = inventory.getItem(i);

                if (item == null || item.getType() == Material.AIR) {
                    inventory.setItem(i, filler);
                }
            }
        }
    }

    public void destroy() {
        if (buttons != null) {
            buttons.clear();
        }
        inventory.clear();

        if (updater != null) {
            updater.cancel();
        }
    }

    public void onClick(InventoryClickEvent e) {
    }

    public void onClose() {
    }

    public abstract Map<Integer, Button> getButtons(Player p);

    public void setButton(int slot, Button button) {
        if (buttons == null) {
            buttons = new HashMap<>();
        }
        buttons.put(slot, button);
        inventory.setItem(slot, button.getItemStack());
    }

    public void removeButton(int slot) {
        if (buttons != null) {
            buttons.remove(slot);
        }
        inventory.setItem(slot, null);
    }

    public void fillEmpty(Button button) {
        for (int i = 0; i < inventory.getSize(); i++) {
            ItemStack item = inventory.getItem(i);
            if (item == null || item.getType() == Material.AIR) {
                setButton(i, button);
            }
        }
    }

    public void fillBorder(Button button) {
        int size = inventory.getSize();
        int rows = size / 9;

        for (int i = 0; i < 9; i++) {
            setButton(i, button);
            if (rows > 1) {
                setButton(size - 9 + i, button);
            }
        }

        for (int i = 1; i < rows - 1; i++) {
            setButton(i * 9, button);
            setButton(i * 9 + 8, button);
        }
    }

    public void setRow(int row, Button... buttons) {
        for (int i = 0; i < buttons.length && i < 9; i++) {
            setButton(row * 9 + i, buttons[i]);
        }
    }

    public void setColumn(int column, Button... buttons) {
        for (int i = 0; i < buttons.length; i++) {
            setButton(i * 9 + column, buttons[i]);
        }
    }

    public int[] getCenterSlots() {
        int size = inventory.getSize();
        int rows = size / 9;

        int[] slots = new int[(rows - 2) * 7];
        int index = 0;

        for (int row = 1; row < rows - 1; row++) {
            for (int col = 1; col < 8; col++) {
                slots[index++] = row * 9 + col;
            }
        }

        return slots;
    }
}