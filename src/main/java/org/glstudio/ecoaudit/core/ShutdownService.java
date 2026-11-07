package org.glstudio.ecoaudit.core;

import org.glstudio.ecoaudit.GEcoAudit;
import org.glstudio.nexus.utils.Diagnostics;
import org.glstudio.nexus.utils.LoggerUtils;

import java.time.Duration;

/**
 * Stops the plugin's services in reverse startup order; each step is isolated so one failure does
 * not skip the rest, and it is safe after a partial startup.
 */
public class ShutdownService {

    private final GEcoAudit plugin;

    /**
     * Creates the shutdown sequence.
     *
     * @param plugin the plugin being disabled
     */
    public ShutdownService(GEcoAudit plugin) {
        this.plugin = plugin;
    }

    /**
     * Runs every shutdown step. Pending writes are drained before the database closes.
     */
    public void stop() {
        step("closeMenus", () -> {
            if (plugin.getMenuManager() != null) plugin.getMenuManager().closeAll();
            if (plugin.getChatInput() != null) plugin.getChatInput().shutdown();
        });
        step("unregisterCommands", () -> {
            if (plugin.getCommandManager() != null) plugin.getCommandManager().unregisterAll();
        });
        step("clearCache", () -> {
            if (plugin.getTransactionManager() != null) plugin.getTransactionManager().clearAll();
        });
        step("drainExecutors", () -> {
            if (plugin.getExecutors() != null) plugin.getExecutors().shutdown(Duration.ofSeconds(10));
        });
        step("closeDatabase", () -> {
            if (plugin.getDatabaseProvider() != null) {
                plugin.getDatabaseProvider().shutdown();
                LoggerUtils.logInfo("Database disconnected.");
            }
        });
    }

    private void step(String name, Runnable action) {
        try {
            action.run();
        } catch (RuntimeException e) {
            Diagnostics.logFailure("ShutdownService#" + name, e);
        }
    }
}
