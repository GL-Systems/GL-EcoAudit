package org.glstudio.ecoaudit.modules.service;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.glstudio.ecoaudit.GEcoAudit;
import org.glstudio.ecoaudit.modules.database.repository.TransactionRepository;
import org.glstudio.ecoaudit.modules.manager.TransactionManager;
import org.glstudio.ecoaudit.modules.model.TransactionRecord;
import org.glstudio.nexus.concurrent.Futures;
import org.glstudio.nexus.utils.Diagnostics;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Records payments and answers the queries of the menus and commands.
 *
 * <p>Writes go through a single ordered thread, so the limit check, the pruning and the insert of
 * one payment never interleave with another's. Reads run on the I/O pool and call back on the main
 * thread. A failed read answers {@code 0} or an empty list.
 */
@Getter
public class TransactionService {

    private final GEcoAudit plugin;
    private final TransactionRepository repository;
    private final TransactionManager manager;

    /**
     * Creates the service.
     *
     * @param plugin     the plugin
     * @param repository the storage
     * @param manager    the in-memory cache of recent records
     */
    public TransactionService(GEcoAudit plugin, TransactionRepository repository, TransactionManager manager) {
        this.plugin = plugin;
        this.repository = repository;
        this.manager = manager;
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
    public void recordTransaction(UUID senderUUID, String senderName, UUID recipientUUID, String recipientName, double amount) {
        recordTransaction(senderUUID, senderName, recipientUUID, recipientName, amount, "pay");
    }

    /**
     * Records a payment in the background, applying {@code TRANSACTIONS.MAX_PER_PLAYER}: past the
     * limit the oldest record is deleted, or with {@code STOP_RECORDING} the payment is skipped.
     *
     * @param senderUUID    who paid
     * @param senderName    the payer's name
     * @param recipientUUID who was paid
     * @param recipientName the payee's name
     * @param amount        the amount
     * @param category      the kind of payment
     */
    public void recordTransaction(UUID senderUUID, String senderName, UUID recipientUUID, String recipientName, double amount, String category) {
        int maxPerPlayer = plugin.getConfigManager().getMainConfig().getInt("TRANSACTIONS.MAX_PER_PLAYER", 100);
        String behavior = plugin.getConfigManager().getMainConfig().getString("TRANSACTIONS.LIMIT_BEHAVIOR", "DELETE_OLDEST");
        String configuredServer = plugin.getConfigManager().getMainConfig().getString("SERVER_NAME");
        String serverName = configuredServer != null ? configuredServer : Bukkit.getServer().getName();

        TransactionRecord record = new TransactionRecord(
                UUID.randomUUID(),
                senderUUID,
                senderName,
                recipientUUID,
                recipientName,
                amount,
                System.currentTimeMillis(),
                serverName,
                category
        );

        plugin.getExecutors().ordered("storage").execute(() -> {
            try {
                int sentCount = repository.countSentTransactions(senderUUID);
                int receivedCount = repository.countReceivedTransactions(recipientUUID);

                if (sentCount >= maxPerPlayer) {
                    if ("STOP_RECORDING".equalsIgnoreCase(behavior)) {
                        plugin.getLogger().warning("Sent limit reached for " + senderName + " (" + senderUUID + "). Transaction skipped.");
                        return;
                    }
                    repository.deleteOldestSent(senderUUID);
                }

                if (receivedCount >= maxPerPlayer) {
                    if ("STOP_RECORDING".equalsIgnoreCase(behavior)) {
                        plugin.getLogger().warning("Received limit reached for " + recipientName + " (" + recipientUUID + "). Transaction skipped.");
                        return;
                    }
                    repository.deleteOldestReceived(recipientUUID);
                }
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to enforce transaction limits: " + e.getMessage());
            }

            try {
                repository.insert(record);
            } catch (Exception e) {
                Diagnostics.logFailure("TransactionService#recordTransaction", e);
            }

            manager.addSentTransaction(senderUUID, record);
            manager.addReceivedTransaction(recipientUUID, record);
        });
    }

    /**
     * Sums the payments a player made. Blocking.
     *
     * @param playerUUID the payer
     * @return the total, {@code 0} on failure
     */
    public double getTotalSent(UUID playerUUID) {
        try {
            return repository.getTotalSent(playerUUID);
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Sums the payments a player received. Blocking.
     *
     * @param playerUUID the payee
     * @return the total, {@code 0} on failure
     */
    public double getTotalReceived(UUID playerUUID) {
        try {
            return repository.getTotalReceived(playerUUID);
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Sums the payments a player made and passes the total to {@code callback} on the main thread.
     *
     * @param playerUUID the payer
     * @param callback   receives the total
     */
    public void getTotalSentAsync(UUID playerUUID, Consumer<Double> callback) {
        query(() -> getTotalSent(playerUUID), callback, "getTotalSentAsync");
    }

    /**
     * Sums the payments a player received and passes the total to {@code callback} on the main
     * thread.
     *
     * @param playerUUID the payee
     * @param callback   receives the total
     */
    public void getTotalReceivedAsync(UUID playerUUID, Consumer<Double> callback) {
        query(() -> getTotalReceived(playerUUID), callback, "getTotalReceivedAsync");
    }

    /**
     * Counts the payments a player made.
     *
     * @param playerUUID the payer
     * @return the count, {@code 0} on failure
     */
    public CompletableFuture<Integer> getSentCountAsync(UUID playerUUID) {
        return CompletableFuture.supplyAsync(() -> getSentCount(playerUUID), plugin.getExecutors().io());
    }

    /**
     * Counts the payments a player received.
     *
     * @param playerUUID the payee
     * @return the count, {@code 0} on failure
     */
    public CompletableFuture<Integer> getReceivedCountAsync(UUID playerUUID) {
        return CompletableFuture.supplyAsync(() -> getReceivedCount(playerUUID), plugin.getExecutors().io());
    }

    /**
     * Loads a page of a player's payments and passes it to {@code callback} on the main thread.
     *
     * @param playerUUID the payer
     * @param offset     records to skip
     * @param limit      page size
     * @param callback   receives the page
     */
    public void getSentTransactionsAsync(UUID playerUUID, int offset, int limit, Consumer<List<TransactionRecord>> callback) {
        query(() -> getSentTransactions(playerUUID, offset, limit), callback, "getSentTransactionsAsync");
    }

    /**
     * Loads a page of the payments a player received and passes it to {@code callback} on the main
     * thread.
     *
     * @param playerUUID the payee
     * @param offset     records to skip
     * @param limit      page size
     * @param callback   receives the page
     */
    public void getReceivedTransactionsAsync(UUID playerUUID, int offset, int limit, Consumer<List<TransactionRecord>> callback) {
        query(() -> getReceivedTransactions(playerUUID, offset, limit), callback, "getReceivedTransactionsAsync");
    }

    /**
     * Returns a page of a player's payments. Blocking.
     *
     * @param playerUUID the payer
     * @param offset     records to skip
     * @param limit      page size
     * @return the page, empty on failure
     */
    public List<TransactionRecord> getSentTransactions(UUID playerUUID, int offset, int limit) {
        try {
            return repository.getSentTransactions(playerUUID, limit, offset);
        } catch (Exception e) {
            return List.of();
        }
    }

    /**
     * Returns a page of the payments a player received. Blocking.
     *
     * @param playerUUID the payee
     * @param offset     records to skip
     * @param limit      page size
     * @return the page, empty on failure
     */
    public List<TransactionRecord> getReceivedTransactions(UUID playerUUID, int offset, int limit) {
        try {
            return repository.getReceivedTransactions(playerUUID, limit, offset);
        } catch (Exception e) {
            return List.of();
        }
    }

    /**
     * Counts the payments a player made. Blocking.
     *
     * @param playerUUID the payer
     * @return the count, {@code 0} on failure
     */
    public int getSentCount(UUID playerUUID) {
        try {
            return repository.countSentTransactions(playerUUID);
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Counts the payments a player received. Blocking.
     *
     * @param playerUUID the payee
     * @return the count, {@code 0} on failure
     */
    public int getReceivedCount(UUID playerUUID) {
        try {
            return repository.countReceivedTransactions(playerUUID);
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Searches by payer or payee name and passes the page to {@code callback} on the main thread.
     *
     * @param query    part of a name
     * @param offset   records to skip
     * @param limit    page size
     * @param callback receives the page
     */
    public void searchTransactionsAsync(String query, int offset, int limit, Consumer<List<TransactionRecord>> callback) {
        query(() -> {
            try {
                return repository.searchTransactions(query, limit, offset);
            } catch (Exception e) {
                return List.<TransactionRecord>of();
            }
        }, callback, "searchTransactionsAsync");
    }

    /**
     * Ranks players by total paid and passes the rows to {@code callback} on the main thread.
     *
     * @param limit    rows to return
     * @param callback receives the rows
     */
    public void getTopSendersAsync(int limit, Consumer<List<TransactionRecord>> callback) {
        query(() -> {
            try {
                return repository.getTopSenders(limit);
            } catch (Exception e) {
                return List.<TransactionRecord>of();
            }
        }, callback, "getTopSendersAsync");
    }

    /**
     * Ranks players by total received and passes the rows to {@code callback} on the main thread.
     *
     * @param limit    rows to return
     * @param callback receives the rows
     */
    public void getTopReceiversAsync(int limit, Consumer<List<TransactionRecord>> callback) {
        query(() -> {
            try {
                return repository.getTopReceivers(limit);
            } catch (Exception e) {
                return List.<TransactionRecord>of();
            }
        }, callback, "getTopReceiversAsync");
    }

    /**
     * Deletes the records older than {@code cutoffTimestamp}. Blocking.
     *
     * @param cutoffTimestamp epoch milliseconds
     */
    public void purgeOldRecords(long cutoffTimestamp) {
        try {
            repository.purgeOldRecords(cutoffTimestamp);
            plugin.getLogger().info("Purged records older than " + cutoffTimestamp);
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to purge old records: " + e.getMessage());
        }
    }

    private <T> void query(Supplier<T> work, Consumer<T> callback, String context) {
        Futures.log(CompletableFuture.supplyAsync(work, plugin.getExecutors().io())
                .thenAcceptAsync(callback, plugin.getTasks().mainThread()), "TransactionService#" + context);
    }
}
