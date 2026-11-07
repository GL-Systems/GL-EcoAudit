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
import org.glstudio.ecoaudit.modules.model.TransactionRecord;
import org.glstudio.ecoaudit.modules.service.TransactionService;
import org.glstudio.nexus.utils.CC;
import org.glstudio.nexus.utils.ConfigFile;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Paged list of the payments the player made, newest first, or of a name search. Each page is
 * loaded in the background when it is opened.
 */
public class SentTransactionsMenu extends Menu {

    private final GEcoAudit plugin;
    private final TransactionService transactionService;
    private final Player player;
    private final String searchQuery;
    private List<TransactionRecord> currentPageData;
    private boolean loading = true;

    private int page = 0;
    private final int maxItemsPerPage;
    private int previousPageSlot;
    private int nextPageSlot;
    private int closeSlot;
    private int backSlot = -1;
    private Button backButton;

    /**
     * Creates the menu and starts loading the first page.
     *
     * @param manager     the menu manager
     * @param plugin      the plugin
     * @param p           whose payments are shown, also the viewer
     * @param searchQuery part of a name to search for, or {@code null} for the player's own list
     */
    public SentTransactionsMenu(MenuManager manager, GEcoAudit plugin, Player p, String searchQuery) {
        super(manager, p, CC.t(plugin.getConfigManager().getNegativeTransactionsMenu()
                .getString("TITLE", "&#ff6b6bSent Transactions")), 54);
        this.maxItemsPerPage = plugin.getConfigManager().getMainConfig()
                .getInt("TRANSACTIONS.MAX_ENTRIES_PER_PAGE", 45);
        this.plugin = plugin;
        this.player = p;
        this.transactionService = plugin.getTransactionService();
        this.searchQuery = searchQuery;

        ConfigFile menuConfig = plugin.getConfigManager().getNegativeTransactionsMenu();
        this.backSlot = menuConfig.getInt("BACK.SLOT", 48);
        this.previousPageSlot = menuConfig.getInt("PREVIOUS_PAGE.SLOT", 47);
        this.nextPageSlot = menuConfig.getInt("NEXT_PAGE.SLOT", 50);
        this.closeSlot = menuConfig.getInt("CLOSE.SLOT", 49);

        Button backButton = buildBackButton(menuConfig);
        if (backButton != null) {
            this.backButton = backButton;
        }

        loadPageAsync(0);
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
        ConfigFile menuConfig = plugin.getConfigManager().getNegativeTransactionsMenu();
        String dateFormat = safeDateFormat(plugin.getConfigManager().getMainConfig()
                .getString("FORMATTING.DATE_FORMAT", "dd/MM/yyyy hh:mm:ss a"));
        SimpleDateFormat sdf = new SimpleDateFormat(dateFormat);
        String economyPattern = plugin.getConfigManager().getMainConfig()
                .getString("FORMATTING.ECONOMY_FORMAT", "#,##0.00");
        DecimalFormat df = new DecimalFormat(economyPattern);

        if (loading || currentPageData == null) {
            buttons.put(4, Button.builder()
                    .material(Material.HOPPER)
                    .name(MenuTexts.text(plugin, "MENUS.LOADING"))
                    .build());
        } else if (currentPageData.isEmpty()) {
            buttons.put(4, Button.builder()
                    .material(Material.BARRIER)
                    .name(CC.t(menuConfig.getString("NO_TRANSACTIONS", "&#ff6b6bNo transactions to display.")))
                    .build());
        } else {
            // Slot 0 holds the search button.
            int slot = 1;
            for (TransactionRecord record : currentPageData) {
                String targetName = record.recipientName();
                String amount = df.format(record.amount());
                String date = sdf.format(new Date(record.timestamp()));
                buttons.put(slot++, buildTransactionItem(record, targetName, amount, date, df));
            }
        }

        buttons.put(0, buildSearchButton(menuConfig));

        if (backSlot >= 0 && backButton != null) {
            buttons.put(backSlot, backButton);
        }
        if (page > 0) {
            buttons.put(previousPageSlot, buildPreviousPageButton(menuConfig));
        }
        if (currentPageData != null && currentPageData.size() >= maxItemsPerPage) {
            buttons.put(nextPageSlot, buildNextPageButton(menuConfig));
        }
        buttons.put(closeSlot, buildCloseButton(menuConfig));

        return buttons;
    }

    private void loadPageAsync(int newPage) {
        loading = true;
        int offset = newPage * maxItemsPerPage;

        if (searchQuery != null && !searchQuery.isEmpty()) {
            transactionService.searchTransactionsAsync(searchQuery, offset, maxItemsPerPage, records -> {
                currentPageData = records;
                loading = false;
                update();
            });
        } else {
            transactionService.getSentTransactionsAsync(player.getUniqueId(), offset, maxItemsPerPage, records -> {
                currentPageData = records;
                loading = false;
                update();
            });
        }
    }

