package org.glstudio.ecoaudit.modules.manager;

import org.glstudio.ecoaudit.modules.model.TransactionRecord;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Recent records per player, kept in memory for the session.
 */
public class TransactionManager {

    private final Map<UUID, List<TransactionRecord>> sentCache = new ConcurrentHashMap<>();
    private final Map<UUID, List<TransactionRecord>> receivedCache = new ConcurrentHashMap<>();

    /**
     * Adds a payment a player made, newest first.
     *
     * @param playerUUID the payer
     * @param record     the record
     */
    public void addSentTransaction(UUID playerUUID, TransactionRecord record) {
        sentCache.computeIfAbsent(playerUUID, k -> new ArrayList<>()).add(0, record);
    }

    /**
     * Adds a payment a player received, newest first.
     *
     * @param playerUUID the payee
     * @param record     the record
     */
    public void addReceivedTransaction(UUID playerUUID, TransactionRecord record) {
        receivedCache.computeIfAbsent(playerUUID, k -> new ArrayList<>()).add(0, record);
    }

    /**
     * Returns the cached payments a player made.
     *
     * @param playerUUID the payer
     * @return the records, newest first
     */
    public List<TransactionRecord> getSentTransactions(UUID playerUUID) {
        return sentCache.getOrDefault(playerUUID, Collections.emptyList());
    }

    /**
     * Returns the cached payments a player received.
     *
     * @param playerUUID the payee
     * @return the records, newest first
     */
    public List<TransactionRecord> getReceivedTransactions(UUID playerUUID) {
        return receivedCache.getOrDefault(playerUUID, Collections.emptyList());
    }

    /**
     * Returns whether a player has cached payments made.
     *
     * @param playerUUID the payer
     * @return {@code true} if any
     */
    public boolean hasSentTransactions(UUID playerUUID) {
        List<TransactionRecord> list = sentCache.get(playerUUID);
        return list != null && !list.isEmpty();
    }

    /**
     * Returns whether a player has cached payments received.
     *
     * @param playerUUID the payee
     * @return {@code true} if any
     */
    public boolean hasReceivedTransactions(UUID playerUUID) {
        List<TransactionRecord> list = receivedCache.get(playerUUID);
        return list != null && !list.isEmpty();
    }

    /**
     * Forgets the cached payments a player made.
     *
     * @param playerUUID the payer
     */
    public void clearSent(UUID playerUUID) {
        sentCache.remove(playerUUID);
    }

    /**
     * Forgets the cached payments a player received.
     *
     * @param playerUUID the payee
     */
    public void clearReceived(UUID playerUUID) {
        receivedCache.remove(playerUUID);
    }

    /**
     * Forgets everything.
     */
    public void clearAll() {
        sentCache.clear();
        receivedCache.clear();
    }

    /**
     * Replaces the cached payments a player made.
     *
     * @param playerUUID   the payer
     * @param transactions the records, newest first
     */
    public void setSentTransactions(UUID playerUUID, List<TransactionRecord> transactions) {
        sentCache.put(playerUUID, new ArrayList<>(transactions));
    }

    /**
     * Replaces the cached payments a player received.
     *
     * @param playerUUID   the payee
     * @param transactions the records, newest first
     */
    public void setReceivedTransactions(UUID playerUUID, List<TransactionRecord> transactions) {
        receivedCache.put(playerUUID, new ArrayList<>(transactions));
    }
}
