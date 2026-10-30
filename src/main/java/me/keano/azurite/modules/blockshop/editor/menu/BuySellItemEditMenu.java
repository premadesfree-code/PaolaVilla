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
 * Buy/Sell Item Edit Menu (3 rows / 27 slots).
 */
public class BuySellItemEditMenu extends Menu {

    private final String category;
    private final String itemId;

    public BuySellItemEditMenu(MenuManager manager, Player player, String category, String itemId) {
        super(manager, player, "&8Editing Item - " + itemId, 27, false);
        this.category = category;
        this.itemId = itemId;
    }

    private ShopEditorManager ed() {
        return getInstance().getShopEditorManager();
    }

    @Override
    public Map<Integer, Button> getButtons(Player player) {
        Map<Integer, Button> map = new HashMap<>();

        // Fill with black glass
        for (int i = 0; i < size; i++) {
            int s = i + 1;
            map.put(s, new Button() {
                @Override public void onClick(InventoryClickEvent e) { e.setCancelled(true); }
                @Override public ItemStack getItemStack() {
                    return EditorItems.borderGlassBlack(getInstance());
                }
            });
        }

        // Slot 10 - Edit Item Name
        String name = ed().getItemName(category, itemId);
        if (name == null || name.isEmpty()) name = "&fDefault Name";
        map.put(11, simple(EditorItems.named(getInstance(), "NAME_TAG", "&e&lEdit Item Name",
                Arrays.asList("&7", "&6Current: &f" + name, "&7", "&eClick to edit")), e -> {
            player.closeInventory();
            player.sendMessage(getLanguageConfig().getString("BLOCKSHOP.EDITOR.INPUT_NAME"));
            ed().awaitChatInput(player, input -> {
                ed().setItemName(category, itemId, input);
                new BuySellItemEditMenu(getManager(), player, category, itemId).open();
            });
        }));

        // Slot 11 - Lore Text (with %price% placeholder doc)
        map.put(12, new Button() {
            @Override
            public void onClick(InventoryClickEvent e) {
                e.setCancelled(true);
                new LoreEditMenu(getManager(), player, "&8Editing Item Lore",
                        ed().getItemLore(category, itemId),
                        new LoreEditMenu.LoreSave() {
                            @Override
                            public void save(List<String> lore) {
                                ed().setItemLore(category, itemId, lore);
                            }

                            @Override
                            public void reopen() {
                                new BuySellItemEditMenu(getManager(), player, category, itemId).open();
                            }
                        },
                        "&6%price% &fThrows the price of the item").open();
            }

            @Override
            public ItemStack getItemStack() {
                return EditorItems.named(getInstance(), "SKULL_ITEM", 1, "&e&lLore Text", null);
            }
        });

        // Slot 12 - Price
        int buy = ed().getBuyPrice(category, itemId);
        int sell = ed().getSellPrice(category, itemId);
        String mode = sell >= 0 ? "&aSell" : "&cBuy";
        map.put(13, simple(EditorItems.named(getInstance(), "EXP_BOTTLE", "&e&lPrice",
                Arrays.asList("&7", "&6Current: &a" + buy, "&6Mode: " + mode, "&7",
                        "&aLeft Click to edit buy price.", "&cRight Click to edit sell price.")), e -> {
            if (e.getClick() == org.bukkit.event.inventory.ClickType.RIGHT) {
                player.closeInventory();
                player.sendMessage(getLanguageConfig().getString("BLOCKSHOP.EDITOR.INPUT_PRICE"));
                ed().awaitChatInput(player, input -> {
                    try {
                        int val = Integer.parseInt(input);
                        ed().setSellPrice(category, itemId, val);
                    } catch (NumberFormatException ex) {
                        player.sendMessage(getLanguageConfig().getString("BLOCKSHOP.EDITOR.INVALID_NUMBER"));
                    }
                    new BuySellItemEditMenu(getManager(), player, category, itemId).open();
                });
            } else {
                player.closeInventory();
                player.sendMessage(getLanguageConfig().getString("BLOCKSHOP.EDITOR.INPUT_PRICE"));
                ed().awaitChatInput(player, input -> {
                    try {
                        int val = Integer.parseInt(input);
                        ed().setBuyPrice(category, itemId, val);
                    } catch (NumberFormatException ex) {
                        player.sendMessage(getLanguageConfig().getString("BLOCKSHOP.EDITOR.INVALID_NUMBER"));
                    }
                    new BuySellItemEditMenu(getManager(), player, category, itemId).open();
                });
            }
        }));

        // Slot 13 - Quantity
        int qty = ed().getQuantity(category, itemId);
        map.put(14, simple(EditorItems.named(getInstance(), "HOPPER", "&e&lQuantity",
                Arrays.asList("&7", "&eCurrent: &f" + qty, "&7", "&eClick to edit")), e -> {
            player.closeInventory();
            player.sendMessage(getLanguageConfig().getString("BLOCKSHOP.EDITOR.INPUT_VALUE_QTY"));
            ed().awaitChatInput(player, input -> {
                try {
                    int val = Integer.parseInt(input);
                    if (val < 1 || val > 64) {
                        player.sendMessage(getLanguageConfig().getString("BLOCKSHOP.EDITOR.INVALID_QTY"));
                    } else {
                        ed().setQuantity(category, itemId, val);
                    }
                } catch (NumberFormatException ex) {
                    player.sendMessage(getLanguageConfig().getString("BLOCKSHOP.EDITOR.INVALID_QTY"));
                }
                new BuySellItemEditMenu(getManager(), player, category, itemId).open();
            });
        }));

        // Slot 14 - Reflect Quantity toggle
        boolean reflect = ed().isReflectQty(category, itemId);
        String reflectStr = reflect ? "&aYes" : "&cNo";
        map.put(15, simple(EditorItems.named(getInstance(), "HOPPER_MINECART", "&e&lReflect Quantity in the item",
                Arrays.asList("&7", "&eCurrent: " + reflectStr, "&7", "&eClick to edit")), e -> {
            ed().setReflectQty(category, itemId, !reflect);
            update();
        }));

        // Slot 15 - Back
        map.put(16, simple(EditorItems.named(getInstance(), "BED", "&c&lBack",
                Arrays.asList("&7")), e -> {
            new BuySellConfigMenu(getManager(), player, category).open();
        }));

        return map;
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
