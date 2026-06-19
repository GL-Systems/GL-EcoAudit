package org.glstudio.economy.modules.menu;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.glstudio.economy.GLEcoAudit;
import org.glstudio.economy.common.framework.menu.Menu;
import org.glstudio.economy.common.framework.menu.MenuManager;
import org.glstudio.economy.common.framework.menu.button.Button;
import org.glstudio.economy.common.framework.menu.button.ButtonBuilder;
import org.glstudio.economy.modules.services.VaultService;
import org.glstudio.nexus.utils.CC;
import org.glstudio.nexus.utils.ConfigFile;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class MainTransactionsMenu extends Menu {

    private final GLEcoAudit plugin;
    private final VaultService vaultService;

    public MainTransactionsMenu(MenuManager manager, GLEcoAudit plugin, Player p) {
        super(manager, plugin, p, CC.t(plugin.getConfigManager().getTransactionsMenu()
                .getString("TITLE", "&#c084fcTransactions")), 27, false);
        this.plugin = plugin;
        this.vaultService = plugin.getVaultService();
    }

    @Override
    public Map<Integer, Button> getButtons(Player p) {
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
            List<String> lore = config.getStringList("PLAYER_HEAD.LORE");
            String balanceFormatted = formatBalance(p);
            lore = lore.stream()
                    .map(line -> CC.t(line
                            .replace("{player}", p.getName())
                            .replace("{balance}", balanceFormatted)))
                    .collect(Collectors.toList());
            meta.setLore(lore);
            head.setItemMeta(meta);
        }
        return new Button(head);
    }

    private String formatBalance(Player p) {
        try {
            double balance = vaultService.getBalance(p);
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
            new SentTransactionsMenu(plugin.getMenuManager(), plugin, player).open();
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
            new ReceivedTransactionsMenu(plugin.getMenuManager(), plugin, player).open();
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