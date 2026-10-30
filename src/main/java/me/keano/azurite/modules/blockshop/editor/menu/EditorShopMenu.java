package me.keano.azurite.modules.blockshop.editor.menu;

import me.keano.azurite.modules.blockshop.editor.ShopEditorManager;
import me.keano.azurite.modules.framework.menu.Menu;
import me.keano.azurite.modules.framework.menu.MenuManager;
import me.keano.azurite.modules.framework.menu.button.Button;
import me.keano.azurite.modules.users.User;
import me.keano.azurite.utils.ItemBuilder;
import me.keano.azurite.utils.ItemUtils;
import me.keano.azurite.utils.Utils;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The shop menu rendered from editor-managed category data.
 * Shows decorative items, category buttons (to switch categories),
 * and buy/sell items for the active category.
 */
public class EditorShopMenu extends Menu {

    private final String category;
    private final boolean allowBuySellInteract;

    public EditorShopMenu(MenuManager manager, Player player, String category) {
        this(manager, player, category, false);
    }

    public EditorShopMenu(MenuManager manager, Player player, String category, boolean allowBuySellInteract) {
        super(
                manager,
                player,
                manager.getInstance().getShopEditorManager().getMenuName(),
                manager.getInstance().getShopEditorManager().getCategoryRows(category) * 9,
                false
        );
        this.category = category;
        this.allowBuySellInteract = allowBuySellInteract;
        this.allowInteract = allowBuySellInteract;
    }

    private ShopEditorManager ed() {
        return getInstance().getShopEditorManager();
    }

    @Override
    public Map<Integer, Button> getButtons(Player player) {
        Map<Integer, Button> map = new HashMap<>();

        // 1-indexed internally; convert.
        // Decorative items
        for (Map.Entry<Integer, ItemStack> e : ed().getDecorative(category).entrySet()) {
            int slot = e.getKey() + 1;
            if (slot > size) continue;
            map.put(slot, new Button() {
                @Override
                public void onClick(InventoryClickEvent ev) {
                    ev.setCancelled(true);
                }

                @Override
                public ItemStack getItemStack() {
                    return e.getValue().clone();
                }
            });
        }

        // Category buttons (links to other categories). Place only if slot set.
        for (String cat : ed().getCategoryNames()) {
            int slot = ed().getCategorySlot(cat);
            if (slot < 0) continue;
            int s = slot + 1;
            if (s > size) continue;
            if (map.containsKey(s)) continue;
            map.put(s, categoryButton(cat));
        }

        // Buy/sell items
        for (String id : ed().getItemIds(category)) {
            int slot = ed().getItemSlot(category, id);
            int s = slot + 1;
            if (s > size) continue;
            map.put(s, buySellButton(category, id));
        }

        return map;
    }

    private Button categoryButton(String cat) {
        return new Button() {
            @Override
            public void onClick(InventoryClickEvent e) {
                e.setCancelled(true);
                if (allowBuySellInteract) return;
                Player p = (Player) e.getWhoClicked();
                new EditorShopMenu(getManager(), p, cat).open();
            }

            @Override
            public ItemStack getItemStack() {
                return buildCategoryItem(cat);
            }
        };
    }

    public ItemStack buildCategoryItem(String cat) {
        String mat = ed().getCategoryMaterial(cat);
        ItemStack base;
        if (mat.equalsIgnoreCase("CROWBAR")) {
            base = getInstance().getSpawnerManager() == null
                    ? new ItemStack(Material.GRASS) : me.keano.azurite.modules.framework.Config.CROWBAR.clone();
        } else if (mat.contains("_SPAWNER")) {
            me.keano.azurite.modules.spawners.Spawner sp = getInstance().getSpawnerManager().getByName(mat.split("_")[0]);
            base = (sp == null) ? new ItemStack(ItemUtils.getMat("GRASS")) : sp.getItemStack().clone();
        } else {
            base = new ItemStack(ItemUtils.getMat(mat));
        }
        ItemBuilder b = new ItemBuilder(base);
        b.setName(ed().getCategoryName(cat));
        List<String> lore = ed().getCategoryLore(cat);
        if (lore != null && !lore.isEmpty()) b.setLore(lore);
        return b.toItemStack();
    }

    private Button buySellButton(String cat, String id) {
        return new Button() {
            @Override
            public void onClick(InventoryClickEvent e) {
                if (!allowBuySellInteract) {
                    e.setCancelled(true);
                    handleBuySell((Player) e.getWhoClicked(), cat, id, e.getClick());
                }
            }

            @Override
            public ItemStack getItemStack() {
                return ed().buildShopItem(cat, id, player);
            }
        };
    }

    private void handleBuySell(Player p, String cat, String id, ClickType click) {
        User user = getInstance().getUserManager().getByUUID(p.getUniqueId());
        int buy = ed().getBuyPrice(cat, id);
        int sell = ed().getSellPrice(cat, id);
        int qty = ed().getQuantity(cat, id);
        String mat = ed().getItemMaterial(cat, id);
        int data = ed().getItemData(cat, id);

        if (click == ClickType.RIGHT || click == ClickType.SHIFT_RIGHT) {
            // sell
            if (sell < 0) {
                // no sell price -> treat as buy-only; ignore
                return;
            }
            ItemStack proto = new ItemStack(ItemUtils.getMat(mat));
            if (data != 0) getManager().setData(proto, data);
            int amountItems = Utils.getAmountItems(getManager(), p, proto);
            if (amountItems < qty) {
                p.sendMessage(getLanguageConfig().getString("BLOCKSHOP.NOT_CARRYING_SELL"));
                return;
            }
            Utils.takeItems(getManager(), p, proto, qty);
            user.setBalance(user.getBalance() + sell);
            user.save();
            p.sendMessage(getLanguageConfig().getString("BLOCKSHOP.SOLD")
                    .replace("%amount%", String.valueOf(qty))
                    .replace("%price%", String.valueOf(sell))
                    .replace("%type%", ItemUtils.getItemName(proto)));
        } else {
            // buy (left/middle/shift-left default to buy)
            if (buy < 0) return;
            if (user.getBalance() < buy) {
                p.sendMessage(getLanguageConfig().getString("BLOCKSHOP.NO_MONEY_BUY")
                        .replace("%price%", String.valueOf(buy)));
                return;
            }
            user.setBalance(user.getBalance() - buy);
            user.save();
            ItemStack give = new ItemStack(ItemUtils.getMat(mat), Math.max(1, qty));
            if (data != 0) getManager().setData(give, data);
            ItemUtils.giveItem(p, give, p.getLocation());
            p.sendMessage(getLanguageConfig().getString("BLOCKSHOP.BOUGHT_BLOCK")
                    .replace("%price%", String.valueOf(buy))
                    .replace("%type%", ItemUtils.getItemName(give)));
        }
    }
}
