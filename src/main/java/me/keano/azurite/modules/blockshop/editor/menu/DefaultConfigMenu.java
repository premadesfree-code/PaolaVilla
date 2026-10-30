package me.keano.azurite.modules.blockshop.editor.menu;

import me.keano.azurite.modules.blockshop.editor.ShopEditorManager;
import me.keano.azurite.modules.framework.menu.Menu;
import me.keano.azurite.modules.framework.menu.MenuManager;
import me.keano.azurite.modules.framework.menu.button.Button;
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
 * Default Configuration Menu (3 rows / 27 slots).
 */
public class DefaultConfigMenu extends Menu {

    public DefaultConfigMenu(MenuManager manager, Player player) {
        super(manager, player, "&8Edit Default Configuration", 27, false);
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

        // Slot 10 - Default Name
        String dName = ed().getDefaultName();
        map.put(11, simple(EditorItems.named(getInstance(), "NAME_TAG", "&e&lDefault Name",
                Arrays.asList("&7", "&7&lPlaceholders", "&6%item_name% &fKeep Default Item Name",
                        "&6%quantity% &fThrows the amount set in the item settings",
                        "&7", "&6Current: &f" + dName, "&7", "&eClick to edit")), e -> {
            player.closeInventory();
            player.sendMessage(getLanguageConfig().getString("BLOCKSHOP.EDITOR.INPUT_NAME"));
            ed().awaitChatInput(player, input -> {
                ed().setDefaultName(input);
                new DefaultConfigMenu(getManager(), player).open();
            });
        }));

        // Slot 11 - Lore Text (default lore template with %price% placeholder)
        map.put(12, new Button() {
            @Override
            public void onClick(InventoryClickEvent e) {
                e.setCancelled(true);
                new LoreEditMenu(getManager(), player, "&8Editing Default Lore",
                        ed().getDefaultLore(),
                        new LoreEditMenu.LoreSave() {
                            @Override
                            public void save(List<String> lore) {
                                ed().setDefaultLore(lore);
                            }

                            @Override
                            public void reopen() {
                                new DefaultConfigMenu(getManager(), player).open();
                            }
                        },
                        "&6%price% &fThrows the price of the item").open();
            }

            @Override
            public ItemStack getItemStack() {
                return EditorItems.named(getInstance(), "SKULL_ITEM", 1, "&e&lLore Text", null);
            }
        });

        // Slot 12 - Default Buy/Sell Price
        int dPrice = ed().getDefaultPrice();
        map.put(13, simple(EditorItems.named(getInstance(), "GOLD_INGOT", "&e&lDefault Buy/Sell Price",
                Arrays.asList("&7", "&6Current: &f" + dPrice, "&7", "&eClick to edit.")), e -> {
            player.closeInventory();
            player.sendMessage(getLanguageConfig().getString("BLOCKSHOP.EDITOR.INPUT_PRICE"));
            ed().awaitChatInput(player, input -> {
                try {
                    int val = Integer.parseInt(input);
                    ed().setDefaultPrice(val);
                } catch (NumberFormatException ex) {
                    player.sendMessage(getLanguageConfig().getString("BLOCKSHOP.EDITOR.INVALID_NUMBER"));
                }
                new DefaultConfigMenu(getManager(), player).open();
            });
        }));

        // Slot 13 - Default Quantity
        int dQty = ed().getDefaultQuantity();
        map.put(14, simple(EditorItems.named(getInstance(), "ANVIL", "&e&lDefault Quantity",
                Arrays.asList("&7", "&6Current: " + dQty, "&7", "&eClick to edit")), e -> {
            player.closeInventory();
            player.sendMessage(getLanguageConfig().getString("BLOCKSHOP.EDITOR.INPUT_VALUE_QTY"));
            ed().awaitChatInput(player, input -> {
                try {
                    int val = Integer.parseInt(input);
                    if (val < 1 || val > 64) {
                        player.sendMessage(getLanguageConfig().getString("BLOCKSHOP.EDITOR.INVALID_QTY"));
                    } else {
                        ed().setDefaultQuantity(val);
                    }
                } catch (NumberFormatException ex) {
                    player.sendMessage(getLanguageConfig().getString("BLOCKSHOP.EDITOR.INVALID_QTY"));
                }
                new DefaultConfigMenu(getManager(), player).open();
            });
        }));

        // Slot 14 - Apply Default Configuration
        map.put(15, simple(EditorItems.named(getInstance(), "EMERALD", "&a&lApply Default Configuration",
                Arrays.asList("&7", "&eClick to edit", "&7")), e -> {
            new ApplyConfigMenu(getManager(), player).open();
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
