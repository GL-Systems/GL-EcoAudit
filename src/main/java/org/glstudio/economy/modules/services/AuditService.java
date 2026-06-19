package org.glstudio.economy.modules.services;

import lombok.Getter;
import org.glstudio.economy.GLEcoAudit;

import java.util.UUID;

@Getter
public class AuditService {

    private final GLEcoAudit plugin;
    private final TransactionService transactionService;

    public AuditService(GLEcoAudit plugin, TransactionService transactionService) {
        this.plugin = plugin;
        this.transactionService = transactionService;
    }

    public void auditTransaction(UUID senderUUID, String senderName,
                                 UUID recipientUUID, String recipientName,
                                 double amount) {
        transactionService.recordTransaction(senderUUID, senderName, recipientUUID, recipientName, amount);
    }
}