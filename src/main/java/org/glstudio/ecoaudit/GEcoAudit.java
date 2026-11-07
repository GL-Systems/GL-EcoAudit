package org.glstudio.ecoaudit;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.plugin.java.JavaPlugin;
import org.glstudio.ecoaudit.core.ConfigManager;
import org.glstudio.ecoaudit.core.ShutdownService;
import org.glstudio.ecoaudit.core.StartupService;
import org.glstudio.ecoaudit.modules.database.DatabaseProvider;
import org.glstudio.ecoaudit.modules.database.repository.TransactionRepository;
import org.glstudio.ecoaudit.modules.manager.TransactionManager;
import org.glstudio.ecoaudit.modules.service.AuditService;
import org.glstudio.ecoaudit.modules.service.TransactionService;
import org.glstudio.nexus.Nexus;
import org.glstudio.nexus.concurrent.NexusExecutors;
import org.glstudio.nexus.modules.command.CommandManager;
import org.glstudio.nexus.modules.lang.LanguageFile;
import org.glstudio.nexus.modules.menu.MenuManager;
import org.glstudio.nexus.modules.menu.chat.ChatInputManager;
import org.glstudio.nexus.utils.Tasks;

/**
 * Entry point of GL-EcoAudit, which records every {@code /pay} and lets players browse their payment
 * history. Holds the plugin's services; wiring lives in {@link StartupService}.
 */
@Getter
@Setter
public final class GEcoAudit extends JavaPlugin {

    private ConfigManager configManager;
    private LanguageFile messages;
    private CommandManager commandManager;
    private MenuManager menuManager;
    private ChatInputManager chatInput;
    private NexusExecutors executors;
    private Tasks tasks;

    private AuditService auditService;
    private TransactionService transactionService;

    private DatabaseProvider databaseProvider;
    private TransactionManager transactionManager;
    private TransactionRepository transactionRepository;

    @Override
    public void onEnable() {
        Nexus.init(this);
        if (!new StartupService(this).start()) {
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        new ShutdownService(this).stop();
        Nexus.shutdown(this);
    }
}
