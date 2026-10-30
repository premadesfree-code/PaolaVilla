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
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Slot-picker mode: renders the default shop menu with cycling colored glass
 * panes in empty slots. Clicking a pane sets the category's slot.
 */
public class SlotPickerMenu extends Menu {

    private final String category;
    private final Set<Integer> occupied = new HashSet<>();
    private BukkitTask cycleTask;
    private int colorIndex = 0;

    public SlotPickerMenu(MenuManager manager, Player player, String category) {
        super(manager, player,
                manager.getInstance().getShopEditorManager().getMenuName(),
                manager.getInstance().getShopEditorManager().getCategoryRows(category) * 9,
                false);
        this.category = category;
    }

    private ShopEditorManager ed() {
        return getInstance().getShopEditorManager();
    }

    @Override
    public Map<Integer, Button> getButtons(Player player) {
        Map<Integer, Button> map = new HashMap<>();

        // Mark occupied slots: decorative, category items, buy/sell items
        for (Map.Entry<Integer, ItemStack> e : ed().getDecorative(category).entrySet()) {
            if (e.getKey() < size) occupied.add(e.getKey());
        }
        for (String cat : ed().getCategoryNames()) {
            int s = ed().getCategorySlot(cat);
            if (s >= 0 && s < size) occupied.add(s);
        }
        for (String id : ed().getItemIds(category)) {
            int s = ed().getItemSlot(category, id);
            if (s >= 0 && s < size) occupied.add(s);
        }

        // Render occupied slots as locked
        for (int slot : occupied) {
            int s = slot + 1;
            map.put(s, new Button() {
                @Override public void onClick(InventoryClickEvent e) { e.setCancelled(true); }
                @Override public ItemStack getItemStack() {
                    return inventory.getItem(slot) != null ? inventory.getItem(slot).clone() :
                            new ItemBuilder(ItemUtils.getMat("STAINED_GLASS_PANE")).data(getInstance().getBlockshopManager(), 15).setName("&7").toItemStack();
                }
            });
        }

        // Render decorative + category + buy/sell items in their slots
        for (Map.Entry<Integer, ItemStack> e : ed().getDecorative(category).entrySet()) {
            if (e.getKey() >= size) continue;
            int s = e.getKey() + 1;
            map.put(s, new Button() {
                @Override public void onClick(InventoryClickEvent ev) { ev.setCancelled(true); }
                @Override public ItemStack getItemStack() { return e.getValue().clone(); }
            });
        }
        for (String cat : ed().getCategoryNames()) {
            int slot = ed().getCategorySlot(cat);
            if (slot < 0 || slot >= size) continue;
            int s = slot + 1;
            final String c = cat;
            map.put(s, new Button() {
                @Override public void onClick(InventoryClickEvent ev) { ev.setCancelled(true); }
                @Override public ItemStack getItemStack() {
                    return new EditorShopMenu(getManager(), player, category).buildCategoryItem(c);
                }
            });
        }
        for (String id : ed().getItemIds(category)) {
            int slot = ed().getItemSlot(category, id);
            if (slot < 0 || slot >= size) continue;
            int s = slot + 1;
            final String itemId = id;
            map.put(s, new Button() {
                @Override public void onClick(InventoryClickEvent ev) { ev.setCancelled(true); }
                @Override public ItemStack getItemStack() { return ed().buildShopItem(category, itemId, player); }
            });
        }

        // Fill empty slots with cycling glass panes
        for (int i = 0; i < size; i++) {
            if (occupied.contains(i)) continue;
            int s = i + 1;
            final int slot = i;
            map.put(s, new Button() {
                @Override
                public void onClick(InventoryClickEvent e) {
                    e.setCancelled(true);
                    ed().setCategorySlot(category, slot);
                    if (cycleTask != null) {
                        cycleTask.cancel();
                        cycleTask = null;
                    }
                    new CategoryEditMenu(getManager(), player, category).open();
                }

                @Override
                public ItemStack getItemStack() {
                    return new ItemBuilder(ItemUtils.getMat("STAINED_GLASS_PANE"))
                            .data(getInstance().getBlockshopManager(), colorIndex)
                            .setName("&aClick to set Slot here!")
                            .toItemStack();
                }
            });
        }

        // Start cycling task
        cycleTask = org.bukkit.Bukkit.getScheduler().runTaskTimer(getInstance(), () -> {
            colorIndex = (colorIndex + 1) % 16;
            for (int i = 0; i < size; i++) {
                if (occupied.contains(i)) continue;
                inventory.setItem(i, new ItemBuilder(ItemUtils.getMat("STAINED_GLASS_PANE"))
                        .data(getInstance().getBlockshopManager(), colorIndex)
                        .setName("&aClick to set Slot here!")
                        .toItemStack());
            }
        }, 20L, 20L);

        return map;
    }

    @Override
    public void onClose() {
        if (cycleTask != null) {
            cycleTask.cancel();
            cycleTask = null;
        }
    }
}
