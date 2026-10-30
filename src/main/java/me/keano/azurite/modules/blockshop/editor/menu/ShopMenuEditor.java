package me.keano.azurite.modules.blockshop.editor.menu;

import me.keano.azurite.modules.blockshop.editor.ShopEditorManager;
import me.keano.azurite.modules.framework.menu.Menu;
import me.keano.azurite.modules.framework.menu.MenuManager;
import me.keano.azurite.modules.framework.menu.button.Button;
import me.keano.azurite.utils.ItemUtils;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Decorative-items editor. Renders the shop menu for a category with all its
 * fixed items locked. Empty slots are editable: the player can place items
 * (becoming decorative) or remove existing decorative items. On close the
 * decorative items are saved and the main editor reopens.
 */
public class ShopMenuEditor extends Menu {

    private final String category;
    private final Set<Integer> lockedSlots = new HashSet<>(); // 0-indexed

    public ShopMenuEditor(MenuManager manager, Player player, String category) {
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

        // Category buttons are locked
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

        // Buy/sell items are locked
        for (String id : ed().getItemIds(category)) {
            int slot = ed().getItemSlot(category, id);
            if (slot < 0 || slot >= size) continue;
            lockedSlots.add(slot);
            int s = slot + 1;
            map.put(s, new Button() {
                @Override public void onClick(InventoryClickEvent e) { e.setCancelled(true); }
                @Override public ItemStack getItemStack() {
                    return ed().buildShopItem(category, id, player);
                }
            });
        }

        // Decorative items: locked too (can only be removed via the editor's own
        // handling in onClick/onClickOwn). We render them but prevent drag/move
        // onto them. Removal is allowed via click-own with empty hand.
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

        return map;
    }

    @Override
    public void onClick(InventoryClickEvent e) {
        Player p = (Player) e.getWhoClicked();
        if (e.getClickedInventory() == null) return;

        if (e.getClickedInventory().equals(p.getInventory())) {
            // Allow normal interaction in own inventory so player can pick up items
            // to place into the top inventory. Only block shift-click (handled below).
            if (e.getClick() == org.bukkit.event.inventory.ClickType.SHIFT_LEFT) {
                e.setCancelled(true);
                ItemStack cursor = e.getCurrentItem();
                if (cursor == null || cursor.getType() == Material.AIR) return;
                int empty = firstEmptyTopSlot();
                if (empty < 0) return;
                e.setCurrentItem(null);
                inventory.setItem(empty, cursor.clone());
            }
            return;
        }

        // clicked top inventory
        int slot = e.getRawSlot();
        if (lockedSlots.contains(slot)) {
            e.setCancelled(true);
            return;
        }

        // Empty top slot: allow placing the cursor item as decorative.
        // Don't cancel - let bukkit handle the move.
    }

    @Override
    public void onClickOwn(InventoryClickEvent e) {
        // handled in onClick for own inventory
    }

    @Override
    public void onClose() {
        // Save decorative items from current top-inventory contents.
        Map<Integer, ItemStack> decor = new HashMap<>();
        Set<Integer> nonDecor = new HashSet<>();
        for (String cat : ed().getCategoryNames()) {
            int s = ed().getCategorySlot(cat);
            if (s >= 0 && s < size) nonDecor.add(s);
        }
        for (String id : ed().getItemIds(category)) {
            int s = ed().getItemSlot(category, id);
            if (s >= 0 && s < size) nonDecor.add(s);
        }

        ItemStack[] contents = inventory.getContents();
        for (int i = 0; i < contents.length; i++) {
            if (nonDecor.contains(i)) continue;
            ItemStack it = contents[i];
            if (it == null || it.getType() == Material.AIR) continue;
            decor.put(i, it);
        }
        ed().setDecorative(category, decor, size);

        // Reopen main editor (delayed 1 tick so MenuListener doesn't destroy it)
        org.bukkit.Bukkit.getScheduler().runTask(getInstance(), () -> {
            new ShopEditorMenu(getManager(), player).open();
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
