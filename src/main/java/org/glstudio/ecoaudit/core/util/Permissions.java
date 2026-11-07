package org.glstudio.ecoaudit.core.util;

/**
 * Permission nodes, following {@code golden.ecoaudit.<action>}.
 */
public final class Permissions {

    /** Use {@code /transactions} for one's own history. */
    public static final String TRANSACTIONS = "golden.ecoaudit.transactions";
    /** Inspect other players, see the rankings and use {@code /ecoaudit}. */
    public static final String ADMIN = "golden.ecoaudit.admin";

    private Permissions() {
    }
}
