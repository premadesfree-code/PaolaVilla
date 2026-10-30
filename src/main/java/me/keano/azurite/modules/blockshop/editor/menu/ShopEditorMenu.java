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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * /shop editor - main menu (5 rows / 45 slots).
 */
public class ShopEditorMenu extends Menu {

    private static final Set<Integer> BORDER = new HashSet<>();
    static {
        for (int i = 0; i <= 8; i++) BORDER.add(i);
        int[] sides = {9, 17, 18, 26, 27, 35};
        for (int s : sides) BORDER.add(s);
        for (int i = 36; i <= 44; i++) BORDER.add(i);
    }

    public ShopEditorMenu(MenuManager manager, Player player) {
        super(manager, player, "&8Shop Categories", 45, false);
    }

    private ShopEditorManager ed() {
        return getInstance().getShopEditorManager();
    }

    @Override
    public Map<Integer, Button> getButtons(Player player) {
        Map<Integer, Button> map = new HashMap<>();

        // borders
        for (int slot : BORDER) {
            int s = slot + 1;
            map.put(s, new Button() {
                @Override public void onClick(InventoryClickEvent e) { e.setCancelled(true); }
                @Override public ItemStack getItemStack() {
                    return new ItemBuilder(ItemUtils.getMat("STAINED_GLASS_PANE"))
                            .data(getInstance().getBlockshopManager(), 15)
                            .setName("&7").toItemStack();
                }
            });
        }

        // Slot 1 -> Apply Default Configuration
        map.put(2, simple(EditorItems.named(getInstance(), "EMERALD", "&a&lApply Default Configuration",
                Arrays.asList("&7", "&eClick to edit...", "&7")), e -> {
            new DefaultConfigMenu(getManager(), player).open();
        }));

        // Slot 2 -> Edit Shop Menu (decorative)
        map.put(3, simple(EditorItems.named(getInstance(), "WORKBENCH", "&6&lEdit Shop Menu",
                Arrays.asList("&7", "&eClick to edit decorative items", "&7")), e -> {
            String def = ed().getDefaultCategory();
            if (def.isEmpty()) {
                player.sendMessage(getLanguageConfig().getString("BLOCKSHOP.NO_DEFAULT_CATEGORY"));
                return;
            }
            new ShopMenuEditor(getManager(), player, def).open();
        }));

        // Slot 4 -> Create Category
        map.put(5, simple(EditorItems.named(getInstance(), "NETHER_STAR", "&a&lCreate Category",
                Arrays.asList("&7", "&eClick to create a new category", "&7")), e -> {
            player.closeInventory();
            player.sendMessage(getLanguageConfig().getString("BLOCKSHOP.EDITOR.INPUT_NAME"));
            ed().awaitChatInput(player, input -> {
                if (input.equalsIgnoreCase("cancel")) {
                    new ShopEditorMenu(getManager(), player).open();
                    return;
                }
                if (ed().categoryExists(input)) {
                    player.sendMessage(getLanguageConfig().getString("BLOCKSHOP.EDITOR.INVALID_NUMBER"));
                    new ShopEditorMenu(getManager(), player).open();
                    return;
                }
                ed().createCategory(input);
                new ShopEditorMenu(getManager(), player).open();
            });
        }));

        // Slot 6 -> Default Shop Category
        String defCat = ed().getDefaultCategory();
        List<String> defLore = new ArrayList<>(Arrays.asList("&7",
                "&6Current: &f" + (defCat.isEmpty() ? "" : defCat), "&7", "&eClick to edit"));
        map.put(7, simple(EditorItems.named(getInstance(), "INK_SACK", 8, "&e&lDefault Shop Category", defLore), e -> {
            new DefaultCategoryPickerMenu(getManager(), player).open();
        }));

        // Slot 7 -> Shops Menu Name
        List<String> nameLore = new ArrayList<>(Arrays.asList("&7",
                "&6Current: &f" + ed().getMenuName(), "&7", "&eClick to edit"));
        map.put(8, simple(EditorItems.named(getInstance(), "HOPPER", "&e&lShops Menu Name", nameLore), e -> {
            player.closeInventory();
            player.sendMessage(getLanguageConfig().getString("BLOCKSHOP.EDITOR.INPUT_VALUE"));
            ed().awaitChatInput(player, input -> {
                ed().setMenuName(input);
                new ShopEditorMenu(getManager(), player).open();
            });
        }));

        // Slots 10 onward - category items
        int idx = 10;
        for (String cat : ed().getCategoryNames()) {
            int s = idx + 1;
            if (s > 45) break;
            map.put(s, categoryButton(cat));
            idx++;
        }

        return map;
    }

    private Button categoryButton(String cat) {
        return new Button() {
            @Override
            public void onClick(InventoryClickEvent e) {
                e.setCancelled(true);
                new CategoryEditMenu(getManager(), player, cat).open();
            }

            @Override
            public ItemStack getItemStack() {
                String mat = ed().getCategoryMaterial(cat);
                ItemStack base = new ItemStack(ItemUtils.getMat(mat));
                ItemBuilder b = new ItemBuilder(base);
                b.setName(ed().getCategoryName(cat));
                List<String> lore = ed().getCategoryLore(cat);
                if (lore != null && !lore.isEmpty()) b.setLore(lore);
                return b.toItemStack();
            }
        };
    }

    private Button simple(ItemStack item, java.util.function.Consumer<InventoryClickEvent> handler) {
        return new Button() {
            @Override
            public void onClick(InventoryClickEvent e) {
                e.setCancelled(true);
                handler.accept(e);
            }

            @Override
            public ItemStack getItemStack() {
                return item;
            }
        };
    }
}
