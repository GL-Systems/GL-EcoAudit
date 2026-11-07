package org.glstudio.ecoaudit.modules.command;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.glstudio.ecoaudit.GEcoAudit;
import org.glstudio.ecoaudit.core.util.Permissions;
import org.glstudio.ecoaudit.modules.menu.MainTransactionsMenu;
import org.glstudio.ecoaudit.modules.menu.SentTransactionsMenu;
import org.glstudio.ecoaudit.modules.model.TransactionRecord;
import org.glstudio.ecoaudit.modules.service.TransactionService;
import org.glstudio.nexus.modules.command.Command;
import org.glstudio.nexus.modules.command.CommandManager;
import org.glstudio.nexus.modules.lang.LanguageFile;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * {@code /transactions [top [sent|received] | search <name> | <player>]}: the player's payment
 * history, the rankings, a name search, or another player's history.
 *
 * <p>Not a {@code CommandGroup}: the first argument is either a subcommand or a player name, and
 * administrators get player names in its completion.
 */
public class TransactionsCommand extends Command {

    private final GEcoAudit plugin;

    /**
     * Creates the command.
     *
     * @param manager    the command manager
     * @param name       the command label
     * @param permission the permission needed to use the command at all
     * @param plugin     the plugin
     */
    public TransactionsCommand(CommandManager manager, String name, String permission, GEcoAudit plugin) {
        super(manager, name, permission);
        this.plugin = plugin;
    }

    @Override
    public List<String> aliases() {
        return List.of("trans", "tx");
    }

    @Override
    public List<String> usage() {
        return plugin.getMessages().rawList("COMMANDS.TRANSACTIONS.USAGE");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            openOwn(sender);
            return;
        }

        switch (args[0].toLowerCase()) {
            case "top" -> handleTop(sender, args);
            case "search" -> handleSearch(sender, args);
            default -> handleInspect(sender, args[0]);
        }
    }

    private void openOwn(CommandSender sender) {
        LanguageFile messages = plugin.getMessages();
        if (!(sender instanceof Player player)) {
            messages.send(sender, "GLOBAL.ONLY_PLAYERS");
            return;
        }
        if (!player.hasPermission(getPermission())) {
            messages.send(sender, "GLOBAL.NO_PERMISSION");
            return;
        }
        new MainTransactionsMenu(plugin.getMenuManager(), plugin, player).open();
    }

    private void handleInspect(CommandSender sender, String targetName) {
        LanguageFile messages = plugin.getMessages();
        if (!sender.hasPermission(Permissions.ADMIN)) {
            messages.send(sender, "GLOBAL.NO_PERMISSION");
            return;
        }

        if (!(sender instanceof Player)) {
            messages.send(sender, "GLOBAL.ONLY_PLAYERS");
            return;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayerIfCached(targetName);
        if (target == null) {
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (online.getName().equalsIgnoreCase(targetName)) {
                    target = online;
                    break;
                }
            }
        }
        if (target == null) {
            messages.send(sender, "COMMANDS.TRANSACTIONS.PLAYER_NOT_FOUND", "{player}", targetName);
            return;
        }

        // The menu is built for, and opened to, the inspected player.
        new MainTransactionsMenu(plugin.getMenuManager(), plugin, (Player) target).open();
    }

    private void handleSearch(CommandSender sender, String[] args) {
        LanguageFile messages = plugin.getMessages();
        if (!(sender instanceof Player player)) {
            messages.send(sender, "GLOBAL.ONLY_PLAYERS");
            return;
        }
        if (!player.hasPermission(getPermission())) {
            messages.send(sender, "GLOBAL.NO_PERMISSION");
            return;
        }

        if (args.length < 2) {
            messages.send(sender, "COMMANDS.TRANSACTIONS.SEARCH_USAGE");
            return;
        }

        String query = args[1];
        new SentTransactionsMenu(plugin.getMenuManager(), plugin, player, query).open();
    }

    private void handleTop(CommandSender sender, String[] args) {
        LanguageFile messages = plugin.getMessages();
        if (!sender.hasPermission(Permissions.ADMIN)) {
            messages.send(sender, "GLOBAL.NO_PERMISSION");
            return;
        }

        boolean topSenders = true;
        if (args.length >= 2) {
            if (args[1].equalsIgnoreCase("received")) {
                topSenders = false;
            } else if (!args[1].equalsIgnoreCase("sent")) {
                messages.send(sender, "COMMANDS.TRANSACTIONS.TOP_USAGE");
                return;
            }
        }

        int topLimit = plugin.getConfigManager().getMainConfig().getInt("TRANSACTIONS.TOP_LIMIT", 10);
        TransactionService ts = plugin.getTransactionService();

        messages.send(sender, "COMMANDS.TRANSACTIONS.TOP_SEPARATOR");
        messages.send(sender, topSenders ? "COMMANDS.TRANSACTIONS.TOP_TITLE_SENT" : "COMMANDS.TRANSACTIONS.TOP_TITLE_RECEIVED");
        sender.sendMessage("");

        if (topSenders) {
            ts.getTopSendersAsync(topLimit, records -> displayTop(sender, records, true));
        } else {
            ts.getTopReceiversAsync(topLimit, records -> displayTop(sender, records, false));
        }
    }

    private void displayTop(CommandSender sender, List<TransactionRecord> records, boolean senders) {
        LanguageFile messages = plugin.getMessages();
        if (records.isEmpty()) {
            messages.send(sender, "COMMANDS.TRANSACTIONS.TOP_EMPTY");
            messages.send(sender, "COMMANDS.TRANSACTIONS.TOP_SEPARATOR");
            return;
        }

        DecimalFormat df = new DecimalFormat(plugin.getConfigManager().getMainConfig()
                .getString("FORMATTING.ECONOMY_FORMAT", "#,##0.00"));
        String direction = messages.raw(senders
                ? "COMMANDS.TRANSACTIONS.TOP_DIRECTION_SENT"
                : "COMMANDS.TRANSACTIONS.TOP_DIRECTION_RECEIVED", "");

        int rank = 1;
        for (TransactionRecord rec : records) {
            // Ranking rows carry the number of payments in the timestamp.
            messages.send(sender, "COMMANDS.TRANSACTIONS.TOP_ENTRY",
                    "{rank}", rank,
                    "{name}", rec.senderName(),
                    "{total}", df.format(rec.amount()),
                    "{count}", rec.timestamp(),
                    "{direction}", direction);
            rank++;
        }
        messages.send(sender, "COMMANDS.TRANSACTIONS.TOP_SEPARATOR");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            List<String> completions = new ArrayList<>();
            completions.add("top");
            completions.add("search");
            if (sender.hasPermission(Permissions.ADMIN)) {
                for (Player online : Bukkit.getOnlinePlayers()) {
                    completions.add(online.getName());
                }
            }
            return filterTabCompletion(completions, args[0]);
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("top")) {
            return filterTabCompletion(List.of("sent", "received"), args[1]);
        }
        return List.of();
    }
}
