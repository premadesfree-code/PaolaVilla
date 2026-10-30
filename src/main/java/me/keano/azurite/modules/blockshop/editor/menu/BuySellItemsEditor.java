package me.keano.azurite.modules.blockshop.editor.menu;

import me.keano.azurite.modules.blockshop.editor.ShopEditorManager;
import me.keano.azurite.modules.framework.menu.Menu;
import me.keano.azurite.modules.framework.menu.MenuManager;
import me.keano.azurite.modules.framework.menu.button.Button;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Buy/sell items editor. Renders the category's shop menu with decorative +
 * category items locked. Empty slots accept new items (become buy/sell items
 * at default price). Existing buy/sell items can be removed. On close, changes
 * are saved and the Category Edit Menu reopens.
 */
public class BuySellItemsEditor extends Menu {

    private final String category;
    private final Set<Integer> lockedSlots = new HashSet<>(); // 0-indexed

    public BuySellItemsEditor(MenuManager manager, Player player, String category) {
        super(manager, player,
                manager.getInstance().getShopEditorManager().getMenuName(),
                manager.getInstance().getShopEditorManager().getCategoryRows(category) * 9,
                false);
        this.category = category;
        this.allowInteract = true;
    }

    private ShopEditorManager ed() {
        return getInstance().getShopEditorManager();
    }

    @Override
    public Map<Integer, Button> getButtons(Player player) {
        Map<Integer, Button> map = new HashMap<>();

        // Decorative - locked
        for (Map.Entry<Integer, ItemStack> e : ed().getDecorative(category).entrySet()) {
            int slot = e.getKey();
            if (slot >= size) continue;
            lockedSlots.add(slot);
            int s = slot + 1;
            map.put(s, new Button() {
                @Override public void onClick(InventoryClickEvent e) { e.setCancelled(true); }
                @Override public ItemStack getItemStack() { return e.getValue().clone(); }
            });
        }

        // Category buttons - locked, no switching
        for (String cat : ed().getCategoryNames()) {
            int slot = ed().getCategorySlot(cat);
            if (slot < 0 || slot >= size) continue;
            lockedSlots.add(slot);
            int s = slot + 1;
            map.put(s, new Button() {
                @Override public void onClick(InventoryClickEvent e) { e.setCancelled(true); }
                @Override public ItemStack getItemStack() {
                    return new EditorShopMenu(getManager(), player, category).buildCategoryItem(cat);
                }
            });
        }

        // Existing buy/sell items - editable/removable. We render them but
        // handle clicks ourselves.
        for (String id : ed().getItemIds(category)) {
            int slot = ed().getItemSlot(category, id);
            if (slot < 0 || slot >= size) continue;
            int s = slot + 1;
            final String itemId = id;
            map.put(s, new Button() {
                @Override
                public void onClick(InventoryClickEvent e) {
                    // Allow removal: shift-click with empty cursor removes the item.
                    if (e.getClick() == ClickType.SHIFT_LEFT || e.getClick() == ClickType.SHIFT_RIGHT) {
                        e.setCancelled(true);
                        ed().deleteBuySellItem(category, itemId);
                        inventory.setItem(slot, null);
                        lockedSlots.remove(slot);
                        update();
                        return;
                    }
                    e.setCancelled(true);
                }

                @Override
                public ItemStack getItemStack() {
                    return ed().buildShopItem(category, itemId, player);
                }
            });
        }

        return map;
    }

    @Override
    public void onClick(InventoryClickEvent e) {
        Player p = (Player) e.getWhoClicked();
        if (e.getClickedInventory() == null) return;

        if (e.getClickedInventory().equals(p.getInventory())) {
            if (e.getClick() == ClickType.SHIFT_LEFT) {
                e.setCancelled(true);
                ItemStack cursor = e.getCurrentItem();
                if (cursor == null || cursor.getType() == Material.AIR) return;
                int empty = firstEmptyTopSlot();
                if (empty < 0) return;
                ed().createBuySellItem(category, empty, cursor.clone());
                e.setCurrentItem(null);
                update();
            }
            return;
        }

        int slot = e.getRawSlot();
        if (lockedSlots.contains(slot)) {
            return;
        }

        ItemStack cursor = e.getCursor();
        if (cursor != null && cursor.getType() != Material.AIR) {
            e.setCancelled(true);
            String newId = ed().createBuySellItem(category, slot, cursor.clone());
            inventory.setItem(slot, ed().buildShopItem(category, newId, p));
            cursor.setAmount(cursor.getAmount() - 1);
            e.setCursor(cursor.getAmount() <= 0 ? null : cursor);
            update();
        } else {
            e.setCancelled(true);
        }
    }

    @Override
    public void onClose() {
        org.bukkit.Bukkit.getScheduler().runTask(getInstance(), () -> {
            new CategoryEditMenu(getManager(), player, category).open();
        });
    }

    private int firstEmptyTopSlot() {
        ItemStack[] contents = inventory.getContents();
        for (int i = 0; i < contents.length; i++) {
            if (lockedSlots.contains(i)) continue;
            ItemStack it = contents[i];
            if (it == null || it.getType() == Material.AIR) return i;
        }
        return -1;
    }
}
