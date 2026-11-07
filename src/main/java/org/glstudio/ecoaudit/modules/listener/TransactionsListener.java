package org.glstudio.ecoaudit.modules.listener;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.glstudio.ecoaudit.GEcoAudit;
import org.glstudio.ecoaudit.modules.service.AuditService;
import org.glstudio.nexus.hooks.economy.VaultEconomy;
import org.glstudio.nexus.utils.LoggerUtils;

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Detects {@code /pay} and records it once the sender's balance shows the money left.
 *
 * <p>The economy plugin runs the command after this listener, so the balance is compared one tick
 * later; a payment that did not go through (insufficient funds, unknown player) leaves the balance
 * unchanged and is not recorded.
 */
public class TransactionsListener implements Listener {

    private final GEcoAudit plugin;
    private final AuditService auditService;

    /**
     * Creates the listener.
     *
     * @param plugin       the plugin
     * @param auditService where payments are recorded
     */
    public TransactionsListener(GEcoAudit plugin, AuditService auditService) {
        this.plugin = plugin;
        this.auditService = auditService;
    }

    /**
     * Parses {@code /pay <player> <amount>} (with {@code k}, {@code m} and {@code b} suffixes) and
     * schedules the balance check.
     *
     * @param event the command event
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        if (event.isCancelled()) return;

        String message = event.getMessage().trim();
        if (!message.toLowerCase().startsWith("/pay ")) return;

        Player sender = event.getPlayer();
        String[] parts = message.substring(5).trim().split("\\s+", 3);
        if (parts.length < 2) return;

        String targetName = parts[0];
        double amount;
        try {
            amount = parseShorthandAmount(parts[1]);
        } catch (NumberFormatException e) {
            LoggerUtils.logDebug("LISTENER", "Invalid amount format: " + parts[1]);
            return;
        }

        if (amount <= 0) return;

        UUID senderUUID = sender.getUniqueId();
        String senderName = sender.getName();
        double balanceBefore = VaultEconomy.get().balance(sender);

        Player onlineTarget = findPlayer(targetName);
        if (onlineTarget != null) {
            UUID targetUUID = onlineTarget.getUniqueId();
            String targetNameActual = onlineTarget.getName();
            UUID onlineTargetUUID = targetUUID;
            String onlineTargetName = targetNameActual;
            plugin.getTasks().runLater(1L, () -> {
                double balanceAfter = VaultEconomy.get().balance(sender);
                if (balanceBefore - balanceAfter >= amount - 0.01) {
                    LoggerUtils.logDebug("LISTENER", "Recording /pay: " + senderName + " -> " + onlineTargetName + " $" + amount);
                    auditService.auditTransaction(senderUUID, senderName, onlineTargetUUID, onlineTargetName, amount);
                } else {
                    LoggerUtils.logDebug("LISTENER", "Skipped /pay " + senderName + " -> " + onlineTargetName + " $" + amount + " (balance unchanged, likely insufficient funds)");
                }
            });
            return;
        }

        OfflinePlayer offlineTarget = Bukkit.getOfflinePlayer(targetName);
        if (offlineTarget.hasPlayedBefore() || offlineTarget.getName() != null) {
            UUID targetUUID = offlineTarget.getUniqueId();
            String targetNameActual = offlineTarget.getName() != null ? offlineTarget.getName() : targetName;
            UUID offlineTargetUUID = targetUUID;
            String offlineTargetName = targetNameActual;
            plugin.getTasks().runLater(1L, () -> {
                double balanceAfter = VaultEconomy.get().balance(sender);
                if (balanceBefore - balanceAfter >= amount - 0.01) {
                    LoggerUtils.logDebug("LISTENER", "Recording /pay to offline: " + senderName + " -> " + offlineTargetName + " $" + amount);
                    auditService.auditTransaction(senderUUID, senderName, offlineTargetUUID, offlineTargetName, amount);
                } else {
                    LoggerUtils.logDebug("LISTENER", "Skipped /pay " + senderName + " -> " + offlineTargetName + " $" + amount + " (balance unchanged, likely insufficient funds)");
                }
            });
            return;
        }

        plugin.getLogger().warning("Player not found for /pay: " + targetName + " (sender: " + senderName + ")");
    }

    private Player findPlayer(String name) {
        Player exact = Bukkit.getPlayerExact(name);
        if (exact != null) return exact;

        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.getName().equalsIgnoreCase(name)) {
                return online;
            }
        }
        return null;
    }

    private double parseShorthandAmount(String input) {
        if (input == null || input.isEmpty()) {
            throw new NumberFormatException("Empty amount");
        }

        Pattern pattern = Pattern.compile("^(-?\\d+(?:\\.\\d+)?)([kKmMbB]?)$");
        Matcher matcher = pattern.matcher(input.trim());
        if (!matcher.matches()) {
            throw new NumberFormatException("Invalid amount: " + input);
        }

        double value = Double.parseDouble(matcher.group(1));
        String suffix = matcher.group(2);

        switch (suffix.toLowerCase()) {
            case "k": return value * 1000;
            case "m": return value * 1000000;
            case "b": return value * 1000000000;
            default: return value;
        }
    }
}
