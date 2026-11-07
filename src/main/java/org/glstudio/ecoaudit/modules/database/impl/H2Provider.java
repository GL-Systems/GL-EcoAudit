package org.glstudio.ecoaudit.modules.database.impl;

import org.glstudio.nexus.storage.DataSources;
import org.glstudio.nexus.storage.PoolOptions;
import org.glstudio.nexus.utils.Diagnostics;

import java.sql.SQLException;

/**
 * Embedded H2 backend in MySQL compatibility mode.
 */
public class H2Provider extends SQLDatabaseProvider {

    private final String filePath;

    /**
     * Creates the provider.
     *
     * @param filePath absolute path of the database, without extension
     */
    public H2Provider(String filePath) {
        this.filePath = filePath;
    }

    @Override
    protected String getJdbcUrl() {
        return "jdbc:h2:" + filePath + ";MODE=MySQL;DB_CLOSE_DELAY=-1";
    }

    @Override
    protected PoolOptions configure(PoolOptions options) {
        return options.driver("org.h2.Driver");
    }

    @Override
    public void shutdown() {
        if (dataSource == null || dataSource.isClosed()) {
            return;
        }
        try {
            DataSources.closeH2(dataSource);
        } catch (SQLException e) {
            Diagnostics.logFailure("H2Provider#shutdown", e);
        }
    }
}
