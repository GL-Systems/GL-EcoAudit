package org.glstudio.economy.common.database;

import org.glstudio.economy.modules.model.TransactionRecord;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface TransactionRepository {

    void createTable();

    void insert(TransactionRecord record);

    void deleteOldestSent(UUID senderUuid);

    void deleteOldestReceived(UUID recipientUuid);

    int countSentTransactions(UUID senderUuid);

    int countReceivedTransactions(UUID recipientUuid);

    List<TransactionRecord> getSentTransactions(UUID playerUuid, int limit, int offset);

    List<TransactionRecord> getReceivedTransactions(UUID playerUuid, int limit, int offset);

    void purgeOldRecords(long cutoffTimestamp);

    CompletableFuture<Integer> getSentCountAsync(UUID playerUuid);

    CompletableFuture<Integer> getReceivedCountAsync(UUID playerUuid);

    static TransactionRepository create(DatabaseProvider provider, String tableName) {
        if (provider instanceof MongoDBProvider mongo) {
            return new MongoTransactionRepository(mongo, tableName);
        }
        return new SQLTransactionRepository((SQLDatabaseProvider) provider, tableName);
    }
}