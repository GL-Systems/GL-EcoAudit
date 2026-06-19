package org.glstudio.economy.modules.services;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;
import org.glstudio.economy.GLEcoAudit;
import org.glstudio.economy.common.database.TransactionRepository;
import org.glstudio.economy.modules.manager.TransactionManager;
import org.glstudio.economy.modules.model.TransactionRecord;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

@Getter
public class TransactionService {

    private final GLEcoAudit plugin;
    private final TransactionRepository repository;
    private final TransactionManager manager;

    public TransactionService(GLEcoAudit plugin, TransactionRepository repository, TransactionManager manager) {
        this.plugin = plugin;
        this.repository = repository;
        this.manager = manager;
    }

    public void recordTransaction(UUID senderUUID, String senderName, UUID recipientUUID, String recipientName, double amount) {
        int maxPerPlayer = plugin.getConfigManager().getMainConfig().getInt("TRANSACTIONS.MAX_PER_PLAYER", 100);
        String behavior = plugin.getConfigManager().getMainConfig().getString("TRANSACTIONS.LIMIT_BEHAVIOR", "DELETE_OLDEST");

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

        TransactionRecord record = new TransactionRecord(
                UUID.randomUUID(),
                senderUUID,
                senderName,
                recipientUUID,
                recipientName,
                amount,
                System.currentTimeMillis(),
                getServerName()
        );

        try {
            repository.insert(record);
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to save transaction: " + e.getMessage());
            e.printStackTrace();
        }

        manager.addSentTransaction(senderUUID, record);
        manager.addReceivedTransaction(recipientUUID, record);
    }

    public CompletableFuture<Integer> getSentCountAsync(UUID playerUUID) {
        CompletableFuture<Integer> future = new CompletableFuture<>();
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    future.complete(repository.countSentTransactions(playerUUID));
                } catch (Exception e) {
                    future.complete(0);
                }
            }
        }.runTaskAsynchronously(plugin);
        return future;
    }

    public CompletableFuture<Integer> getReceivedCountAsync(UUID playerUUID) {
        CompletableFuture<Integer> future = new CompletableFuture<>();
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    future.complete(repository.countReceivedTransactions(playerUUID));
                } catch (Exception e) {
                    future.complete(0);
                }
            }
        }.runTaskAsynchronously(plugin);
        return future;
    }

    public void getSentTransactionsAsync(UUID playerUUID, int offset, int limit, Consumer<List<TransactionRecord>> callback) {
        new BukkitRunnable() {
            @Override
            public void run() {
                List<TransactionRecord> result = getSentTransactions(playerUUID, offset, limit);
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        callback.accept(result);
                    }
                }.runTask(plugin);
            }
        }.runTaskAsynchronously(plugin);
    }

    public void getReceivedTransactionsAsync(UUID playerUUID, int offset, int limit, Consumer<List<TransactionRecord>> callback) {
        new BukkitRunnable() {
            @Override
            public void run() {
                List<TransactionRecord> result = getReceivedTransactions(playerUUID, offset, limit);
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        callback.accept(result);
                    }
                }.runTask(plugin);
            }
        }.runTaskAsynchronously(plugin);
    }

    public List<TransactionRecord> getSentTransactions(UUID playerUUID, int offset, int limit) {
        try {
            return repository.getSentTransactions(playerUUID, limit, offset);
        } catch (Exception e) {
            return List.of();
        }
    }

    public List<TransactionRecord> getReceivedTransactions(UUID playerUUID, int offset, int limit) {
        try {
            return repository.getReceivedTransactions(playerUUID, limit, offset);
        } catch (Exception e) {
            return List.of();
        }
    }

    public int getSentCount(UUID playerUUID) {
        try {
            return repository.countSentTransactions(playerUUID);
        } catch (Exception e) {
            return 0;
        }
    }

    public int getReceivedCount(UUID playerUUID) {
        try {
            return repository.countReceivedTransactions(playerUUID);
        } catch (Exception e) {
            return 0;
        }
    }

    public void purgeOldRecords(long cutoffTimestamp) {
        try {
            repository.purgeOldRecords(cutoffTimestamp);
            plugin.getLogger().info("Purged records older than " + cutoffTimestamp);
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to purge old records: " + e.getMessage());
        }
    }

    private String getServerName() {
        String serverName = plugin.getConfigManager().getMainConfig().getString("SERVER_NAME");
        return serverName != null ? serverName : Bukkit.getServer().getName();
    }
}