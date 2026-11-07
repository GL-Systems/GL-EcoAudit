package org.glstudio.ecoaudit.modules.database.repository;

import org.glstudio.ecoaudit.modules.database.impl.SQLDatabaseProvider;

import org.glstudio.ecoaudit.modules.model.TransactionRecord;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * SQL storage of transaction records.
 */
public class SQLTransactionRepository implements TransactionRepository {

    private static final String SENDER_UUID = "sender_uuid";
    private static final String RECIPIENT_UUID = "recipient_uuid";

    private final SQLDatabaseProvider db;
    private final String table;

    /**
     * Creates the repository.
     *
     * @param db    the connected backend
     * @param table the table name
     */
    public SQLTransactionRepository(SQLDatabaseProvider db, String table) {
        this.db = db;
        this.table = table;
    }

    @Override
    public void createTable() {
        try (Connection conn = db.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(String.format("""
                    CREATE TABLE IF NOT EXISTS %s (
                        id VARCHAR(36) PRIMARY KEY,
                        sender_uuid VARCHAR(36) NOT NULL,
                        sender_name VARCHAR(16) NOT NULL,
                        recipient_uuid VARCHAR(36) NOT NULL,
                        recipient_name VARCHAR(16) NOT NULL,
                        amount %s NOT NULL,
                        timestamp BIGINT NOT NULL,
                        server VARCHAR(64) NOT NULL DEFAULT '',
                        category VARCHAR(32) NOT NULL DEFAULT 'pay'
                    )
                    """, table, db.doubleType()));

            // Tables created before the category column existed get it here; on newer tables the
            // statement fails because the column is already there, which is expected.
            try {
                stmt.executeUpdate("ALTER TABLE " + table + " ADD COLUMN category VARCHAR(32) NOT NULL DEFAULT 'pay'");
            } catch (Exception ignored) {
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to create table " + table, e);
        }
    }

    @Override
    public void insert(TransactionRecord record) {
        String sql = String.format(
                "INSERT INTO %s (id, sender_uuid, sender_name, recipient_uuid, recipient_name, amount, timestamp, server, category) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                table);
        try {
            db.executeUpdate(sql,
                    record.id().toString(),
                    record.senderUUID().toString(),
                    record.senderName(),
                    record.recipientUUID().toString(),
                    record.recipientName(),
                    record.amount(),
                    record.timestamp(),
                    record.server(),
                    record.category());
        } catch (Exception e) {
            throw new RuntimeException("Failed to insert transaction", e);
        }
    }

    @Override
    public void deleteOldestSent(UUID senderUuid) {
        String sql = String.format(
                "DELETE FROM %s WHERE id = (SELECT id FROM %s WHERE sender_uuid = ? ORDER BY timestamp ASC LIMIT 1)",
                table, table);
        try {
            db.executeUpdate(sql, senderUuid.toString());
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete oldest sent transaction", e);
        }
    }

    @Override
    public void deleteOldestReceived(UUID recipientUuid) {
        String sql = String.format(
                "DELETE FROM %s WHERE id = (SELECT id FROM %s WHERE recipient_uuid = ? ORDER BY timestamp ASC LIMIT 1)",
                table, table);
        try {
            db.executeUpdate(sql, recipientUuid.toString());
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete oldest received transaction", e);
        }
    }

    @Override
    public int countSentTransactions(UUID senderUuid) {
        String sql = "SELECT COUNT(*) FROM " + table + " WHERE sender_uuid = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, senderUuid.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to count sent transactions", e);
        }
    }

