package org.glstudio.ecoaudit.modules.service;

import lombok.Getter;
import org.glstudio.ecoaudit.GEcoAudit;

import java.util.UUID;

/**
 * Entry point for recording a detected payment.
 */
@Getter
public class AuditService {

    private final GEcoAudit plugin;
    private final TransactionService transactionService;

    /**
     * Creates the service.
     *
     * @param plugin             the plugin
     * @param transactionService where payments are recorded
     */
    public AuditService(GEcoAudit plugin, TransactionService transactionService) {
        this.plugin = plugin;
        this.transactionService = transactionService;
    }

    /**
     * Records a {@code /pay} payment.
     *
     * @param senderUUID    who paid
     * @param senderName    the payer's name
     * @param recipientUUID who was paid
     * @param recipientName the payee's name
     * @param amount        the amount
     */
    public void auditTransaction(UUID senderUUID, String senderName,
                                 UUID recipientUUID, String recipientName,
                                 double amount) {
        transactionService.recordTransaction(senderUUID, senderName, recipientUUID, recipientName, amount, "pay");
    }

    /**
     * Records a payment of the given kind.
     *
     * @param senderUUID    who paid
     * @param senderName    the payer's name
     * @param recipientUUID who was paid
     * @param recipientName the payee's name
     * @param amount        the amount
     * @param category      the kind of payment
     */
    public void auditTransaction(UUID senderUUID, String senderName,
                                 UUID recipientUUID, String recipientName,
                                 double amount, String category) {
        transactionService.recordTransaction(senderUUID, senderName, recipientUUID, recipientName, amount, category);
    }
}
