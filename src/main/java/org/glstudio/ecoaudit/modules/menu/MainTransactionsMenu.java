package org.glstudio.ecoaudit.modules.menu;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.glstudio.ecoaudit.GEcoAudit;
import org.glstudio.nexus.modules.menu.Menu;
import org.glstudio.nexus.modules.menu.MenuManager;
import org.glstudio.nexus.modules.menu.button.Button;
import org.glstudio.nexus.modules.menu.button.ButtonBuilder;
import org.glstudio.ecoaudit.modules.service.TransactionService;
import org.glstudio.nexus.concurrent.Futures;
import org.glstudio.nexus.hooks.economy.VaultEconomy;
import org.glstudio.nexus.utils.CC;
import org.glstudio.nexus.utils.ConfigFile;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.stream.Collectors;

/**
 * {@code /transactions} main screen: the player's head with balance and totals, and the buttons to
 * the sent and received lists. The totals load in the background.
 */
public class MainTransactionsMenu extends Menu {

    private final GEcoAudit plugin;
    private final Player player;

    private boolean statsLoaded = false;
    private int sentCount;
    private int receivedCount;
    private double sentTotal;
    private double receivedTotal;

    /**
     * Creates the menu and starts loading the totals.
     *
     * @param manager the menu manager
     * @param plugin  the plugin
     * @param p       whose payments are shown, also the viewer
     */
    public MainTransactionsMenu(MenuManager manager, GEcoAudit plugin, Player p) {
        super(manager, p, CC.t(plugin.getConfigManager().getTransactionsMenu()
                .getString("TITLE", "&#c084fcTransactions")), 27);
        this.plugin = plugin;
        this.player = p;
        loadStatsAsync();
    }

    private void loadStatsAsync() {
        TransactionService ts = plugin.getTransactionService();
        Futures.log(CompletableFuture.runAsync(() -> {
            int sc = ts.getSentCount(player.getUniqueId());
            int rc = ts.getReceivedCount(player.getUniqueId());
            double st = ts.getTotalSent(player.getUniqueId());
            double rt = ts.getTotalReceived(player.getUniqueId());
            plugin.getTasks().mainThread().execute(() -> {
                sentCount = sc;
                receivedCount = rc;
                sentTotal = st;
                receivedTotal = rt;
                statsLoaded = true;
                update();
            });
        }, plugin.getExecutors().io()), "MainTransactionsMenu#loadStatsAsync");
    }

    @Override
    protected void render() {
        clear();
        for (Map.Entry<Integer, Button> entry : getButtons(player).entrySet()) {
            place(entry.getKey(), entry.getValue());
        }
    }

    private Map<Integer, Button> getButtons(Player p) {
        Map<Integer, Button> buttons = new HashMap<>();
        ConfigFile menuConfig = plugin.getConfigManager().getTransactionsMenu();

        buttons.put(menuConfig.getInt("PLAYER_HEAD.SLOT", 13),
                buildPlayerHead(p, menuConfig));

        buttons.put(menuConfig.getInt("SENT.SLOT", 11),
                buildSentButton(menuConfig));

        buttons.put(menuConfig.getInt("RECEIVED.SLOT", 15),
                buildReceivedButton(menuConfig));

        if (menuConfig.getBoolean("BORDER.ENABLED", false)) {
            Button border = buildBorder(menuConfig);
            for (int i = 0; i < 27; i++) {
                buttons.putIfAbsent(i, border);
            }
        }

        return buttons;
    }

