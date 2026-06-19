package org.glstudio.economy.common.framework.menu;

import lombok.Getter;
import org.bukkit.plugin.java.JavaPlugin;
import org.glstudio.economy.common.framework.menu.listener.MenuListener;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Getter
public class MenuManager {

    private final JavaPlugin plugin;
    private final Map<UUID, Menu> menus;

    private MenuListener menuListener;

    public MenuManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.menus = new ConcurrentHashMap<>();

        this.initialize();
    }

    private void initialize() {
        this.menuListener = new MenuListener(this);
        plugin.getServer().getPluginManager().registerEvents(menuListener, plugin);
    }
}