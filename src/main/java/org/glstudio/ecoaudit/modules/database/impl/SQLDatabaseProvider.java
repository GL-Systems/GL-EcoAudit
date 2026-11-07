package org.glstudio.ecoaudit.modules.database.impl;

import com.zaxxer.hikari.HikariDataSource;
import lombok.Setter;
import org.glstudio.ecoaudit.modules.database.DatabaseProvider;
import org.glstudio.nexus.storage.DataSources;
import org.glstudio.nexus.storage.PoolOptions;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Base of the SQL backends: a HikariCP pool over the backend's JDBC URL.
 */
public abstract class SQLDatabaseProvider implements DatabaseProvider {

    /** The pool, set by {@link #initialize()}. */
    protected HikariDataSource dataSource;
    /** How long to wait for a connection, in milliseconds. */
    @Setter
    protected int connectionTimeoutMs = 30000;

    /**
     * Returns the JDBC URL.
     *
     * @return the URL
     */
    /**
     * Returns the column type of a floating-point amount.
     *
     * @return {@code DOUBLE} unless the backend spells it differently
     */
    public String doubleType() {
        return "DOUBLE";
    }

    protected abstract String getJdbcUrl();

    /**
     * Returns the user to connect as.
     *
     * @return the user, {@code null} for none
     */
    protected String username() {
        return null;
    }

    /**
     * Returns the password to connect with.
     *
     * @return the password, {@code null} for none
     */
    protected String password() {
        return null;
    }

    /**
     * Adjusts the pool settings for the backend.
     *
     * @param options the shared defaults
     * @return the options to use
     */
    protected PoolOptions configure(PoolOptions options) {
        return options;
    }

    @Override
    public void initialize() throws Exception {
        PoolOptions options = PoolOptions.named("GL-EcoAudit")
                .maximumPoolSize(25)
                .minimumIdle(5)
                .timeouts(connectionTimeoutMs, 600000, 1800000, 300000)
                .leakDetection(60000);
        this.dataSource = DataSources.create(getJdbcUrl(), username(), password(), configure(options));
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

    /**
     * Borrows a connection; the caller closes it.
     *
     * @return the connection
     * @throws SQLException if none is available
     */
    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    /**
     * Runs an update with positional parameters.
     *
     * @param sql    the statement
     * @param params the parameters, bound with {@code setObject}
     * @throws SQLException if it fails
     */
    public void executeUpdate(String sql, Object... params) throws SQLException {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                stmt.setObject(i + 1, params[i]);
            }
            stmt.executeUpdate();
        }
    }
}
