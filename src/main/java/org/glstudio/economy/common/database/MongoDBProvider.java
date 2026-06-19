package org.glstudio.economy.common.database;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import lombok.Getter;
import org.bson.Document;

@Getter
public class MongoDBProvider implements DatabaseProvider {

    private MongoClient client;
    private MongoDatabase database;
    private final String connectionString;
    private final String databaseName;

    public MongoDBProvider(String connectionString, String databaseName) {
        this.connectionString = connectionString;
        this.databaseName = databaseName;
    }

    @Override
    public void initialize() throws Exception {
        MongoClientSettings settings = MongoClientSettings.builder()
                .applyConnectionString(new ConnectionString(connectionString))
                .build();
        this.client = MongoClients.create(settings);
        this.database = client.getDatabase(databaseName);
        database.runCommand(new Document("ping", 1));
    }

    @Override
    public void shutdown() {
        if (client != null) {
            client.close();
        }
    }

    @Override
    public boolean isInitialized() {
        return client != null;
    }

    public MongoCollection<Document> getCollection(String name) {
        return database.getCollection(name);
    }
}