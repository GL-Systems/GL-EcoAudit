package org.glstudio.ecoaudit.modules.database.impl;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import lombok.Getter;
import org.bson.Document;
import org.glstudio.ecoaudit.modules.database.DatabaseProvider;
import org.glstudio.nexus.storage.MongoConnector;

import java.time.Duration;

/**
 * MongoDB backend.
 */
@Getter
public class MongoDBProvider implements DatabaseProvider {

    // The driver's own default; the connection used to wait this long before failing.
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(30);

    private MongoConnector.Handle handle;
    private final String connectionString;
    private final String databaseName;

    /**
     * Creates the provider.
     *
     * @param connectionString the connection URI
     * @param databaseName     the database to use
     */
    public MongoDBProvider(String connectionString, String databaseName) {
        this.connectionString = connectionString;
        this.databaseName = databaseName;
    }

    @Override
    public void initialize() {
        this.handle = MongoConnector.connect(connectionString, databaseName, CONNECT_TIMEOUT);
    }

    @Override
    public void shutdown() {
        if (handle != null) {
            handle.close();
        }
    }

    @Override
    public boolean isInitialized() {
        return handle != null;
    }

    /**
     * Returns the database.
     *
     * @return the database, {@code null} before {@link #initialize()}
     */
    public MongoDatabase getDatabase() {
        return handle == null ? null : handle.database();
    }

    /**
     * Returns a collection of the database.
     *
     * @param name the collection name
     * @return the collection
     */
    public MongoCollection<Document> getCollection(String name) {
        return handle.database().getCollection(name);
    }
}
