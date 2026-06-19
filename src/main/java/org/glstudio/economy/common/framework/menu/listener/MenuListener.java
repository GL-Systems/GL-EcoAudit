package org.glstudio.economy.common.framework.menu.listener;

import org.glstudio.economy.common.framework.menu.Menu;
import org.glstudio.economy.common.framework.menu.MenuManager;
import org.glstudio.economy.common.framework.menu.button.Button;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerJoinEvent;

public class MenuListener implements Listener {

    private final MenuManager menuManager;

    public MenuListener(MenuManager menuManager) {
        this.menuManager = menuManager;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        if (e.getClickedInventory() == null) return;

        Menu menu = menuManager.getMenus().get(p.getUniqueId());

        if (menu != null) {
            menu.onClick(e);

            if (!menu.isAllowInteract()) {
                e.setCancelled(true);
            }

            if (e.getClickedInventory() != p.getInventory() && menu.getButtons() != null) {
                Button button = menu.getButtons().get(e.getSlot());
                if (button != null) {
                    button.onClick(e);
                }
            }
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        Menu menu = menuManager.getMenus().get(p.getUniqueId());

        if (menu != null) {
            menu.onClose();
            menu.destroy();
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent e) {
        Player p = (Player) e.getPlayer();
        Menu menu = menuManager.getMenus().remove(p.getUniqueId());

        if (menu != null) {
            menu.onClose();
            menu.destroy();
        }
    }
}