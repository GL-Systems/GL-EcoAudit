package org.glstudio.economy.common.database;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Sorts;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.glstudio.economy.modules.model.TransactionRecord;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static com.mongodb.client.model.Filters.*;

public class MongoTransactionRepository implements TransactionRepository {

    private static final String SENDER_UUID = "sender_uuid";
    private static final String RECIPIENT_UUID = "recipient_uuid";
    private static final String TIMESTAMP = "timestamp";
    private static final String ID = "id";

    private final MongoCollection<Document> collection;

    public MongoTransactionRepository(MongoDBProvider provider, String tableName) {
        this.collection = provider.getCollection(tableName);
    }

    private Document toDocument(TransactionRecord r) {
        return new Document(ID, r.getId().toString())
                .append(SENDER_UUID, r.getSenderUUID().toString())
                .append("sender_name", r.getSenderName())
                .append(RECIPIENT_UUID, r.getRecipientUUID().toString())
                .append("recipient_name", r.getRecipientName())
                .append("amount", r.getAmount())
                .append(TIMESTAMP, r.getTimestamp())
                .append("server", r.getServer());
    }

    private TransactionRecord fromDocument(Document doc) {
        return new TransactionRecord(
                UUID.fromString(doc.getString(ID)),
                UUID.fromString(doc.getString(SENDER_UUID)),
                doc.getString("sender_name"),
                UUID.fromString(doc.getString(RECIPIENT_UUID)),
                doc.getString("recipient_name"),
                doc.getDouble("amount"),
                doc.getLong(TIMESTAMP),
                doc.getString("server")
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