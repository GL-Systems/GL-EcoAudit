package org.glstudio.economy.modules.command;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.glstudio.economy.GLEcoAudit;
import org.glstudio.economy.modules.menu.MainTransactionsMenu;
import org.glstudio.nexus.modules.command.Command;
import org.glstudio.nexus.modules.command.CommandManager;

import java.util.List;

public class TransactionsCommand extends Command {

    private final GLEcoAudit plugin;

    public TransactionsCommand(CommandManager manager, String name, String permission, GLEcoAudit plugin) {
        super(manager, name, permission);
        this.plugin = plugin;
    }

    @Override
    public List<String> aliases() {
        return List.of("trans", "tx");
    }

    @Override
    public List<String> usage() {
        return plugin.getConfigManager().getLangConfig().getStringList("COMMANDS.TRANSACTIONS.USAGE");
    }

    @Override
    public void execute(CommandSender commandSender, String[] strings) {
        if (!(commandSender instanceof Player player)) {
            sendMessage(commandSender, plugin.getConfigManager().getLangConfig().getString("GLOBAL.ONLY_PLAYERS"));
            return;
        }

        if (!player.hasPermission(getPermission())) {
            sendMessage(commandSender, plugin.getConfigManager().getLangConfig().getString("GLOBAL.NO_PERMISSION"));
            return;
        }

        new MainTransactionsMenu(
                plugin.getMenuManager(),
                plugin,
                player
        ).open();
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        return List.of();
    }
}