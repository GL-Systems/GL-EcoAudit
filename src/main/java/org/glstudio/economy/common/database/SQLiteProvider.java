package org.glstudio.economy.common.database;

import com.zaxxer.hikari.HikariConfig;

public class SQLiteProvider extends SQLDatabaseProvider {

    private final String filePath;

    public SQLiteProvider(String filePath) {
        this.filePath = filePath;
    }

    @Override
    protected String getJdbcUrl() {
        return "jdbc:sqlite:" + filePath + "?journal_mode=WAL&foreign_keys=true";
    }

    @Override
    protected void configureConfig(HikariConfig config) {
        config.setDriverClassName("org.sqlite.JDBC");
        config.setMaximumPoolSize(1);
        config.setConnectionTestQuery("SELECT 1");
    }
}