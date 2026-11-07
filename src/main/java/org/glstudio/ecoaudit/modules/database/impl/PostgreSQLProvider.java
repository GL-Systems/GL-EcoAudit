package org.glstudio.ecoaudit.modules.database.impl;

import org.glstudio.nexus.storage.PoolOptions;

/**
 * PostgreSQL backend.
 */
public class PostgreSQLProvider extends SQLDatabaseProvider {

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
    public PostgreSQLProvider(String host, int port, String database, String username, String password) {
        this.host = host;
        this.port = port;
        this.database = database;
        this.username = username;
        this.password = password;
    }

    @Override
    protected String getJdbcUrl() {
        return "jdbc:postgresql://" + host + ":" + port + "/" + database;
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
        return options.driver("org.postgresql.Driver");
    }

    @Override
    public String doubleType() {
        return "DOUBLE PRECISION";
    }
}
