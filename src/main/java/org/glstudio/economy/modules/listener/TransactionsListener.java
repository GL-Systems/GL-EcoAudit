package org.glstudio.economy.modules.listener;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.glstudio.economy.GLEcoAudit;
import org.glstudio.economy.modules.services.AuditService;
import org.glstudio.nexus.utils.LoggerUtils;

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TransactionsListener implements Listener {

    private final GLEcoAudit plugin;
    private final AuditService auditService;

    public TransactionsListener(GLEcoAudit plugin, AuditService auditService) {
        this.plugin = plugin;
        this.auditService = auditService;
    }

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
        double balanceBefore = plugin.getVaultService().getBalance(sender);

        Player onlineTarget = findPlayer(targetName);
        if (onlineTarget != null) {
            UUID targetUUID = onlineTarget.getUniqueId();
            String targetNameActual = onlineTarget.getName();
            UUID onlineTargetUUID = targetUUID;
            String onlineTargetName = targetNameActual;
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                double balanceAfter = plugin.getVaultService().getBalance(sender);
                if (balanceBefore - balanceAfter >= amount - 0.01) {
                    LoggerUtils.logDebug("LISTENER", "Recording /pay: " + senderName + " -> " + onlineTargetName + " $" + amount);
                    auditService.auditTransaction(senderUUID, senderName, onlineTargetUUID, onlineTargetName, amount);
                } else {
                    LoggerUtils.logDebug("LISTENER", "Skipped /pay " + senderName + " -> " + onlineTargetName + " $" + amount + " (balance unchanged, likely insufficient funds)");
                }
            }, 1L);
            return;
        }

        OfflinePlayer offlineTarget = Bukkit.getOfflinePlayer(targetName);
        if (offlineTarget.hasPlayedBefore() || offlineTarget.getName() != null) {
            UUID targetUUID = offlineTarget.getUniqueId();
            String targetNameActual = offlineTarget.getName() != null ? offlineTarget.getName() : targetName;
            UUID offlineTargetUUID = targetUUID;
            String offlineTargetName = targetNameActual;
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                double balanceAfter = plugin.getVaultService().getBalance(sender);
                if (balanceBefore - balanceAfter >= amount - 0.01) {
                    LoggerUtils.logDebug("LISTENER", "Recording /pay to offline: " + senderName + " -> " + offlineTargetName + " $" + amount);
                    auditService.auditTransaction(senderUUID, senderName, offlineTargetUUID, offlineTargetName, amount);
                } else {
                    LoggerUtils.logDebug("LISTENER", "Skipped /pay " + senderName + " -> " + offlineTargetName + " $" + amount + " (balance unchanged, likely insufficient funds)");
                }
            }, 1L);
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