package org.glstudio.economy.modules.manager;

import org.glstudio.economy.modules.model.TransactionRecord;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class TransactionManager {

    private final Map<UUID, List<TransactionRecord>> sentCache = new ConcurrentHashMap<>();
    private final Map<UUID, List<TransactionRecord>> receivedCache = new ConcurrentHashMap<>();

    public void addSentTransaction(UUID playerUUID, TransactionRecord record) {
        sentCache.computeIfAbsent(playerUUID, k -> new ArrayList<>()).add(0, record);
    }

    public void addReceivedTransaction(UUID playerUUID, TransactionRecord record) {
        receivedCache.computeIfAbsent(playerUUID, k -> new ArrayList<>()).add(0, record);
    }

    public List<TransactionRecord> getSentTransactions(UUID playerUUID) {
        return sentCache.getOrDefault(playerUUID, Collections.emptyList());
    }

    public List<TransactionRecord> getReceivedTransactions(UUID playerUUID) {
        return receivedCache.getOrDefault(playerUUID, Collections.emptyList());
    }

    public boolean hasSentTransactions(UUID playerUUID) {
        List<TransactionRecord> list = sentCache.get(playerUUID);
        return list != null && !list.isEmpty();
    }

    public boolean hasReceivedTransactions(UUID playerUUID) {
        List<TransactionRecord> list = receivedCache.get(playerUUID);
        return list != null && !list.isEmpty();
    }

    public void clearSent(UUID playerUUID) {
        sentCache.remove(playerUUID);
    }

    public void clearReceived(UUID playerUUID) {
        receivedCache.remove(playerUUID);
    }

    public void clearAll() {
        sentCache.clear();
        receivedCache.clear();
    }

    public void setSentTransactions(UUID playerUUID, List<TransactionRecord> transactions) {
        sentCache.put(playerUUID, new ArrayList<>(transactions));
    }

    public void setReceivedTransactions(UUID playerUUID, List<TransactionRecord> transactions) {
        receivedCache.put(playerUUID, new ArrayList<>(transactions));
    }
}