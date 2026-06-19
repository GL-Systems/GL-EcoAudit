package org.glstudio.economy.modules.command;

import org.bukkit.command.CommandSender;
import org.glstudio.economy.GLEcoAudit;
import org.glstudio.nexus.modules.command.Command;
import org.glstudio.nexus.modules.command.CommandManager;
import org.glstudio.nexus.utils.CC;
import org.glstudio.nexus.utils.ConfigFile;

import java.util.List;

public class EcoAuditCommand extends Command {

    private final GLEcoAudit plugin;

    public EcoAuditCommand(CommandManager manager, String name, String permission, GLEcoAudit plugin) {
        super(manager, name, permission);
        this.plugin = plugin;
    }

    @Override
    public List<String> aliases() {
        return List.of("ea");
    }

    @Override
    public List<String> usage() {
        return plugin.getConfigManager().getLangConfig().getStringList("COMMANDS.ECOAUDIT.USAGE");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!sender.hasPermission(getPermission())) {
            sendMessage(sender, plugin.getConfigManager().getLangConfig().getString("GLOBAL.NO_PERMISSION"));
            return;
        }

        if (args.length == 0) {
            usage().forEach(line -> sendMessage(sender, line));
            return;
        }

        switch (args[0].toLowerCase()) {
            case "reload":
                handleReload(sender);
                break;
            case "purge":
                handlePurge(sender, args);
                break;
            default:
                usage().forEach(line -> sendMessage(sender, line));
                break;
        }
    }

    private void handleReload(CommandSender sender) {
        ConfigFile lang = plugin.getConfigManager().getLangConfig();
        try {
            plugin.getConfigManager().reloadConfigs();
            sendMessage(sender, CC.t(lang.getString("COMMANDS.TRANSACTIONS.RELOAD_SUCCESS")));
        } catch (Exception e) {
            sendMessage(sender, CC.t(lang.getString("COMMANDS.ECOAUDIT.RELOAD_ERROR")
                    .replace("{error}", e.getMessage())));
        }
    }

    private void handlePurge(CommandSender sender, String[] args) {
        ConfigFile lang = plugin.getConfigManager().getLangConfig();
        int days = 90;
        if (args.length >= 2) {
            try {
                days = Integer.parseInt(args[1]);
                if (days < 1) {
                    sendMessage(sender, CC.t(lang.getString("COMMANDS.ECOAUDIT.PURGE_INVALID_DAYS")));
                    return;
                }
            } catch (NumberFormatException e) {
                sendMessage(sender, CC.t(lang.getString("COMMANDS.ECOAUDIT.PURGE_INVALID_NUMBER")
                        .replace("{value}", args[1])));
                return;
            }
        }

        long cutoff = System.currentTimeMillis() - (days * 86400000L);
        plugin.getTransactionService().purgeOldRecords(cutoff);
        sendMessage(sender, CC.t(lang.getString("COMMANDS.ECOAUDIT.PURGE_SUCCESS")
                .replace("{days}", String.valueOf(days))));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return List.of("reload", "purge");
        }
        return List.of();
    }
}