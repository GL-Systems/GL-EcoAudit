package org.glstudio.economy.modules.services;

import lombok.Getter;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.OfflinePlayer;

@Getter
public class VaultService {

    private final Economy economy;

    public VaultService(Economy economy) {
        this.economy = economy;
    }

    public double getBalance(OfflinePlayer player) {
        return economy.getBalance(player);
    }

    public String format(double amount) {
        return economy.format(amount);
    }

    public boolean hasAccount(OfflinePlayer player) {
        return economy.hasAccount(player);
    }

    public String currencyNamePlural() {
        return economy.currencyNamePlural();
    }

    public String currencyNameSingular() {
        return economy.currencyNameSingular();
    }
}