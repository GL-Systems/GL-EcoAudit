package org.glstudio.economy.common.framework.menu.paginated;

import lombok.Getter;
import lombok.Setter;
import org.glstudio.economy.common.framework.menu.Menu;
import org.glstudio.economy.common.framework.menu.MenuManager;
import org.glstudio.economy.common.framework.menu.button.Button;
import org.glstudio.economy.common.framework.menu.button.ButtonPresets;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
public abstract class PaginatedMenu extends Menu {

    protected int page = 0;
    protected int maxItemsPerPage;

    protected int previousPageSlot = 48;
    protected int nextPageSlot = 50;
    protected int closeSlot = 49;
    protected int backSlot = -1;
    protected Button backButton;

    public PaginatedMenu(MenuManager manager, Player p, String title, int size) {
        super(manager, p, title, size, false);
        this.maxItemsPerPage = calculateMaxItems(size);
    }

    public PaginatedMenu(MenuManager manager, Player p, String title, int size, int maxItemsPerPage) {
        super(manager, p, title, size, false);
        this.maxItemsPerPage = maxItemsPerPage;
    }

    private int calculateMaxItems(int size) {
        int rows = size / 9;
        return (rows - 1) * 9;
    }

    @Override
    public Map<Integer, Button> getButtons(Player p) {
        Map<Integer, Button> buttons = new HashMap<>();

        List<Button> allButtons = getAllButtons(p);

        int maxPage = (int) Math.ceil((double) allButtons.size() / maxItemsPerPage) - 1;
        if (maxPage < 0) maxPage = 0;

        int startIndex = page * maxItemsPerPage;
        int endIndex = Math.min(startIndex + maxItemsPerPage, allButtons.size());

        int slot = 0;
        for (int i = startIndex; i < endIndex; i++) {
            buttons.put(slot++, allButtons.get(i));
        }

        if (backSlot >= 0 && backButton != null) {
            buttons.put(backSlot, backButton);
        }

        if (page > 0) {
            buttons.put(previousPageSlot, ButtonPresets.previousPage(() -> {
                page--;
                update();
            }));
        }

        if (page < maxPage) {
            buttons.put(nextPageSlot, ButtonPresets.nextPage(() -> {
                page++;
                update();
            }));
        }

        buttons.put(closeSlot, ButtonPresets.close());

        return buttons;
    }

    public abstract List<Button> getAllButtons(Player p);

    public void goToPage(int page) {
        this.page = Math.max(0, page);
        update();
    }

    public void nextPage() {
        page++;
        update();
    }

    public void previousPage() {
        if (page > 0) {
            page--;
            update();
        }
    }

    public int getTotalPages(List<Button> allButtons) {
        return Math.max(1, (int) Math.ceil((double) allButtons.size() / maxItemsPerPage));
    }
}