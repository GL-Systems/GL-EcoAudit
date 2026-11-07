package org.glstudio.ecoaudit.modules.database.repository;

import org.glstudio.ecoaudit.modules.database.DatabaseProvider;
import org.glstudio.ecoaudit.modules.database.impl.MongoDBProvider;
import org.glstudio.ecoaudit.modules.database.impl.SQLDatabaseProvider;

import org.glstudio.ecoaudit.modules.model.TransactionRecord;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Storage of transaction records. Every method is blocking: call them off the main thread.
 */
public interface TransactionRepository {

    /**
     * Creates the table or the indexes if missing.
     */
    void createTable();

    /**
     * Stores a record.
     *
     * @param record the record
     */
    void insert(TransactionRecord record);

    /**
     * Deletes the oldest payment a player made.
     *
     * @param senderUuid the payer
     */
    void deleteOldestSent(UUID senderUuid);

    /**
     * Deletes the oldest payment a player received.
     *
     * @param recipientUuid the payee
     */
    void deleteOldestReceived(UUID recipientUuid);

    /**
     * Counts the payments a player made.
     *
     * @param senderUuid the payer
     * @return the count
     */
    int countSentTransactions(UUID senderUuid);

    /**
     * Counts the payments a player received.
     *
     * @param recipientUuid the payee
     * @return the count
     */
    int countReceivedTransactions(UUID recipientUuid);

    /**
     * Returns a page of a player's payments, newest first.
     *
     * @param playerUuid the payer
     * @param limit      page size
     * @param offset     records to skip
     * @return the page
     */
    List<TransactionRecord> getSentTransactions(UUID playerUuid, int limit, int offset);

    /**
     * Returns a page of the payments a player received, newest first.
     *
     * @param playerUuid the payee
     * @param limit      page size
     * @param offset     records to skip
     * @return the page
     */
    List<TransactionRecord> getReceivedTransactions(UUID playerUuid, int limit, int offset);

    /**
     * Sums the payments a player made.
     *
     * @param playerUuid the payer
     * @return the total
     */
    default double getTotalSent(UUID playerUuid) { return 0; }

    /**
     * Sums the payments a player received.
     *
     * @param playerUuid the payee
     * @return the total
     */
    default double getTotalReceived(UUID playerUuid) { return 0; }

    /**
     * Returns a page of the records whose payer or payee name contains {@code query}, newest first.
     *
     * @param query  part of a name
     * @param limit  page size
     * @param offset records to skip
     * @return the page
     */
    default List<TransactionRecord> searchTransactions(String query, int limit, int offset) {
        return List.of();
    }

    /**
     * Ranks players by total paid.
     *
     * @param limit rows to return
     * @return ranking rows (see {@link TransactionRecord})
     */
    default List<TransactionRecord> getTopSenders(int limit) { return List.of(); }

    /**
     * Ranks players by total received.
     *
     * @param limit rows to return
     * @return ranking rows (see {@link TransactionRecord})
     */
    default List<TransactionRecord> getTopReceivers(int limit) { return List.of(); }

    /**
     * Deletes the records older than {@code cutoffTimestamp}.
     *
     * @param cutoffTimestamp epoch milliseconds
     */
    void purgeOldRecords(long cutoffTimestamp);

    /**
     * Counts the payments a player made, asynchronously.
     *
     * @param playerUuid the payer
     * @return the count
     */
    CompletableFuture<Integer> getSentCountAsync(UUID playerUuid);

    /**
     * Counts the payments a player received, asynchronously.
     *
     * @param playerUuid the payee
     * @return the count
     */
    CompletableFuture<Integer> getReceivedCountAsync(UUID playerUuid);

    /**
     * Creates the repository for a backend.
     *
     * @param provider  the connected backend
     * @param tableName the table or collection name
     * @return the repository
     */
    static TransactionRepository create(DatabaseProvider provider, String tableName) {
        if (provider instanceof MongoDBProvider mongo) {
            return new MongoTransactionRepository(mongo, tableName);
        }
        return new SQLTransactionRepository((SQLDatabaseProvider) provider, tableName);
    }
}
