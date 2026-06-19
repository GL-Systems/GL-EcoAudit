package org.glstudio.economy.common.database;

import com.zaxxer.hikari.HikariConfig;

public class H2Provider extends SQLDatabaseProvider {

    private final String filePath;

    public H2Provider(String filePath) {
        this.filePath = filePath;
    }

    @Override
    protected String getJdbcUrl() {
        return "jdbc:h2:" + filePath + ";MODE=MySQL;DB_CLOSE_DELAY=-1";
    }

    @Override
    protected void configureConfig(HikariConfig config) {
        config.setDriverClassName("org.h2.Driver");
        config.setConnectionTestQuery("SELECT 1");
    }
}