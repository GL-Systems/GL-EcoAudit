package org.glstudio.ecoaudit.core;

import lombok.Getter;
import org.glstudio.ecoaudit.GEcoAudit;
import org.glstudio.nexus.utils.ConfigFile;

import java.util.List;

/**
 * Owns the plugin's YAML files: configuration, language, database and the three menus.
 */
@Getter
public class ConfigManager {

    private final ConfigFile mainConfig, langConfig, databaseConfig;
    private final ConfigFile transactionsMenu, negativeTransactionsMenu, positiveTransactionsMenu;

    /**
     * Loads every file, copying it from the jar first if missing.
     *
     * @param plugin the plugin
     */
    public ConfigManager(GEcoAudit plugin) {
        this.mainConfig = new ConfigFile(plugin, "config.yml");
        this.langConfig = new ConfigFile(plugin, "language.yml");
        this.databaseConfig = new ConfigFile(plugin, "database.yml");
        this.transactionsMenu = new ConfigFile(plugin, "menus", "transactions-menu.yml");
        this.negativeTransactionsMenu = new ConfigFile(plugin, "menus", "transactions-sent.yml");
        this.positiveTransactionsMenu = new ConfigFile(plugin, "menus", "transactions-received.yml");
    }

    /**
     * Returns every file.
     *
     * @return the files
     */
    public List<ConfigFile> all() {
        return List.of(mainConfig, langConfig, databaseConfig, transactionsMenu, negativeTransactionsMenu,
                positiveTransactionsMenu);
    }

    /**
     * Re-reads every file from disk.
     */
    public void reloadConfigs() {
        mainConfig.reload();
        langConfig.reload();
        databaseConfig.reload();
        transactionsMenu.reload();
        negativeTransactionsMenu.reload();
        positiveTransactionsMenu.reload();
    }
}
