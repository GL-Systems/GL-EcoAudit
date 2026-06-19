package org.glstudio.economy.modules.model;

import java.util.UUID;

public class TransactionRecord {

    private final UUID id;
    private final UUID senderUUID;
    private final String senderName;
    private final UUID recipientUUID;
    private final String recipientName;
    private final double amount;
    private final long timestamp;
    private final String server;

    public TransactionRecord(UUID id, UUID senderUUID, String senderName, UUID recipientUUID, String recipientName, double amount, long timestamp, String server) {
        this.id = id;
        this.senderUUID = senderUUID;
        this.senderName = senderName;
        this.recipientUUID = recipientUUID;
        this.recipientName = recipientName;
        this.amount = amount;
        this.timestamp = timestamp;
        this.server = server;
    }

    public UUID getId() {
        return id;
    }

    public UUID getSenderUUID() {
        return senderUUID;
    }

    public String getSenderName() {
        return senderName;
    }

    public UUID getRecipientUUID() {
        return recipientUUID;
    }

    public String getRecipientName() {
        return recipientName;
    }

    public double getAmount() {
        return amount;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getServer() {
        return server;
    }
}