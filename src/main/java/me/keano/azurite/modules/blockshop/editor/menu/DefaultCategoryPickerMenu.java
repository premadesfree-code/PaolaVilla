package me.keano.azurite.modules.blockshop.editor.menu;

import me.keano.azurite.modules.blockshop.editor.ShopEditorManager;
import me.keano.azurite.modules.framework.menu.Menu;
import me.keano.azurite.modules.framework.menu.MenuManager;
import me.keano.azurite.modules.framework.menu.button.Button;
import me.keano.azurite.utils.ItemBuilder;
import me.keano.azurite.utils.ItemUtils;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Dynamic menu listing all categories so the player can pick the default one.
 */
public class DefaultCategoryPickerMenu extends Menu {

    public DefaultCategoryPickerMenu(MenuManager manager, Player player) {
        super(manager, player, "&8Select Default Category", computeSize(manager), false);
    }

    private static int computeSize(MenuManager manager) {
        int count = manager.getInstance().getShopEditorManager().getCategoryNames().size();
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
        int idx = 0;
        for (String cat : ed().getCategoryNames()) {
            int s = idx + 1;
            if (s > size) break;
            final String category = cat;
            map.put(s, new Button() {
                @Override
                public void onClick(InventoryClickEvent e) {
                    e.setCancelled(true);
                    ed().setDefaultCategory(category);
                    new ShopEditorMenu(getManager(), player).open();
                }

                @Override
                public ItemStack getItemStack() {
                    String mat = ed().getCategoryMaterial(category);
                    ItemStack base = new ItemStack(ItemUtils.getMat(mat));
                    ItemBuilder b = new ItemBuilder(base);
                    b.setName(ed().getCategoryName(category));
                    List<String> lore = new ArrayList<>(ed().getCategoryLore(category));
                    lore.add("&7");
                    lore.add("&eClick to set default category!");
                    b.setLore(lore);
                    return b.toItemStack();
                }
            });
            idx++;
        }
        return map;
    }
}
