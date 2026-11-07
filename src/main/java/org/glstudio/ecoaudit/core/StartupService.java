package org.glstudio.ecoaudit.core;

import org.bukkit.plugin.PluginManager;
import org.glstudio.ecoaudit.GEcoAudit;
import org.glstudio.ecoaudit.core.util.Permissions;
import org.glstudio.ecoaudit.modules.command.TransactionsCommand;
import org.glstudio.ecoaudit.modules.command.ecoaudit.EcoAuditCommand;
import org.glstudio.ecoaudit.modules.database.DatabaseProvider;
import org.glstudio.ecoaudit.modules.database.impl.H2Provider;
import org.glstudio.ecoaudit.modules.database.impl.MongoDBProvider;
import org.glstudio.ecoaudit.modules.database.impl.MySQLProvider;
import org.glstudio.ecoaudit.modules.database.impl.PostgreSQLProvider;
import org.glstudio.ecoaudit.modules.database.impl.SQLDatabaseProvider;
import org.glstudio.ecoaudit.modules.database.impl.SQLiteProvider;
import org.glstudio.ecoaudit.modules.database.repository.TransactionRepository;
import org.glstudio.ecoaudit.modules.listener.TransactionsListener;
import org.glstudio.ecoaudit.modules.manager.TransactionManager;
import org.glstudio.ecoaudit.modules.service.AuditService;
import org.glstudio.ecoaudit.modules.service.TransactionService;
import org.glstudio.nexus.api.NexusAPI;
import org.glstudio.nexus.concurrent.NexusExecutors;
import org.glstudio.nexus.hooks.economy.VaultEconomy;
import org.glstudio.nexus.modules.command.CommandManager;
import org.glstudio.nexus.modules.lang.LanguageFile;
import org.glstudio.nexus.storage.MongoConnector;
import org.glstudio.nexus.utils.ConfigFile;
import org.glstudio.nexus.utils.Diagnostics;
import org.glstudio.nexus.utils.LoggerUtils;
import org.glstudio.nexus.utils.Tasks;
import org.glstudio.nexus.utils.TextFormat;

import java.io.File;
import java.util.function.BooleanSupplier;

/**
 * Starts the plugin's services in dependency order.
 */
public class StartupService {

    private final GEcoAudit plugin;

    /**
     * Creates the startup sequence.
     *
     * @param plugin the plugin being enabled
     */
    public StartupService(GEcoAudit plugin) {
        this.plugin = plugin;
    }

    /**
     * Runs every startup step.
     *
     * @return {@code false} if a step failed and the plugin should disable itself
     */
    public boolean start() {
        boolean started = step("loadConfigs", this::loadConfigs)
                && step("loadExecutors", this::loadExecutors)
                && step("loadIntegrations", this::loadIntegrations)
                && step("loadDatabase", this::loadDatabase)
                && step("loadManagers", this::loadManagers)
                && step("registerCommands", this::registerCommands)
                && step("registerListeners", this::registerListeners)
                && step("startTasks", this::startTasks);
        if (!started) return false;

        LoggerUtils.sendEnable(plugin);
        return true;
    }

    private boolean step(String name, BooleanSupplier action) {
        try {
            return action.getAsBoolean();
        } catch (RuntimeException e) {
            Diagnostics.logFailure("StartupService#" + name, e);
            return false;
        }
    }

    private boolean loadConfigs() {
        ConfigManager configs = new ConfigManager(plugin);
        plugin.setConfigManager(configs);
        plugin.setMessages(new LanguageFile(configs.getLangConfig(), TextFormat.LEGACY).prefixPath(null));
        return true;
    }

    private boolean loadExecutors() {
        plugin.setExecutors(NexusExecutors.builder(plugin.getName()).ioThreads(4).ordered("storage").build());
        plugin.setTasks(new Tasks(plugin));
        return true;
    }

    private boolean loadIntegrations() {
        if (!VaultEconomy.get().isAvailable()) {
            LoggerUtils.logError(String.format("[%s] - Disabled due to no Vault dependency found!", plugin.getName()));
            return false;
        }
        return true;
    }

    private boolean loadDatabase() {
        ConfigFile dbConfig = plugin.getConfigManager().getDatabaseConfig();
        String type = dbConfig.getString("TYPE", "SQLITE").toUpperCase();

        LoggerUtils.logInfo("Selected database type: " + type);

        try {
            DatabaseProvider provider = createProvider(dbConfig, type);

            int timeoutSec = dbConfig.getInt("INIT_TIMEOUT_SECONDS", 10);
            if (provider instanceof SQLDatabaseProvider sql) {
                sql.setConnectionTimeoutMs(timeoutSec * 1000);
            }

            LoggerUtils.logInfo("Initializing database connection...");
            provider.initialize();

            plugin.setDatabaseProvider(provider);
            TransactionRepository repository = TransactionRepository.create(provider, "transactions");
            repository.createTable();
            plugin.setTransactionRepository(repository);
            LoggerUtils.logSuccess("Database connected successfully.");
            return true;
        } catch (Exception e) {
            Diagnostics.logFailure("Database initialization", e);
            LoggerUtils.logError("Failed to initialize database! Disabling...");
            return false;
        }
    }

