package me.keano.azurite.modules.blockshop.editor.menu;

import me.keano.azurite.modules.blockshop.editor.ShopEditorManager;
import me.keano.azurite.modules.framework.menu.Menu;
import me.keano.azurite.modules.framework.menu.MenuManager;
import me.keano.azurite.modules.framework.menu.button.Button;
import me.keano.azurite.utils.ItemBuilder;
import me.keano.azurite.utils.ItemUtils;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Category Edit Menu (4 rows / 36 slots).
 */
public class CategoryEditMenu extends Menu {

    private final String category;

    public CategoryEditMenu(MenuManager manager, Player player, String category) {
        super(manager, player, "&8Editing - " + category, 27, false);
        this.category = category;
    }

    public String getCategory() {
        return category;
    }

    private ShopEditorManager ed() {
        return getInstance().getShopEditorManager();
    }

    @Override
    public Map<Integer, Button> getButtons(Player player) {
        Map<Integer, Button> map = new HashMap<>();

        for (int i = 0; i < size; i++) {
            int s = i + 1;
            map.put(s, new Button() {
                @Override public void onClick(InventoryClickEvent e) { e.setCancelled(true); }
                @Override public ItemStack getItemStack() {
                    return EditorItems.borderGlassBlack(getInstance());
                }
            });
        }

        // 10 - Edit Category Item
        map.put(11, simple(EditorItems.named(getInstance(), "SIGN", "&e&lEdit Category Item",
                Arrays.asList("&7", "&7Click to edit item", "&7")), e -> {
            player.closeInventory();
            player.sendMessage(getLanguageConfig().getString("BLOCKSHOP.EDITOR.INPUT_ITEM"));
            ed().awaitItemPick(player, item -> {
                ed().setCategoryMaterial(category, item.getType());
                new CategoryEditMenu(getManager(), player, category).open();
            });
        }));

        // 11 - Category Slot
        int slot = ed().getCategorySlot(category);
        String slotStr = slot < 0 ? "&cNone" : "&f" + slot;
        map.put(12, simple(EditorItems.named(getInstance(), "ANVIL", "&e&lCategory Slot",
                Arrays.asList("&7", "&6Current Slot: " + slotStr, "&7", "&eClick to edit")), e -> {
            String def = ed().getDefaultCategory();
            if (def.isEmpty()) {
                player.sendMessage(getLanguageConfig().getString("BLOCKSHOP.NO_DEFAULT_CATEGORY"));
                return;
            }
            new SlotPickerMenu(getManager(), player, category).open();
        }));

        // 12 - Item Name
        map.put(13, simple(EditorItems.named(getInstance(), "NAME_TAG", "&e&lItem Name",
                Arrays.asList("&7", "&6Current Name: " + ed().getCategoryName(category), "&7", "&eClick to edit.")), e -> {
            player.closeInventory();
            player.sendMessage(getLanguageConfig().getString("BLOCKSHOP.EDITOR.INPUT_NAME"));
            ed().awaitChatInput(player, input -> {
                ed().setCategoryName(category, input);
                new CategoryEditMenu(getManager(), player, category).open();
            });
        }));

        // 13 - Lore Text
        map.put(14, loreButton());

        // 14 - Buy/Sell Items Editor
        map.put(15, simple(EditorItems.named(getInstance(), "CHEST", "&e&lBuy/Sell Items Editor",
                new ArrayList<>()), e -> {
            new BuySellItemsEditor(getManager(), player, category).open();
        }));

        // 15 - Edit Menu Category Rows
        map.put(16, simple(EditorItems.named(getInstance(), "PAPER", "&e&lEdit Menu Category Rows",
                Arrays.asList("&7", "&eClick to edit.", "&7")), e -> {
            player.closeInventory();
            player.sendMessage(getLanguageConfig().getString("BLOCKSHOP.EDITOR.INPUT_VALUE_ROWS"));
            ed().awaitChatInput(player, input -> {
                try {
                    int rows = Integer.parseInt(input);
                    if (rows < 1 || rows > 6) {
                        player.sendMessage(getLanguageConfig().getString("BLOCKSHOP.EDITOR.INVALID_ROWS"));
                        new CategoryEditMenu(getManager(), player, category).open();
                        return;
                    }
                    ed().setCategoryRows(category, rows);
                } catch (NumberFormatException ex) {
                    player.sendMessage(getLanguageConfig().getString("BLOCKSHOP.EDITOR.INVALID_ROWS"));
                }
                new CategoryEditMenu(getManager(), player, category).open();
            });
        }));

        // 16 - Buy/Sell Items Configuration
        map.put(17, simple(EditorItems.named(getInstance(), "BOOK", "&e&lBuy/Sell Items Configuration",
                Arrays.asList("&7", "&eClick to edit.", "&7")), e -> {
            new BuySellConfigMenu(getManager(), player, category).open();
        }));

        return map;
    }

    private Button loreButton() {
        return new Button() {
            @Override
            public void onClick(InventoryClickEvent e) {
                e.setCancelled(true);
                new LoreEditMenu(getManager(), player, "&8Editing Category Lore",
                        ed().getCategoryLore(category),
                        new LoreEditMenu.LoreSave() {
                            @Override
                            public void save(List<String> lore) {
                                ed().setCategoryLore(category, lore);
                            }

                            @Override
                            public void reopen() {
                                new CategoryEditMenu(getManager(), player, category).open();
                            }
                        },
                        null).open();
            }

            @Override
            public ItemStack getItemStack() {
                return EditorItems.named(getInstance(), "SKULL_ITEM", 1, "&e&lLore Text", null);
            }
        };
    }

    private Button simple(ItemStack item, Consumer<InventoryClickEvent> handler) {
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
