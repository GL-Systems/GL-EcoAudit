package org.glstudio.economy.core;

import org.glstudio.economy.GLEcoAudit;
import org.glstudio.economy.common.database.DatabaseProvider;
import org.glstudio.nexus.utils.LoggerUtils;

public class ShutdownService {

    private final GLEcoAudit plugin;

    public ShutdownService(GLEcoAudit plugin) {
        this.plugin = plugin;
    }

    public void run() {
        if (plugin.getTransactionManager() != null) {
            plugin.getTransactionManager().clearAll();
        }

        DatabaseProvider provider = plugin.getDatabaseProvider();
        if (provider != null) {
            provider.shutdown();
            LoggerUtils.logInfo("Database disconnected.");
        }

        LoggerUtils.sendDisable(plugin);
    }
}