    private DatabaseProvider createProvider(ConfigFile dbConfig, String type) {
        return switch (type) {
            case "MYSQL" -> new MySQLProvider(
                    dbConfig.getString("SETTINGS.MYSQL.IP_ADDRESS", "localhost"),
                    dbConfig.getInt("SETTINGS.MYSQL.PORT", 3306),
                    dbConfig.getString("SETTINGS.MYSQL.DATABASE", "GL-EcoAudit"),
                    dbConfig.getString("SETTINGS.MYSQL.AUTH.USERNAME", ""),
                    dbConfig.getString("SETTINGS.MYSQL.AUTH.PASSWORD", ""));
            case "H2" -> new H2Provider(new File(plugin.getDataFolder(),
                    dbConfig.getString("SETTINGS.H2.FILE_NAME", "GL-EcoAudit")).getAbsolutePath());
            case "POSTGRESQL" -> new PostgreSQLProvider(
                    dbConfig.getString("SETTINGS.POSTGRESQL.IP_ADDRESS", "localhost"),
                    dbConfig.getInt("SETTINGS.POSTGRESQL.PORT", 5432),
                    dbConfig.getString("SETTINGS.POSTGRESQL.DATABASE", "GL-EcoAudit"),
                    dbConfig.getString("SETTINGS.POSTGRESQL.AUTH.USERNAME", ""),
                    dbConfig.getString("SETTINGS.POSTGRESQL.AUTH.PASSWORD", ""));
            case "MONGO" -> mongoProvider(dbConfig);
            default -> {
                if (!"SQLITE".equals(type)) {
                    plugin.getLogger().warning("Unknown database type '" + type + "'. Falling back to SQLite.");
                }
                String fileName = dbConfig.getString("SETTINGS.SQLITE.FILE_NAME", "GL-EcoAudit");
                yield new SQLiteProvider(new File(plugin.getDataFolder(), fileName + ".db").getAbsolutePath());
            }
        };
    }

    private MongoDBProvider mongoProvider(ConfigFile dbConfig) {
        String authType = dbConfig.getString("SETTINGS.MONGO.AUTH_TYPE", "URI");
        String uri = dbConfig.getString("SETTINGS.MONGO.URI", "");
        String database = dbConfig.getString("SETTINGS.MONGO.DATABASE", "GL-EcoAudit");
        if ("URI".equalsIgnoreCase(authType) && !uri.isEmpty()) {
            return new MongoDBProvider(uri, database);
        }
        boolean auth = dbConfig.getBoolean("SETTINGS.MONGO.AUTH.ENABLED");
        // Credentials authenticate against the plugin's database, as the path of the old URI did.
        String connection = MongoConnector.uri(
                dbConfig.getString("SETTINGS.MONGO.IP_ADDRESS", "localhost"),
                dbConfig.getInt("SETTINGS.MONGO.PORT", 27017),
                auth ? dbConfig.getString("SETTINGS.MONGO.AUTH.USERNAME", "") : null,
                auth ? dbConfig.getString("SETTINGS.MONGO.AUTH.PASSWORD", "") : null,
                database);
        return new MongoDBProvider(connection, database);
    }

    private boolean loadManagers() {
        TransactionManager transactionManager = new TransactionManager();
        plugin.setTransactionManager(transactionManager);
        plugin.setMenuManager(NexusAPI.get().createMenuManager(plugin));
        plugin.setChatInput(NexusAPI.get().createChatInputManager(plugin));

        TransactionService transactionService = new TransactionService(plugin, plugin.getTransactionRepository(),
                transactionManager);
        plugin.setTransactionService(transactionService);
        plugin.setAuditService(new AuditService(plugin, transactionService));

        LoggerUtils.logSuccess("Services registered.");
        return true;
    }

    private boolean registerCommands() {
        CommandManager manager = NexusAPI.get().createCommandManager(plugin);
        plugin.setCommandManager(manager);
        plugin.getMessages().applyTo(manager, "GLOBAL.NO_PERMISSION", "GLOBAL.ONLY_PLAYERS");
        boolean registered = manager.registerAll(
                new TransactionsCommand(manager, "transactions", Permissions.TRANSACTIONS, plugin),
                new EcoAuditCommand(manager, "ecoaudit", Permissions.ADMIN, plugin));
        if (registered) LoggerUtils.logSuccess("Commands registered.");
        return registered;
    }

    private boolean registerListeners() {
        PluginManager pm = plugin.getServer().getPluginManager();
        pm.registerEvents(new TransactionsListener(plugin, plugin.getAuditService()), plugin);
        LoggerUtils.logSuccess("Listeners registered.");
        return true;
    }

    private boolean startTasks() {
        int purgeAfterDays = plugin.getConfigManager().getMainConfig().getInt("TRANSACTIONS.PURGE_AFTER_DAYS", 0);
        if (purgeAfterDays <= 0) return true;

        // The cutoff is taken once at startup, so each run deletes what was already old then.
        long cutoff = System.currentTimeMillis() - (purgeAfterDays * 86400000L);

        plugin.getTasks().timerAsync(0L, 72000L, () ->
                plugin.getExecutors().ordered("storage").execute(() ->
                        plugin.getTransactionService().purgeOldRecords(cutoff)));

        plugin.getLogger().info("Auto-purge enabled: records older than " + purgeAfterDays + " days will be removed.");
        return true;
    }
}
