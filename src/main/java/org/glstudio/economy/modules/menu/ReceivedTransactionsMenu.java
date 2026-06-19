package org.glstudio.economy.modules.menu;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.glstudio.economy.GLEcoAudit;
import org.glstudio.economy.common.framework.menu.MenuManager;
import org.glstudio.economy.common.framework.menu.button.Button;
import org.glstudio.economy.common.framework.menu.button.ButtonBuilder;
import org.glstudio.economy.common.framework.menu.paginated.PaginatedMenu;
import org.glstudio.economy.modules.model.TransactionRecord;
import org.glstudio.economy.modules.services.TransactionService;
import org.glstudio.nexus.utils.CC;
import org.glstudio.nexus.utils.ConfigFile;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReceivedTransactionsMenu extends PaginatedMenu {

    private final GLEcoAudit plugin;
    private final TransactionService transactionService;
    private final Player player;
    private List<TransactionRecord> currentPageData;
    private boolean loading = true;

    public ReceivedTransactionsMenu(MenuManager manager, GLEcoAudit plugin, Player p) {
        super(manager, p, CC.t(plugin.getConfigManager().getPositiveTransactionsMenu()
                .getString("TITLE", "&#51cf66Received Transactions")), 54,
                plugin.getConfigManager().getMainConfig().getInt("TRANSACTIONS.MAX_ENTRIES_PER_PAGE", 45));
        this.plugin = plugin;
        this.player = p;
        this.transactionService = plugin.getTransactionService();

        ConfigFile menuConfig = plugin.getConfigManager().getPositiveTransactionsMenu();
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
    public List<Button> getAllButtons(Player p) {
        return new ArrayList<>();
    }

    @Override
    public Map<Integer, Button> getButtons(Player p) {
        Map<Integer, Button> buttons = new HashMap<>();
        ConfigFile menuConfig = plugin.getConfigManager().getPositiveTransactionsMenu();
        String dateFormat = safeDateFormat(plugin.getConfigManager().getMainConfig()
                .getString("FORMATTING.DATE_FORMAT", "dd/MM/yyyy hh:mm:ss a"));
        SimpleDateFormat sdf = new SimpleDateFormat(dateFormat);
        String economyPattern = plugin.getConfigManager().getMainConfig()
                .getString("FORMATTING.ECONOMY_FORMAT", "#,##0.00");
        DecimalFormat df = new DecimalFormat(economyPattern);

        if (loading || currentPageData == null) {
            buttons.put(4, Button.builder()
                    .material(Material.HOPPER)
                    .name(CC.t("&#ffff00Loading..."))
                    .build());
        } else if (currentPageData.isEmpty()) {
            buttons.put(4, Button.builder()
                    .material(Material.BARRIER)
                    .name(CC.t(menuConfig.getString("NO_TRANSACTIONS", "&#ff6b6bNo transactions to display.")))
                    .build());
        } else {
            int slot = 0;
            for (TransactionRecord record : currentPageData) {
                String targetName = record.getSenderName();
                String amount = df.format(record.getAmount());
                String date = sdf.format(new Date(record.getTimestamp()));
                buttons.put(slot++, buildTransactionItem(record, targetName, amount, date));
            }
        }

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
        transactionService.getReceivedTransactionsAsync(player.getUniqueId(), offset, maxItemsPerPage, records -> {
            currentPageData = records;
            loading = false;
            update();
        });
    }

    private Button buildTransactionItem(TransactionRecord record, String targetName, String amount, String date) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        if (meta != null) {
            meta.setOwningPlayer(plugin.getServer().getOfflinePlayer(record.getSenderUUID()));
            ConfigFile menuConfig = plugin.getConfigManager().getPositiveTransactionsMenu();
            meta.setDisplayName(CC.t(menuConfig
                    .getString("ENTRY_NAME", "&#f0e8ffFrom: {target}")
                    .replace("{target}", targetName)));
            List<String> lore = new ArrayList<>();
            for (String line : menuConfig
                    .getStringList("ENTRY_LORE")) {
                lore.add(CC.t(line
                        .replace("{target}", targetName)
                        .replace("{amount}", amount)
                        .replace("{date}", date)));
            }
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return Button.builder().item(item).build();
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
            plugin.getLogger().warning("Invalid material at " + path + " in transactions-received.yml, using " + defaultMaterial);
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