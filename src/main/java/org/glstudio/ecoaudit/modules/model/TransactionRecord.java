package org.glstudio.ecoaudit.modules.model;

import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * A recorded payment, or a row of a ranking.
 *
 * <p>Ranking rows reuse the type: {@code senderUUID}/{@code senderName} hold the ranked player,
 * {@code amount} the total and {@code timestamp} the number of payments; the other components are
 * {@code null}.
 *
 * @param id            the record id
 * @param senderUUID    who paid
 * @param senderName    the payer's name at the time
 * @param recipientUUID who was paid
 * @param recipientName the payee's name at the time
 * @param amount        the amount
 * @param timestamp     when, in epoch milliseconds
 * @param server        the server it happened on
 * @param category      the kind of payment, {@code pay} for {@code /pay}
 */
public record TransactionRecord(
        @Nullable UUID id,
        UUID senderUUID,
        String senderName,
        @Nullable UUID recipientUUID,
        @Nullable String recipientName,
        double amount,
        long timestamp,
        @Nullable String server,
        @Nullable String category
) {
}
