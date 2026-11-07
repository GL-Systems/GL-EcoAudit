package org.glstudio.ecoaudit.modules.database.impl;

import org.glstudio.nexus.storage.PoolOptions;

/**
 * MySQL backend.
 */
public class MySQLProvider extends SQLDatabaseProvider {

    private final String host;
    private final int port;
    private final String database;
    private final String username;
    private final String password;

    /**
     * Creates the provider.
     *
     * @param host     the server host
     * @param port     the server port
     * @param database the database name
     * @param username the user
     * @param password the password
     */
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
    protected String username() {
        return username;
    }

    @Override
    protected String password() {
        return password;
    }

    @Override
    protected PoolOptions configure(PoolOptions options) {
        return options.driver("com.mysql.cj.jdbc.Driver");
    }
}
