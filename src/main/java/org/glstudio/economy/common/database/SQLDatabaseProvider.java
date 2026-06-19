package org.glstudio.economy.common.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.Setter;

import java.sql.Connection;
import java.sql.PreparedStatement;

public abstract class SQLDatabaseProvider implements DatabaseProvider {

    protected HikariDataSource dataSource;
    @Setter
    protected int connectionTimeoutMs = 30000;

    protected abstract String getJdbcUrl();

    protected abstract void configureConfig(HikariConfig config);

    protected void configureDefaultConfig(HikariConfig config) {
        config.setMaximumPoolSize(25);
        config.setMinimumIdle(5);
        config.setConnectionTimeout(connectionTimeoutMs);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);
        config.setLeakDetectionThreshold(60000);
        config.setKeepaliveTime(300000);
        config.setConnectionTestQuery("SELECT 1");
    }

    @Override
    public void initialize() throws Exception {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(getJdbcUrl());
        configureDefaultConfig(config);
        configureConfig(config);
        this.dataSource = new HikariDataSource(config);
    }

    @Override
    public void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }

    @Override
    public boolean isInitialized() {
        return dataSource != null && !dataSource.isClosed();
    }

    public Connection getConnection() throws Exception {
        return dataSource.getConnection();
    }

    public void executeUpdate(String sql, Object... params) throws Exception {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                stmt.setObject(i + 1, params[i]);
            }
            stmt.executeUpdate();
        }
    }
}