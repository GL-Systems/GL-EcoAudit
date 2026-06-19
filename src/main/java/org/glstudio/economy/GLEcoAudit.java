package org.glstudio.economy;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.glstudio.economy.common.framework.menu.MenuManager;
import org.glstudio.economy.core.ConfigManager;
import org.glstudio.economy.core.ShutdownService;
import org.glstudio.economy.core.StartupService;
import org.glstudio.economy.common.database.DatabaseProvider;
import org.glstudio.economy.common.database.TransactionRepository;
import org.glstudio.economy.modules.manager.TransactionManager;
import org.glstudio.economy.modules.services.AuditService;
import org.glstudio.economy.modules.services.TransactionService;
import org.glstudio.economy.modules.services.VaultService;
import org.glstudio.nexus.modules.command.CommandManager;
import org.glstudio.nexus.utils.LoggerUtils;

@Getter
public final class GLEcoAudit extends JavaPlugin {

    // ─── Core managers
    private ConfigManager configManager;
    private CommandManager commandManager;
    private MenuManager menuManager;

    // ─── Services
    private AuditService auditService;
    private TransactionService transactionService;
    private VaultService vaultService;

    // ─── Repository Services
    private DatabaseProvider databaseProvider;
    private TransactionManager transactionManager;
    private TransactionRepository transactionRepository;

    @Override
    public void onEnable() {
        this.configManager = new ConfigManager(this);
        this.commandManager = new CommandManager(this);
        this.menuManager = new MenuManager(this);
        this.transactionManager = new TransactionManager();

        StartupService startupService = new StartupService(this);

        if (!startupService.run()) {
            LoggerUtils.logError("Plugin initialization failed! Disabling...");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        this.databaseProvider = startupService.getDatabaseProvider();
        this.transactionRepository = startupService.getTransactionRepository();
        this.vaultService = startupService.getVaultService();
        this.transactionService = startupService.getTransactionService();
        this.auditService = startupService.getAuditService();

        schedulePurgeTask();
    }

    @Override
    public void onDisable() {
        ShutdownService shutdownService = new ShutdownService(this);
        shutdownService.run();
    }

    private void schedulePurgeTask() {
        int purgeAfterDays = configManager.getMainConfig().getInt("TRANSACTIONS.PURGE_AFTER_DAYS", 0);
        if (purgeAfterDays <= 0) return;

        long cutoff = System.currentTimeMillis() - (purgeAfterDays * 86400000L);

        Bukkit.getScheduler().runTaskTimerAsynchronously(this, () ->
                transactionService.purgeOldRecords(cutoff), 0L, 72000L);

        getLogger().info("Auto-purge enabled: records older than " + purgeAfterDays + " days will be removed.");
    }
}