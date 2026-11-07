package org.glstudio.ecoaudit.modules.menu;

import org.glstudio.ecoaudit.GEcoAudit;
import org.glstudio.nexus.modules.menu.placeholder.Placeholders;
import org.glstudio.nexus.utils.CC;

/**
 * Texts of {@code language.yml} rendered as legacy strings for item names and lore.
 */
final class MenuTexts {

    private MenuTexts() {
    }

    static String text(GEcoAudit plugin, String key, Object... pairs) {
        return CC.t(Placeholders.apply(plugin.getMessages().raw(key, ""), pairs));
    }
}