    @Override
    public int countReceivedTransactions(UUID recipientUuid) {
        String sql = "SELECT COUNT(*) FROM " + table + " WHERE recipient_uuid = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, recipientUuid.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to count received transactions", e);
        }
    }

    @Override
    public List<TransactionRecord> getSentTransactions(UUID playerUuid, int limit, int offset) {
        String sql = "SELECT * FROM " + table + " WHERE sender_uuid = ? ORDER BY timestamp DESC LIMIT ? OFFSET ?";
        return queryList(sql, playerUuid, limit, offset);
    }

    @Override
    public List<TransactionRecord> getReceivedTransactions(UUID playerUuid, int limit, int offset) {
        String sql = "SELECT * FROM " + table + " WHERE recipient_uuid = ? ORDER BY timestamp DESC LIMIT ? OFFSET ?";
        return queryList(sql, playerUuid, limit, offset);
    }

    @Override
    public double getTotalSent(UUID playerUuid) {
        String sql = "SELECT COALESCE(SUM(amount), 0) FROM " + table + " WHERE sender_uuid = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, playerUuid.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getDouble(1) : 0;
            }
        } catch (Exception e) {
            return 0;
        }
    }

    @Override
    public double getTotalReceived(UUID playerUuid) {
        String sql = "SELECT COALESCE(SUM(amount), 0) FROM " + table + " WHERE recipient_uuid = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, playerUuid.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getDouble(1) : 0;
            }
        } catch (Exception e) {
            return 0;
        }
    }

    @Override
    public List<TransactionRecord> searchTransactions(String query, int limit, int offset) {
        String pattern = "%" + query + "%";
        String sql = "SELECT * FROM " + table + " WHERE sender_name LIKE ? OR recipient_name LIKE ? ORDER BY timestamp DESC LIMIT ? OFFSET ?";
        List<TransactionRecord> result = new ArrayList<>();
        try (Connection conn = db.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, pattern);
            stmt.setString(2, pattern);
            stmt.setInt(3, limit);
            stmt.setInt(4, offset);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    result.add(mapRecord(rs));
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to search transactions", e);
        }
        return result;
    }

    @Override
    public List<TransactionRecord> getTopSenders(int limit) {
        String sql = "SELECT sender_uuid, sender_name, SUM(amount) as total_amount, COUNT(*) as tx_count, MAX(timestamp) as last_tx " +
                "FROM " + table + " GROUP BY sender_uuid, sender_name ORDER BY total_amount DESC LIMIT ?";
        return queryTop(sql, limit);
    }

    @Override
    public List<TransactionRecord> getTopReceivers(int limit) {
        String sql = "SELECT recipient_uuid as sender_uuid, recipient_name as sender_name, SUM(amount) as total_amount, COUNT(*) as tx_count, MAX(timestamp) as last_tx " +
                "FROM " + table + " GROUP BY recipient_uuid, recipient_name ORDER BY total_amount DESC LIMIT ?";
        return queryTop(sql, limit);
    }

    private List<TransactionRecord> queryTop(String sql, int limit) {
        List<TransactionRecord> result = new ArrayList<>();
        try (Connection conn = db.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, limit);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    result.add(new TransactionRecord(
                            null,
                            UUID.fromString(rs.getString("sender_uuid")),
                            rs.getString("sender_name"),
                            null,
                            null,
                            rs.getDouble("total_amount"),
                            rs.getLong("tx_count"),
                            null,
                            null
                    ));
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to query top rankings", e);
        }
        return result;
    }

    private List<TransactionRecord> queryList(String sql, UUID uuid, int limit, int offset) {
        List<TransactionRecord> result = new ArrayList<>();
        try (Connection conn = db.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, uuid.toString());
            stmt.setInt(2, limit);
            stmt.setInt(3, offset);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    result.add(mapRecord(rs));
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to query transactions", e);
        }
        return result;
    }

    @Override
    public void purgeOldRecords(long cutoffTimestamp) {
        String sql = "DELETE FROM " + table + " WHERE timestamp < ?";
        try {
            db.executeUpdate(sql, cutoffTimestamp);
        } catch (Exception e) {
            throw new RuntimeException("Failed to purge old records", e);
        }
    }

    @Override
    public CompletableFuture<Integer> getSentCountAsync(UUID playerUuid) {
        return CompletableFuture.supplyAsync(() -> countSentTransactions(playerUuid));
    }

    @Override
    public CompletableFuture<Integer> getReceivedCountAsync(UUID playerUuid) {
        return CompletableFuture.supplyAsync(() -> countReceivedTransactions(playerUuid));
    }

    private TransactionRecord mapRecord(ResultSet rs) throws SQLException {
        String cat;
        try {
            cat = rs.getString("category");
            if (cat == null) cat = "pay";
        } catch (Exception e) {
            cat = "pay";
        }
        return new TransactionRecord(
                UUID.fromString(rs.getString("id")),
                UUID.fromString(rs.getString(SENDER_UUID)),
                rs.getString("sender_name"),
                UUID.fromString(rs.getString(RECIPIENT_UUID)),
                rs.getString("recipient_name"),
                rs.getDouble("amount"),
                rs.getLong("timestamp"),
                rs.getString("server"),
                cat
        );
    }
}
