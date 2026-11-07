package org.glstudio.ecoaudit.modules.database;

/**
 * A connection to one of the supported backends.
 */
public interface DatabaseProvider {

    /**
     * Connects. Blocking.
     *
     * @throws Exception if the backend cannot be reached
     */
    void initialize() throws Exception;

    /**
     * Closes the connection.
     */
    void shutdown();

    /**
     * Returns whether the connection is open.
     *
     * @return {@code true} after a successful {@link #initialize()}
     */
    boolean isInitialized();
}
