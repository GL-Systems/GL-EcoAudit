package org.glstudio.ecoaudit.modules.command.ecoaudit;

import org.bukkit.command.CommandSender;
import org.glstudio.ecoaudit.GEcoAudit;
import org.glstudio.nexus.modules.command.CommandGroup;
import org.glstudio.nexus.modules.command.CommandManager;

import java.util.List;

/**
 * {@code /ecoaudit <reload|purge [days]>}: administration. Without a known subcommand it shows the
 * help.
 */
public final class EcoAuditCommand extends CommandGroup<Void> {

    private final GEcoAudit plugin;

    /**
     * Creates the command and its subcommands.
     *
     * @param manager    the command manager
     * @param name       the command label
     * @param permission the permission every subcommand requires
     * @param plugin     the plugin
     */
    public EcoAuditCommand(CommandManager manager, String name, String permission, GEcoAudit plugin) {
        super(manager, name, permission);
        this.plugin = plugin;
        register(new ReloadSubCommand(plugin));
        register(new PurgeSubCommand(plugin));
    }

    @Override
    public List<String> aliases() {
        return List.of("ea");
    }

    @Override
    public List<String> usage() {
        return plugin.getMessages().rawList("COMMANDS.ECOAUDIT.USAGE");
    }

    @Override
    protected void onUnknown(CommandSender sender, Void context, String label) {
        sendUsage(sender);
    }
}
