package org.glstudio.ecoaudit.modules.database.impl;

import org.glstudio.nexus.storage.PoolOptions;

/**
 * SQLite backend with WAL journaling. A single connection, since SQLite serialises writes anyway.
 */
public class SQLiteProvider extends SQLDatabaseProvider {

    private final String filePath;

    /**
     * Creates the provider.
     *
     * @param filePath absolute path of the database file
     */
    public SQLiteProvider(String filePath) {
        this.filePath = filePath;
    }

    @Override
    protected String getJdbcUrl() {
        return "jdbc:sqlite:" + filePath + "?journal_mode=WAL&foreign_keys=true";
    }

    @Override
    protected PoolOptions configure(PoolOptions options) {
        return options.driver("org.sqlite.JDBC").maximumPoolSize(1);
    }
}
