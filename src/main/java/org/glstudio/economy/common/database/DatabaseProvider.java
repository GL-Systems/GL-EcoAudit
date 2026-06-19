package org.glstudio.economy.common.database;

public interface DatabaseProvider {

    void initialize() throws Exception;

    void shutdown();

    boolean isInitialized();
}