package org.glstudio.ecoaudit.modules.database.repository;

import org.glstudio.ecoaudit.modules.database.impl.H2Provider;
import org.glstudio.ecoaudit.modules.model.TransactionRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SQLTransactionRepositoryTest {

    private final UUID alice = UUID.randomUUID();
    private final UUID bob = UUID.randomUUID();
    private H2Provider provider;
    private TransactionRepository repository;

    @BeforeEach
    void open() throws Exception {
        provider = new H2Provider("mem:ecoaudit-" + UUID.randomUUID());
        provider.initialize();
        repository = TransactionRepository.create(provider, "transactions");
        repository.createTable();
        // Creating the table again must not fail on an existing schema.
        repository.createTable();
    }

    @AfterEach
    void close() {
        provider.shutdown();
    }

    private TransactionRecord pay(UUID from, String fromName, UUID to, String toName, double amount, long at) {
        return new TransactionRecord(UUID.randomUUID(), from, fromName, to, toName, amount, at, "Survival", "pay");
    }

    @Test
    void recordsAreCountedPagedAndSummed() {
        repository.insert(pay(alice, "Alice", bob, "Bob", 10, 1));
        repository.insert(pay(alice, "Alice", bob, "Bob", 20, 2));
        repository.insert(pay(bob, "Bob", alice, "Alice", 5, 3));

        assertEquals(2, repository.countSentTransactions(alice));
        assertEquals(1, repository.countReceivedTransactions(alice));
        assertEquals(30.0, repository.getTotalSent(alice));

        List<TransactionRecord> page = repository.getSentTransactions(alice, 1, 0);
        assertEquals(1, page.size());
        assertEquals(20.0, page.get(0).amount());
        assertEquals("pay", page.get(0).category());
    }

    @Test
    void oldestRecordsArePrunedAndPurged() {
        repository.insert(pay(alice, "Alice", bob, "Bob", 10, 1));
        repository.insert(pay(alice, "Alice", bob, "Bob", 20, 2));

        repository.deleteOldestSent(alice);
        assertEquals(20.0, repository.getSentTransactions(alice, 10, 0).get(0).amount());

        repository.purgeOldRecords(3);
        assertEquals(0, repository.countSentTransactions(alice));
    }

    @Test
    void searchAndRankings() {
        repository.insert(pay(alice, "Alice", bob, "Bob", 10, 1));
        repository.insert(pay(bob, "Bob", alice, "Alice", 50, 2));

        assertEquals(2, repository.searchTransactions("Ali", 10, 0).size());

        List<TransactionRecord> top = repository.getTopSenders(10);
        assertEquals("Bob", top.get(0).senderName());
        assertEquals(50.0, top.get(0).amount());
        assertEquals(1L, top.get(0).timestamp());
        assertTrue(repository.getTopReceivers(10).stream().anyMatch(row -> row.senderName().equals("Alice")));
    }
}
