package org.glstudio.ecoaudit.core;

import org.glstudio.ecoaudit.GEcoAudit;
import org.glstudio.nexus.utils.ConfigFile;
import org.jetbrains.annotations.Nullable;

/**
 * Reloads the configuration, language and menu files. The database is not restarted.
 */
public final class ReloadService {

    private final GEcoAudit plugin;

    /**
     * Creates the reload service.
     *
     * @param plugin the plugin
     */
    public ReloadService(GEcoAudit plugin) {
        this.plugin = plugin;
    }

    /**
     * Re-reads every file. Nothing changes if any of them has a YAML error.
     *
     * @return {@code null} on success, otherwise the file and the parser's error
     */
    public @Nullable String reload() {
        ConfigManager configs = plugin.getConfigManager();
        for (ConfigFile file : configs.all()) {
            String error = file.validate();
            if (error != null) return file.getFileName() + ": " + error;
        }
        configs.reloadConfigs();
        plugin.getMessages().applyTo(plugin.getCommandManager(), "GLOBAL.NO_PERMISSION", "GLOBAL.ONLY_PLAYERS");
        return null;
    }
}
