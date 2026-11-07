package org.glstudio.ecoaudit.modules.command.ecoaudit;

import org.bukkit.command.CommandSender;
import org.glstudio.ecoaudit.GEcoAudit;
import org.glstudio.ecoaudit.core.ReloadService;
import org.glstudio.nexus.modules.command.SubCommand;

/**
 * {@code /ecoaudit reload}: re-reads the configuration, language and menu files. The database
 * connection is not restarted.
 */
final class ReloadSubCommand extends SubCommand<Void> {

    private final GEcoAudit plugin;

    ReloadSubCommand(GEcoAudit plugin) {
        super("reload", "");
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender sender, Void context, String[] args) {
        String error = new ReloadService(plugin).reload();
        if (error != null) {
            plugin.getMessages().send(sender, "COMMANDS.ECOAUDIT.RELOAD_ERROR", "{error}", error);
            return;
        }
        plugin.getMessages().send(sender, "COMMANDS.TRANSACTIONS.RELOAD_SUCCESS");
    }
}