    private Button buildPlayerHead(Player p, ConfigFile config) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (meta != null) {
            meta.setOwningPlayer(p);
            meta.setDisplayName(CC.t(config.getString("PLAYER_HEAD.NAME", "&f{player}")
                    .replace("{player}", p.getName())));
            String balanceFormatted = formatBalance(p);
            String economyPattern = plugin.getConfigManager().getMainConfig()
                    .getString("FORMATTING.ECONOMY_FORMAT", "#,##0.00");
            DecimalFormat df = new DecimalFormat(economyPattern);

            List<String> lore = new ArrayList<>();
            for (String line : config.getStringList("PLAYER_HEAD.LORE")) {
                lore.add(CC.t(line
                        .replace("{player}", p.getName())
                        .replace("{balance}", balanceFormatted)));
            }

            lore.add(MenuTexts.text(plugin, "MENUS.STATS_SEPARATOR"));
            if (statsLoaded) {
                lore.add(MenuTexts.text(plugin, "MENUS.STATS_SENT", "{count}", sentCount, "{total}", df.format(sentTotal)));
                lore.add(MenuTexts.text(plugin, "MENUS.STATS_RECEIVED", "{count}", receivedCount, "{total}", df.format(receivedTotal)));
            } else {
                lore.add(MenuTexts.text(plugin, "MENUS.STATS_LOADING"));
            }

            meta.setLore(lore);
            head.setItemMeta(meta);
        }
        return Button.of(head);
    }

    private String formatBalance(Player p) {
        try {
            double balance = VaultEconomy.get().balance(p);
            String pattern = plugin.getConfigManager().getMainConfig()
                    .getString("FORMATTING.ECONOMY_FORMAT", "#,##0.00");
            DecimalFormat df = new DecimalFormat(pattern);
            return df.format(balance);
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to get balance for " + p.getName(), e);
            return "0.00";
        }
    }

    private Button buildSentButton(ConfigFile config) {
        Material material = safeMaterial(config, "SENT.MATERIAL", "RED_STAINED_GLASS_PANE");
        String name = CC.t(config.getString("SENT.NAME", "&cMoney Sent"));
        List<String> lore = config.getStringList("SENT.LORE").stream()
                .map(CC::t)
                .collect(Collectors.toList());
        int amount = config.getInt("SENT.AMOUNT", 1);

        ButtonBuilder builder = Button.builder()
                .material(material)
                .amount(amount)
                .name(name)
                .lore(lore);

        if (config.getBoolean("SENT.GLOW", false)) {
            builder.glow();
        }

        builder.onClick(e -> {
            Player player = (Player) e.getWhoClicked();
            new SentTransactionsMenu(plugin.getMenuManager(), plugin, player, null).open();
        });

        return builder.build();
    }

    private Button buildReceivedButton(ConfigFile config) {
        Material material = safeMaterial(config, "RECEIVED.MATERIAL", "GREEN_STAINED_GLASS_PANE");
        String name = CC.t(config.getString("RECEIVED.NAME", "&aMoney Received"));
        List<String> lore = config.getStringList("RECEIVED.LORE").stream()
                .map(CC::t)
                .collect(Collectors.toList());
        int amount = config.getInt("RECEIVED.AMOUNT", 1);

        ButtonBuilder builder = Button.builder()
                .material(material)
                .amount(amount)
                .name(name)
                .lore(lore);

        if (config.getBoolean("RECEIVED.GLOW", false)) {
            builder.glow();
        }

        builder.onClick(e -> {
            Player player = (Player) e.getWhoClicked();
            new ReceivedTransactionsMenu(plugin.getMenuManager(), plugin, player, null).open();
        });

        return builder.build();
    }

    private Button buildBorder(ConfigFile config) {
        Material material = safeMaterial(config, "BORDER.MATERIAL", "BLACK_STAINED_GLASS_PANE");
        int amount = config.getInt("BORDER.AMOUNT", 1);
        String name = CC.t(config.getString("BORDER.NAME", " "));
        List<String> lore = config.getStringList("BORDER.LORE");

        ButtonBuilder builder = Button.builder()
                .material(material)
                .amount(amount)
                .name(name)
                .lore(lore);

        if (config.getBoolean("BORDER.GLOW", false)) {
            builder.glow();
        }

        return builder.build();
    }

    private Material safeMaterial(ConfigFile config, String path, String defaultMaterial) {
        try {
            String matName = config.getString(path, defaultMaterial);
            return Material.valueOf(matName.toUpperCase());
        } catch (Exception e) {
            plugin.getLogger().warning("Invalid material at " + path + ", using " + defaultMaterial);
            return Material.valueOf(defaultMaterial);
        }
    }
}
