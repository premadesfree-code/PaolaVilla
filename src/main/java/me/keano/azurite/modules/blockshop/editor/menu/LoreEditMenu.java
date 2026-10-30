package me.keano.azurite.modules.blockshop.editor.menu;

import me.keano.azurite.modules.blockshop.editor.ShopEditorManager;
import me.keano.azurite.modules.framework.menu.Menu;
import me.keano.azurite.modules.framework.menu.MenuManager;
import me.keano.azurite.modules.framework.menu.button.Button;
import me.keano.azurite.utils.ItemBuilder;
import me.keano.azurite.utils.ItemUtils;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Dynamic lore editor. Shared by category lore, buy/sell item lore, and
 * default-config lore. The caller provides the current lore list, a save
 * callback, and a reopen callback.
 */
public class LoreEditMenu extends Menu {

    public interface LoreSave {
        void save(List<String> lore);
        void reopen();
    }

    private final List<String> lore;
    private final LoreSave saver;
    private final String pricePlaceholderLine; // optional extra header (e.g. %price% doc)
    private int selected;

    public LoreEditMenu(MenuManager manager, Player player, String title,
                        List<String> lore, LoreSave saver, String pricePlaceholderLine) {
        super(manager, player, title, 27, true);
        this.lore = new ArrayList<>(lore);
        this.saver = saver;
        this.pricePlaceholderLine = pricePlaceholderLine;
        this.selected = 0;
    }

    private ShopEditorManager ed() {
        return getInstance().getShopEditorManager();
    }

    @Override
    public Map<Integer, Button> getButtons(Player player) {
        Map<Integer, Button> map = new HashMap<>();

        for (int i = 0; i < size; i++) {
            int s = i + 1;
            final int slot = i;
            map.put(s, new Button() {
                @Override public void onClick(InventoryClickEvent e) { e.setCancelled(true); }
                @Override public ItemStack getItemStack() {
                    return EditorItems.borderGlassBlack(getInstance());
                }
            });
        }

        // Slot 13 (index 13) holds the lore-edit item
        map.put(14, new Button() {
            @Override
            public void onClick(InventoryClickEvent e) {
                e.setCancelled(true);
                ClickType click = e.getClick();
                if (click == ClickType.SHIFT_LEFT) {
                    // add line
                    player.closeInventory();
                    player.sendMessage(getLanguageConfig().getString("BLOCKSHOP.EDITOR.INPUT_LORE_ADD"));
                    ed().awaitChatInput(player, input -> {
                        lore.add(input);
                        selected = lore.size() - 1;
                        saver.save(lore);
                        reopenSelf();
                    });
                } else if (click == ClickType.SHIFT_RIGHT) {
                    // delete selected
                    if (lore.isEmpty()) return;
                    if (selected >= lore.size()) selected = lore.size() - 1;
                    lore.remove(selected);
                    if (selected > 0) selected--;
                    saver.save(lore);
                    update();
                } else if (click == ClickType.LEFT) {
                    // scroll down
                    if (!lore.isEmpty() && selected < lore.size() - 1) {
                        selected++;
                        update();
                    }
                } else if (click == ClickType.RIGHT) {
                    // scroll up
                    if (selected > 0) {
                        selected--;
                        update();
                    }
                } else if (click == ClickType.MIDDLE) {
                    // edit selected
                    if (lore.isEmpty()) return;
                    if (selected >= lore.size()) selected = lore.size() - 1;
                    final int idx = selected;
                    String current = lore.get(idx);
                    player.closeInventory();
                    player.sendMessage(getLanguageConfig().getString("BLOCKSHOP.EDITOR.INPUT_LORE_EDIT"));
                    ed().awaitChatInput(player, input -> {
                        lore.set(idx, input);
                        saver.save(lore);
                        reopenSelf();
                    });
                }
            }

            @Override
            public ItemStack getItemStack() {
                ItemBuilder b = new ItemBuilder(ItemUtils.getMat("SKULL_ITEM"));
                b.data(getInstance().getBlockshopManager(), 1);
                b.setName("&e&lLore Text");
                List<String> lines = new ArrayList<>();
                lines.add("&7");
                if (pricePlaceholderLine != null) {
                    lines.add("&7&lPlaceholders");
                    lines.add("&7");
                    lines.add(pricePlaceholderLine);
                    lines.add("&7");
                }
                if (lore.isEmpty()) {
                    lines.add("&7(no lines)");
                } else {
                    for (int i = 0; i < lore.size(); i++) {
                        if (i == selected) {
                            lines.add("&a" + i + ". &f" + lore.get(i));
                        } else {
                            lines.add("&7" + i + ". &7" + lore.get(i));
                        }
                    }
                }
                lines.add("&7");
                lines.add("&aShift Left Click to add a new line");
                lines.add("&cShift Right Click to delete a line");
                lines.add("&eLeft and Right to Scroll");
                lines.add("&bMiddle click to edit Text");
                b.setLore(lines);
                return b.toItemStack();
            }
        });

        // Back button
        map.put(23, new Button() {
            @Override
            public void onClick(InventoryClickEvent e) {
                e.setCancelled(true);
                saver.save(lore);
                saver.reopen();
            }

            @Override
            public ItemStack getItemStack() {
                return EditorItems.named(getInstance(), "BED", "&c&lBack", Arrays.asList("&7"));
            }
        });

        return map;
    }

    private void reopenSelf() {
        LoreEditMenu next = new LoreEditMenu(getManager(), player, title, lore, saver, pricePlaceholderLine);
        next.selected = this.selected;
        next.open();
    }
}
