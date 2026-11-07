package org.glstudio.ecoaudit.modules.database.repository;

import org.glstudio.ecoaudit.modules.database.impl.MongoDBProvider;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Accumulators;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Sorts;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.glstudio.ecoaudit.modules.model.TransactionRecord;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static com.mongodb.client.model.Filters.*;

/**
 * MongoDB storage of transaction records.
 */
public class MongoTransactionRepository implements TransactionRepository {

    private static final String SENDER_UUID = "sender_uuid";
    private static final String RECIPIENT_UUID = "recipient_uuid";
    private static final String TIMESTAMP = "timestamp";
    private static final String ID = "id";

    private final MongoCollection<Document> collection;

    /**
     * Creates the repository.
     *
     * @param provider  the connected backend
     * @param tableName the collection name
     */
    public MongoTransactionRepository(MongoDBProvider provider, String tableName) {
        this.collection = provider.getCollection(tableName);
    }

    private Document toDocument(TransactionRecord r) {
        return new Document(ID, r.id().toString())
                .append(SENDER_UUID, r.senderUUID().toString())
                .append("sender_name", r.senderName())
                .append(RECIPIENT_UUID, r.recipientUUID().toString())
                .append("recipient_name", r.recipientName())
                .append("amount", r.amount())
                .append(TIMESTAMP, r.timestamp())
                .append("server", r.server())
                .append("category", r.category() != null ? r.category() : "pay");
    }

    private TransactionRecord fromDocument(Document doc) {
        String cat = doc.getString("category");
        if (cat == null) cat = "pay";
        return new TransactionRecord(
                UUID.fromString(doc.getString(ID)),
                UUID.fromString(doc.getString(SENDER_UUID)),
                doc.getString("sender_name"),
                UUID.fromString(doc.getString(RECIPIENT_UUID)),
                doc.getString("recipient_name"),
                doc.getDouble("amount"),
                doc.getLong(TIMESTAMP),
                doc.getString("server"),
                cat
        );
    }

    @Override
    public void createTable() {
        collection.createIndex(new Document(SENDER_UUID, 1));
        collection.createIndex(new Document(RECIPIENT_UUID, 1));
    }

    @Override
    public void insert(TransactionRecord record) {
        collection.insertOne(toDocument(record));
    }

    @Override
    public void deleteOldestSent(UUID senderUuid) {
        var doc = collection.find(eq(SENDER_UUID, senderUuid.toString()))
                .sort(Sorts.ascending(TIMESTAMP))
                .limit(1)
                .first();
        if (doc != null) {
            collection.deleteOne(eq(ID, doc.getString(ID)));
        }
    }

    @Override
    public void deleteOldestReceived(UUID recipientUuid) {
        var doc = collection.find(eq(RECIPIENT_UUID, recipientUuid.toString()))
                .sort(Sorts.ascending(TIMESTAMP))
                .limit(1)
                .first();
        if (doc != null) {
            collection.deleteOne(eq(ID, doc.getString(ID)));
        }
    }

    @Override
    public int countSentTransactions(UUID senderUuid) {
        return (int) collection.countDocuments(eq(SENDER_UUID, senderUuid.toString()));
    }

    @Override
    public int countReceivedTransactions(UUID recipientUuid) {
        return (int) collection.countDocuments(eq(RECIPIENT_UUID, recipientUuid.toString()));
    }

    @Override
    public List<TransactionRecord> getSentTransactions(UUID playerUuid, int limit, int offset) {
        return findList(eq(SENDER_UUID, playerUuid.toString()), limit, offset);
    }

    @Override
    public List<TransactionRecord> getReceivedTransactions(UUID playerUuid, int limit, int offset) {
        return findList(eq(RECIPIENT_UUID, playerUuid.toString()), limit, offset);
    }

    @Override
    public double getTotalSent(UUID playerUuid) {
        var result = collection.aggregate(List.of(
                Aggregates.match(eq(SENDER_UUID, playerUuid.toString())),
                Aggregates.group(null, Accumulators.sum("total", "$amount"))
        )).first();
        return result != null ? result.getDouble("total") : 0;
    }

    @Override
    public double getTotalReceived(UUID playerUuid) {
        var result = collection.aggregate(List.of(
                Aggregates.match(eq(RECIPIENT_UUID, playerUuid.toString())),
                Aggregates.group(null, Accumulators.sum("total", "$amount"))
        )).first();
        return result != null ? result.getDouble("total") : 0;
    }

    @Override
    public List<TransactionRecord> searchTransactions(String query, int limit, int offset) {
        String pattern = "(?i).*" + query.replaceAll("[.+^${}()|\\[\\]\\\\]", "\\$0") + ".*";
        Bson filter = or(
                regex("sender_name", pattern),
                regex("recipient_name", pattern)
        );
        return findList(filter, limit, offset);
    }

    @Override
    public List<TransactionRecord> getTopSenders(int limit) {
        return aggregateTop(SENDER_UUID, "sender_name", limit);
    }

    @Override
    public List<TransactionRecord> getTopReceivers(int limit) {
        return aggregateTop(RECIPIENT_UUID, "recipient_name", limit);
    }

    private List<TransactionRecord> aggregateTop(String groupField, String nameField, int limit) {
        List<TransactionRecord> result = new ArrayList<>();
        collection.aggregate(List.of(
                Aggregates.group("$" + groupField,
                        Accumulators.first("uuid", "$" + groupField),
                        Accumulators.first("name", "$" + nameField),
                        Accumulators.sum("total", "$amount"),
                        Accumulators.sum("count", 1)
                ),
                Aggregates.sort(Sorts.descending("total")),
                Aggregates.limit(limit)
        )).forEach(doc -> {
            result.add(new TransactionRecord(
                    null,
                    UUID.fromString(doc.getString("uuid")),
                    doc.getString("name"),
                    null,
                    null,
                    doc.getDouble("total"),
                    doc.getInteger("count", 0),
                    null,
                    null
            ));
        });
        return result;
    }

    private List<TransactionRecord> findList(Bson filter, int limit, int offset) {
        return collection.find(filter)
                .sort(Sorts.descending(TIMESTAMP))
                .skip(offset)
                .limit(limit)
                .into(new ArrayList<>())
                .stream()
                .map(this::fromDocument)
                .toList();
    }

    @Override
    public void purgeOldRecords(long cutoffTimestamp) {
        collection.deleteMany(lt(TIMESTAMP, cutoffTimestamp));
    }

    @Override
    public CompletableFuture<Integer> getSentCountAsync(UUID playerUuid) {
        return CompletableFuture.supplyAsync(() -> countSentTransactions(playerUuid));
    }

    @Override
    public CompletableFuture<Integer> getReceivedCountAsync(UUID playerUuid) {
        return CompletableFuture.supplyAsync(() -> countReceivedTransactions(playerUuid));
    }
}