    private Button buildSearchButton(ConfigFile config) {
        Material material = safeMaterial(config, "SEARCH.MATERIAL", "COMPASS");
        String name = CC.t(config.getString("SEARCH.NAME", "&bSearch"));
        List<String> lore = new ArrayList<>();
        if (searchQuery != null && !searchQuery.isEmpty()) {
            lore.add(MenuTexts.text(plugin, "MENUS.SEARCHING", "{query}", searchQuery));
            lore.add(MenuTexts.text(plugin, "MENUS.CLEAR_SEARCH"));
        } else {
            for (String line : config.getStringList("SEARCH.LORE")) {
                lore.add(CC.t(line));
            }
        }

        ButtonBuilder builder = Button.builder()
                .material(material)
                .amount(1)
                .name(name)
                .lore(lore);

        builder.onClick(e -> {
            Player player = (Player) e.getWhoClicked();
            if (searchQuery != null && !searchQuery.isEmpty()) {
                new SentTransactionsMenu(plugin.getMenuManager(), plugin, player, null).open();
            } else {
                player.closeInventory();
                plugin.getChatInput().request(player)
                        .onInput(input -> new SentTransactionsMenu(plugin.getMenuManager(), plugin, player, input).open())
                        .start();
                player.sendMessage(MenuTexts.text(plugin, "MENUS.SEARCH_PROMPT"));
            }
        });

        return builder.build();
    }

    private Button buildTransactionItem(TransactionRecord record, String targetName, String amount, String date, DecimalFormat df) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        if (meta != null) {
            meta.setOwningPlayer(plugin.getServer().getOfflinePlayer(record.recipientUUID()));
            ConfigFile menuConfig = plugin.getConfigManager().getNegativeTransactionsMenu();
            meta.setDisplayName(CC.t(menuConfig
                    .getString("ENTRY_NAME", "&#f0e8ffTo: {target}")
                    .replace("{target}", targetName)));
            List<String> lore = new ArrayList<>();
            for (String line : menuConfig.getStringList("ENTRY_LORE")) {
                lore.add(CC.t(line
                        .replace("{target}", targetName)
                        .replace("{amount}", amount)
                        .replace("{date}", date)));
            }
            String cat = record.category();
            if (cat != null && !cat.isEmpty()) {
                lore.add(MenuTexts.text(plugin, "MENUS.CATEGORY", "{category}", cat));
            }
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return ButtonBuilder.of(item).build();
    }

    private Button buildBackButton(ConfigFile config) {
        Material material = safeMaterial(config, "BACK.MATERIAL", "ARROW");
        String name = CC.t(config.getString("BACK.NAME", "&eBack"));
        List<String> lore = config.getStringList("BACK.LORE");
        int amount = config.getInt("BACK.AMOUNT", 1);

        ButtonBuilder builder = Button.builder()
                .material(material)
                .amount(amount)
                .name(name)
                .lore(lore);

        if (config.getBoolean("BACK.GLOW", false)) builder.glow();

        builder.onClick(e -> {
            Player player = (Player) e.getWhoClicked();
            new MainTransactionsMenu(plugin.getMenuManager(), plugin, player).open();
        });

        return builder.build();
    }

    private Button buildNextPageButton(ConfigFile config) {
        Material material = safeMaterial(config, "NEXT_PAGE.MATERIAL", "ARROW");
        String name = CC.t(config.getString("NEXT_PAGE.NAME", "&aNext Page"));
        List<String> lore = config.getStringList("NEXT_PAGE.LORE");
        int amount = config.getInt("NEXT_PAGE.AMOUNT", 1);

        ButtonBuilder builder = Button.builder()
                .material(material)
                .amount(amount)
                .name(name)
                .lore(lore);

        if (config.getBoolean("NEXT_PAGE.GLOW", false)) builder.glow();

        builder.onClick(e -> {
            page++;
            loadPageAsync(page);
        });

        return builder.build();
    }

    private Button buildPreviousPageButton(ConfigFile config) {
        Material material = safeMaterial(config, "PREVIOUS_PAGE.MATERIAL", "ARROW");
        String name = CC.t(config.getString("PREVIOUS_PAGE.NAME", "&aPrevious Page"));
        List<String> lore = config.getStringList("PREVIOUS_PAGE.LORE");
        int amount = config.getInt("PREVIOUS_PAGE.AMOUNT", 1);

        ButtonBuilder builder = Button.builder()
                .material(material)
                .amount(amount)
                .name(name)
                .lore(lore);

        if (config.getBoolean("PREVIOUS_PAGE.GLOW", false)) builder.glow();

        builder.onClick(e -> {
            if (page > 0) {
                page--;
                loadPageAsync(page);
            }
        });

        return builder.build();
    }

    private Button buildCloseButton(ConfigFile config) {
        Material material = safeMaterial(config, "CLOSE.MATERIAL", "BARRIER");
        String name = CC.t(config.getString("CLOSE.NAME", "&cClose"));
        List<String> lore = config.getStringList("CLOSE.LORE");
        int amount = config.getInt("CLOSE.AMOUNT", 1);

        ButtonBuilder builder = Button.builder()
                .material(material)
                .amount(amount)
                .name(name)
                .lore(lore);

        if (config.getBoolean("CLOSE.GLOW", false)) builder.glow();

        builder.onClick(e -> ((Player) e.getWhoClicked()).closeInventory());

        return builder.build();
    }

    private Material safeMaterial(ConfigFile config, String path, String defaultMaterial) {
        try {
            String matName = config.getString(path, defaultMaterial);
            return Material.valueOf(matName.toUpperCase());
        } catch (Exception e) {
            plugin.getLogger().warning("Invalid material at " + path + " in transactions-sent.yml, using " + defaultMaterial);
            return Material.valueOf(defaultMaterial);
        }
    }

    private String safeDateFormat(String pattern) {
        try {
            new SimpleDateFormat(pattern);
            return pattern;
        } catch (Exception e) {
            plugin.getLogger().warning("Invalid date format '" + pattern + "', using dd/MM/yyyy hh:mm:ss a");
            return "dd/MM/yyyy hh:mm:ss a";
        }
    }
}
