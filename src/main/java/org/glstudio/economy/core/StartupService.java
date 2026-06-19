package org.glstudio.economy.core;

import lombok.Getter;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.glstudio.economy.GLEcoAudit;
import org.glstudio.economy.modules.command.EcoAuditCommand;
import org.glstudio.economy.modules.command.TransactionsCommand;
import org.glstudio.economy.common.database.DatabaseProvider;
import org.glstudio.economy.common.database.H2Provider;
import org.glstudio.economy.common.database.MongoDBProvider;
import org.glstudio.economy.common.database.MySQLProvider;
import org.glstudio.economy.common.database.PostgreSQLProvider;
import org.glstudio.economy.common.database.SQLDatabaseProvider;
import org.glstudio.economy.common.database.SQLiteProvider;
import org.glstudio.economy.common.database.TransactionRepository;
import org.glstudio.economy.modules.listener.TransactionsListener;
import org.glstudio.economy.modules.services.AuditService;
import org.glstudio.economy.modules.services.TransactionService;
import org.glstudio.economy.modules.services.VaultService;
import org.glstudio.nexus.modules.command.CommandManager;
import org.glstudio.nexus.utils.ConfigFile;
import org.glstudio.nexus.utils.LoggerUtils;

import java.io.File;

@Getter
public class StartupService {

    private final GLEcoAudit plugin;

    private Economy economy;
    private VaultService vaultService;
    private TransactionService transactionService;
    private AuditService auditService;
    private DatabaseProvider databaseProvider;
    private TransactionRepository transactionRepository;

    public StartupService(GLEcoAudit plugin) {
        this.plugin = plugin;
    }

    public boolean run() {
        if (!setupEconomy()) {
            LoggerUtils.logError(String.format("[%s] - Disabled due to no Vault dependency found!", plugin.getName()));
            plugin.getServer().getPluginManager().disablePlugin(plugin);
            return false;
        }

        if (!setupDatabase()) {
            LoggerUtils.logError("Failed to initialize database! Disabling...");
            plugin.getServer().getPluginManager().disablePlugin(plugin);
            return false;
        }

        registerServices();
        registerCommands();
        registerListeners();

        LoggerUtils.sendEnable(plugin);
        return true;
    }

    private boolean setupEconomy() {
        if (plugin.getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = plugin.getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        this.economy = rsp.getProvider();
        return true;
    }

    private boolean setupDatabase() {
        ConfigFile dbConfig = plugin.getConfigManager().getDatabaseConfig();
        String type = dbConfig.getString("TYPE", "SQLITE").toUpperCase();

        LoggerUtils.logInfo("Selected database type: " + type);

        DatabaseProvider provider;

        try {
            switch (type) {
                case "MYSQL": {
                    String host = dbConfig.getString("SETTINGS.MYSQL.IP_ADDRESS", "localhost");
                    int port = dbConfig.getInt("SETTINGS.MYSQL.PORT", 3306);
                    String database = dbConfig.getString("SETTINGS.MYSQL.DATABASE", "GL-EcoAudit");
                    String user = dbConfig.getString("SETTINGS.MYSQL.AUTH.USERNAME", "");
                    String pass = dbConfig.getString("SETTINGS.MYSQL.AUTH.PASSWORD", "");
                    provider = new MySQLProvider(host, port, database, user, pass);
                    break;
                }
                case "H2": {
                    String fileName = dbConfig.getString("SETTINGS.H2.FILE_NAME", "GL-EcoAudit");
                    String path = new File(plugin.getDataFolder(), fileName).getAbsolutePath();
                    provider = new H2Provider(path);
                    break;
                }
                case "POSTGRESQL": {
                    String host = dbConfig.getString("SETTINGS.POSTGRESQL.IP_ADDRESS", "localhost");
                    int port = dbConfig.getInt("SETTINGS.POSTGRESQL.PORT", 5432);
                    String database = dbConfig.getString("SETTINGS.POSTGRESQL.DATABASE", "GL-EcoAudit");
                    String user = dbConfig.getString("SETTINGS.POSTGRESQL.AUTH.USERNAME", "");
                    String pass = dbConfig.getString("SETTINGS.POSTGRESQL.AUTH.PASSWORD", "");
                    provider = new PostgreSQLProvider(host, port, database, user, pass);
                    break;
                }
                case "MONGO": {
                    String authType = dbConfig.getString("SETTINGS.MONGO.AUTH_TYPE", "URI");
                    String uri = dbConfig.getString("SETTINGS.MONGO.URI", "");
                    String database = dbConfig.getString("SETTINGS.MONGO.DATABASE", "GL-EcoAudit");
                    if ("URI".equalsIgnoreCase(authType) && !uri.isEmpty()) {
                        provider = new MongoDBProvider(uri, database);
                    } else {
                        String ip = dbConfig.getString("SETTINGS.MONGO.IP_ADDRESS", "localhost");
                        int port = dbConfig.getInt("SETTINGS.MONGO.PORT", 27017);
                        boolean auth = dbConfig.getBoolean("SETTINGS.MONGO.AUTH.ENABLED");
                        String user = dbConfig.getString("SETTINGS.MONGO.AUTH.USERNAME", "");
                        String pass = dbConfig.getString("SETTINGS.MONGO.AUTH.PASSWORD", "");
                        String connStr = "mongodb://" + (auth ? user + ":" + pass + "@" : "") + ip + ":" + port + "/" + database;
                        provider = new MongoDBProvider(connStr, database);
                    }
                    break;
                }
                default: {
                    plugin.getLogger().warning("Unknown database type '" + type + "'. Falling back to SQLite.");
                    String fileName = dbConfig.getString("SETTINGS.SQLITE.FILE_NAME", "GL-EcoAudit");
                    String path = new File(plugin.getDataFolder(), fileName + ".db").getAbsolutePath();
                    provider = new SQLiteProvider(path);
                    break;
                }
            }

            int timeoutSec = dbConfig.getInt("INIT_TIMEOUT_SECONDS", 10);
            if (provider instanceof SQLDatabaseProvider) {
                ((SQLDatabaseProvider) provider).setConnectionTimeoutMs(timeoutSec * 1000);
            }

            LoggerUtils.logInfo("Initializing database connection...");
            provider.initialize();

            this.databaseProvider = provider;
            this.transactionRepository = TransactionRepository.create(provider, "transactions");
            transactionRepository.createTable();
            LoggerUtils.logSuccess("Database connected successfully.");
            return true;

        } catch (Exception e) {
            LoggerUtils.logException("Database initialization", e);
            return false;
        }
    }

    private void registerServices() {
        this.vaultService = new VaultService(economy);
        this.transactionService = new TransactionService(
                plugin,
                transactionRepository,
                plugin.getTransactionManager()
        );
        this.auditService = new AuditService(plugin, transactionService);

        LoggerUtils.logSuccess("Services registered.");
    }

    private void registerCommands() {
        CommandManager manager = plugin.getCommandManager();
        manager.register(new TransactionsCommand(manager, "transactions", "golden.ecoaudit.transactions", plugin));
        manager.register(new EcoAuditCommand(manager, "ecoaudit", "golden.ecoaudit.admin", plugin));
        LoggerUtils.logSuccess("Commands registered.");
    }

    private void registerListeners() {
        PluginManager pm = Bukkit.getPluginManager();
        pm.registerEvents(new TransactionsListener(plugin, auditService), plugin);
        LoggerUtils.logSuccess("Listeners registered.");
    }
}