package org.glstudio.ecoaudit.modules.command.ecoaudit;

import org.bukkit.command.CommandSender;
import org.glstudio.ecoaudit.GEcoAudit;
import org.glstudio.nexus.concurrent.Futures;
import org.glstudio.nexus.modules.command.SubCommand;
import org.glstudio.nexus.modules.lang.LanguageFile;

import java.util.concurrent.CompletableFuture;

/**
 * {@code /ecoaudit purge [days]}: deletes the records older than {@code days} (90 by default).
 */
final class PurgeSubCommand extends SubCommand<Void> {

    private final GEcoAudit plugin;

    PurgeSubCommand(GEcoAudit plugin) {
        super("purge", "");
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender sender, Void context, String[] args) {
        LanguageFile messages = plugin.getMessages();
        int days = 90;
        if (args.length >= 1) {
            try {
                days = Integer.parseInt(args[0]);
                if (days < 1) {
                    messages.send(sender, "COMMANDS.ECOAUDIT.PURGE_INVALID_DAYS");
                    return;
                }
            } catch (NumberFormatException e) {
                messages.send(sender, "COMMANDS.ECOAUDIT.PURGE_INVALID_NUMBER", "{value}", args[0]);
                return;
            }
        }

        long cutoff = System.currentTimeMillis() - (days * 86400000L);
        int purgedDays = days;
        Futures.log(CompletableFuture
                .runAsync(() -> plugin.getTransactionService().purgeOldRecords(cutoff), plugin.getExecutors().ordered("storage"))
                .thenRunAsync(() -> messages.send(sender, "COMMANDS.ECOAUDIT.PURGE_SUCCESS", "{days}", purgedDays),
                        plugin.getTasks().mainThread()), "PurgeSubCommand#execute");
    }
}
