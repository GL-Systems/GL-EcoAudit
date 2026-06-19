package org.glstudio.economy.core;

import lombok.Getter;
import org.glstudio.economy.GLEcoAudit;
import org.glstudio.nexus.utils.ConfigFile;

@Getter
public class ConfigManager {

    private final ConfigFile mainConfig, langConfig, databaseConfig;
    private final ConfigFile transactionsMenu, negativeTransactionsMenu, positiveTransactionsMenu;

    public ConfigManager(GLEcoAudit plugin) {
        this.mainConfig = new ConfigFile(plugin, "config.yml");
        this.langConfig = new ConfigFile(plugin, "language.yml");
        this.databaseConfig = new ConfigFile(plugin, "database.yml");
        this.transactionsMenu = new ConfigFile(plugin, "menus", "transactions-menu.yml");
        this.negativeTransactionsMenu = new ConfigFile(plugin, "menus", "transactions-sent.yml");
        this.positiveTransactionsMenu = new ConfigFile(plugin, "menus", "transactions-received.yml");
    }

    public void reloadConfigs() {
        mainConfig.reload();
        langConfig.reload();
        databaseConfig.reload();
        transactionsMenu.reload();
        negativeTransactionsMenu.reload();
        positiveTransactionsMenu.reload();
    }
}