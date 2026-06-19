package org.glstudio.economy.common.database;

import com.zaxxer.hikari.HikariConfig;

public class MySQLProvider extends SQLDatabaseProvider {

    private final String host;
    private final int port;
    private final String database;
    private final String username;
    private final String password;

    public MySQLProvider(String host, int port, String database, String username, String password) {
        this.host = host;
        this.port = port;
        this.database = database;
        this.username = username;
        this.password = password;
    }

    @Override
    protected String getJdbcUrl() {
        return "jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    }

    @Override
    protected void configureConfig(HikariConfig config) {
        config.setDriverClassName("com.mysql.cj.jdbc.Driver");
        config.setUsername(username);
        config.setPassword(password);
        config.setConnectionTestQuery("SELECT 1");
    }
}