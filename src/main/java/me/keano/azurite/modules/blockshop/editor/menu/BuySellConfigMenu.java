package me.keano.azurite.modules.blockshop.editor.menu;

import me.keano.azurite.modules.blockshop.editor.ShopEditorManager;
import me.keano.azurite.modules.framework.menu.Menu;
import me.keano.azurite.modules.framework.menu.MenuManager;
import me.keano.azurite.modules.framework.menu.button.Button;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Lists buy/sell items of a category in a dynamic menu.
 * Clicking an item opens the Buy/Sell Item Edit Menu.
 */
public class BuySellConfigMenu extends Menu {

    private final String category;

    public BuySellConfigMenu(MenuManager manager, Player player, String category) {
        super(manager, player, "&8Buy/Sell Items - " + category, computeSize(manager, category), false);
        this.category = category;
    }

    private static int computeSize(MenuManager manager, String category) {
        ShopEditorManager ed = manager.getInstance().getShopEditorManager();
        int count = ed.getItemIds(category).size();
        if (count <= 0) return 9;
        int rows = (count - 1) / 9 + 1;
        return Math.min(rows, 6) * 9;
    }

    private ShopEditorManager ed() {
        return getInstance().getShopEditorManager();
    }

    @Override
    public Map<Integer, Button> getButtons(Player player) {
        Map<Integer, Button> map = new HashMap<>();
        List<String> ids = ed().getItemIds(category);
        int idx = 0;
        for (String id : ids) {
            int s = idx + 1;
            if (s > size) break;
            final String itemId = id;
            map.put(s, new Button() {
                @Override
                public void onClick(InventoryClickEvent e) {
                    e.setCancelled(true);
                    new BuySellItemEditMenu(getManager(), player, category, itemId).open();
                }

                @Override
                public ItemStack getItemStack() {
                    return ed().buildShopItem(category, itemId, player);
                }
            });
            idx++;
        }
        return map;
    }

    public String getCategory() {
        return category;
    }
}
