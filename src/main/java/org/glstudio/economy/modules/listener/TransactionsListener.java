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
            amount = Double.parseDouble(parts[1]);
        } catch (NumberFormatException e) {
            return;
        }

        if (amount <= 0) return;

        UUID senderUUID = sender.getUniqueId();
        String senderName = sender.getName();

        Player onlineTarget = findPlayer(targetName);
        if (onlineTarget != null) {
            UUID targetUUID = onlineTarget.getUniqueId();
            String targetNameActual = onlineTarget.getName();
            LoggerUtils.logDebug("LISTENER", "Recording /pay: " + senderName + " -> " + targetNameActual + " $" + amount);
            Bukkit.getScheduler().runTaskLater(plugin, () ->
                    auditService.auditTransaction(senderUUID, senderName, targetUUID, targetNameActual, amount), 1L);
            return;
        }

        OfflinePlayer offlineTarget = Bukkit.getOfflinePlayer(targetName);
        if (offlineTarget.hasPlayedBefore() || offlineTarget.getName() != null) {
            UUID targetUUID = offlineTarget.getUniqueId();
            String targetNameActual = offlineTarget.getName() != null ? offlineTarget.getName() : targetName;
            LoggerUtils.logDebug("LISTENER", "Recording /pay to offline: " + senderName + " -> " + targetNameActual + " $" + amount);
            Bukkit.getScheduler().runTaskLater(plugin, () ->
                    auditService.auditTransaction(senderUUID, senderName, targetUUID, targetNameActual, amount), 1L);
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
}