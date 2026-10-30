package me.keano.azurite.modules.blockshop.editor;

import me.keano.azurite.HCF;
import me.keano.azurite.modules.blockshop.editor.listener.ShopEditorListener;
import me.keano.azurite.modules.framework.Manager;
import me.keano.azurite.utils.BukkitSerialization;
import me.keano.azurite.utils.ItemUtils;
import me.keano.azurite.utils.configs.ConfigYML;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * Copyright (c) 2025. Keano
 * Use or redistribution of source or file is
 * only permitted if given explicit permission.
 */
public class ShopEditorManager extends Manager {

    // Editor-managed categories live under SHOP_EDITOR.CATEGORIES.<name>
    public static final String BASE = "SHOP_EDITOR";
    public static final String CATEGORIES = BASE + ".CATEGORIES";
    public static final String DEFAULT = BASE + ".DEFAULT";
    public static final String PERM = "azurite.shop.editor";

    // Pending chat-input callbacks keyed by player UUID. A null value means
    // the player has no pending text input. When present, the consumer fires
    // on the next AsyncPlayerChatEvent and is then cleared.
    private final Map<UUID, Consumer<String>> chatInputs = new HashMap<>();

    // Pending item-pick callbacks: when a player left-clicks with an item in
    // hand, the callback receives that item stack.
    private final Map<UUID, Consumer<ItemStack>> itemPicks = new HashMap<>();

    public ShopEditorManager(HCF instance) {
        super(instance);
        new ShopEditorListener(this);
    }

    public ConfigYML cfg() {
        return getBlockshopConfig();
    }

    public void save() {
        cfg().save();
    }

    // -- global editor settings --

    public String getMenuName() {
        String s = cfg().getUntranslatedString(BASE + ".MENU_NAME");
        return (s == null || s.isEmpty()) ? "&8Item Shop" : s;
    }

    public void setMenuName(String name) {
        cfg().set(BASE + ".MENU_NAME", name);
        save();
    }

    public String getDefaultCategory() {
        String s = cfg().getUntranslatedString(BASE + ".DEFAULT_CATEGORY");
        return (s == null || s.isEmpty()) ? "" : s;
    }

    public void setDefaultCategory(String name) {
        cfg().set(BASE + ".DEFAULT_CATEGORY", name);
        save();
    }

    // -- default (template) settings applied to new/edited buy-sell items --

    public String getDefaultName() {
        String s = cfg().getUntranslatedString(DEFAULT + ".NAME");
        return (s == null) ? "&f%item_name%" : s;
    }

    public void setDefaultName(String name) {
        cfg().set(DEFAULT + ".NAME", name);
        save();
    }

    public List<String> getDefaultLore() {
        List<String> lore = cfg().getStringList(DEFAULT + ".LORE");
        return lore == null ? new ArrayList<>() : new ArrayList<>(lore);
    }

    public void setDefaultLore(List<String> lore) {
        cfg().set(DEFAULT + ".LORE", lore);
        save();
    }

    public int getDefaultPrice() {
        return cfg().contains(DEFAULT + ".PRICE") ? cfg().getInt(DEFAULT + ".PRICE") : 100;
    }

    public void setDefaultPrice(int price) {
        cfg().set(DEFAULT + ".PRICE", price);
        save();
    }

    public int getDefaultQuantity() {
        return cfg().contains(DEFAULT + ".QUANTITY") ? cfg().getInt(DEFAULT + ".QUANTITY") : 1;
    }

    public void setDefaultQuantity(int qty) {
        cfg().set(DEFAULT + ".QUANTITY", qty);
        save();
    }

    // -- categories --

    /** Returns category keys in file order. */
    public List<String> getCategoryNames() {
        List<String> out = new ArrayList<>();
        if (!cfg().contains(CATEGORIES)) return out;
        for (String key : cfg().getConfigurationSection(CATEGORIES).getKeys(false)) {
            out.add(key);
        }
        return out;
    }

    public boolean categoryExists(String name) {
        return cfg().contains(CATEGORIES + "." + name);
    }

    public String catPath(String name) {
        return CATEGORIES + "." + name;
    }

    public void createCategory(String name) {
        String p = catPath(name);
        cfg().set(p + ".MATERIAL", "GRASS");
        cfg().set(p + ".NAME", "&7&lDefault Item");
        cfg().set(p + ".LORE", new ArrayList<String>());
        cfg().set(p + ".ROWS", 4);
        cfg().set(p + ".SLOT", -1);
        cfg().set(p + ".DECORATIVE", "");
        cfg().set(p + ".ITEMS", "");
        save();
    }

    public String getCategoryMaterial(String name) {
        String s = cfg().getUntranslatedString(catPath(name) + ".MATERIAL");
        return (s == null) ? "GRASS" : s;
    }

    public void setCategoryMaterial(String name, Material mat) {
        cfg().set(catPath(name) + ".MATERIAL", mat.name());
        save();
    }

    public String getCategoryName(String name) {
        String s = cfg().getUntranslatedString(catPath(name) + ".NAME");
        return (s == null) ? "&7&lDefault Item" : s;
    }

    public void setCategoryName(String name, String display) {
        cfg().set(catPath(name) + ".NAME", display);
        save();
    }

    public List<String> getCategoryLore(String name) {
        List<String> lore = cfg().getStringList(catPath(name) + ".LORE");
        return lore == null ? new ArrayList<>() : new ArrayList<>(lore);
    }

    public void setCategoryLore(String name, List<String> lore) {
        cfg().set(catPath(name) + ".LORE", lore);
        save();
    }

    public int getCategoryRows(String name) {
        return cfg().contains(catPath(name) + ".ROWS") ? cfg().getInt(catPath(name) + ".ROWS") : 4;
    }

    public void setCategoryRows(String name, int rows) {
        cfg().set(catPath(name) + ".ROWS", rows);
        save();
    }

    public int getCategorySlot(String name) {
        return cfg().contains(catPath(name) + ".SLOT") ? cfg().getInt(catPath(name) + ".SLOT") : -1;
    }

    public void setCategorySlot(String name, int slot) {
        cfg().set(catPath(name) + ".SLOT", slot);
        save();
    }

    // -- decorative items (stored as base64 item array, slot-indexed map) --

    public Map<Integer, ItemStack> getDecorative(String name) {
        Map<Integer, ItemStack> out = new HashMap<>();
        String data = cfg().getUntranslatedString(catPath(name) + ".DECORATIVE");
        if (data == null || data.isEmpty()) return out;
        try {
            ItemStack[] arr = BukkitSerialization.itemStackArrayFromBase64(data);
            for (int i = 0; i < arr.length; i++) {
                if (arr[i] != null && arr[i].getType() != Material.AIR) {
                    out.put(i, arr[i]);
                }
            }
        } catch (Exception ignored) {
        }
        return out;
    }

    public void setDecorative(String name, Map<Integer, ItemStack> items, int size) {
        ItemStack[] arr = new ItemStack[size];
        for (Map.Entry<Integer, ItemStack> e : items.entrySet()) {
            if (e.getKey() >= 0 && e.getKey() < size) arr[e.getKey()] = e.getValue();
        }
        try {
            cfg().set(catPath(name) + ".DECORATIVE", BukkitSerialization.itemStackArrayToBase64(arr));
        } catch (Exception ex) {
            cfg().set(catPath(name) + ".DECORATIVE", "");
        }
        save();
    }

    // -- buy/sell items --
    // Each item under ITEMS.<id> has: MATERIAL, DATA, NAME, LORE, BUY_PRICE,
    // SELL_PRICE (-1 = none), QUANTITY, REFLECT_QTY (bool), SLOT.

    public List<String> getItemIds(String category) {
        List<String> out = new ArrayList<>();
        String p = catPath(category) + ".ITEMS";
        if (!cfg().contains(p) || cfg().getConfigurationSection(p) == null) return out;
        for (String key : cfg().getConfigurationSection(p).getKeys(false)) {
            out.add(key);
        }
        return out;
    }

    public String itemPath(String category, String id) {
        return catPath(category) + ".ITEMS." + id;
    }

    public String createItemId(String category) {
        AtomicInteger max = new AtomicInteger(0);
        for (String id : getItemIds(category)) {
            try {
                max.set(Math.max(max.get(), Integer.parseInt(id)));
            } catch (NumberFormatException ignored) {
            }
        }
        return String.valueOf(max.get() + 1);
    }

    public String createBuySellItem(String category, int slot, ItemStack item) {
        String id = createItemId(category);
        String p = itemPath(category, id);
        cfg().set(p + ".MATERIAL", item.getType().name());
        int data = getData(item);
        if (data != 0) cfg().set(p + ".DATA", data);
        cfg().set(p + ".NAME", getDefaultName());
        cfg().set(p + ".LORE", getDefaultLore());
        cfg().set(p + ".BUY_PRICE", getDefaultPrice());
        cfg().set(p + ".SELL_PRICE", -1);
        cfg().set(p + ".QUANTITY", getDefaultQuantity());
        cfg().set(p + ".REFLECT_QTY", false);
        cfg().set(p + ".SLOT", slot);
        save();
        return id;
    }

    public void deleteBuySellItem(String category, String id) {
        cfg().set(itemPath(category, id), null);
        save();
    }

    public int getData(ItemStack item) {
        if (item == null) return 0;
        if (me.keano.azurite.utils.Utils.isModernVer()) {
            if (item.getItemMeta() == null) return 0;
            return ((org.bukkit.inventory.meta.Damageable) item.getItemMeta()).getDamage();
        }
        return item.getDurability();
    }

    public String getItemName(String category, String id) {
        String s = cfg().getUntranslatedString(itemPath(category, id) + ".NAME");
        return s == null ? "" : s;
    }

    public void setItemName(String category, String id, String name) {
        cfg().set(itemPath(category, id) + ".NAME", name);
        save();
    }

    public List<String> getItemLore(String category, String id) {
        List<String> lore = cfg().getStringList(itemPath(category, id) + ".LORE");
        return lore == null ? new ArrayList<>() : new ArrayList<>(lore);
    }

    public void setItemLore(String category, String id, List<String> lore) {
        cfg().set(itemPath(category, id) + ".LORE", lore);
        save();
    }

    public int getBuyPrice(String category, String id) {
        return cfg().contains(itemPath(category, id) + ".BUY_PRICE") ? cfg().getInt(itemPath(category, id) + ".BUY_PRICE") : 100;
    }

    public void setBuyPrice(String category, String id, int price) {
        cfg().set(itemPath(category, id) + ".BUY_PRICE", price);
        save();
    }

    public int getSellPrice(String category, String id) {
        return cfg().contains(itemPath(category, id) + ".SELL_PRICE") ? cfg().getInt(itemPath(category, id) + ".SELL_PRICE") : -1;
    }

    public void setSellPrice(String category, String id, int price) {
        cfg().set(itemPath(category, id) + ".SELL_PRICE", price);
        save();
    }

    public int getQuantity(String category, String id) {
        return cfg().contains(itemPath(category, id) + ".QUANTITY") ? cfg().getInt(itemPath(category, id) + ".QUANTITY") : 1;
    }

    public void setQuantity(String category, String id, int qty) {
        cfg().set(itemPath(category, id) + ".QUANTITY", qty);
        save();
    }

    public boolean isReflectQty(String category, String id) {
        return cfg().contains(itemPath(category, id) + ".REFLECT_QTY") && cfg().getBoolean(itemPath(category, id) + ".REFLECT_QTY");
    }

    public void setReflectQty(String category, String id, boolean val) {
        cfg().set(itemPath(category, id) + ".REFLECT_QTY", val);
        save();
    }

    public int getItemSlot(String category, String id) {
        return cfg().contains(itemPath(category, id) + ".SLOT") ? cfg().getInt(itemPath(category, id) + ".SLOT") : 0;
    }

    public String getItemMaterial(String category, String id) {
        return cfg().getUntranslatedString(itemPath(category, id) + ".MATERIAL");
    }

    public int getItemData(String category, String id) {
        if (!cfg().contains(itemPath(category, id) + ".DATA")) return 0;
        return cfg().getInt(itemPath(category, id) + ".DATA");
    }

    /** Count all buy/sell items across every category. */
    public int countAllItems() {
        int total = 0;
        for (String cat : getCategoryNames()) {
            total += getItemIds(cat).size();
        }
        return total;
    }

    /** Build the display item for a buy/sell item, applying templates. */
    public ItemStack buildShopItem(String category, String id, Player player) {
        String p = itemPath(category, id);
        String mat = cfg().getUntranslatedString(p + ".MATERIAL");
        int data = getItemData(category, id);
        int qty = getQuantity(category, id);
        boolean reflect = isReflectQty(category, id);

        ItemStack base;
        if (mat != null && mat.equalsIgnoreCase("CROWBAR")) {
            base = me.keano.azurite.modules.framework.Config.CROWBAR.clone();
        } else if (mat != null && mat.contains("_SPAWNER")) {
            me.keano.azurite.modules.spawners.Spawner sp = getInstance().getSpawnerManager().getByName(mat.split("_")[0]);
            base = (sp == null) ? new ItemStack(Material.STONE) : sp.getItemStack().clone();
        } else {
            base = new ItemStack(ItemUtils.getMat(mat == null ? "STONE" : mat));
        }

        me.keano.azurite.utils.ItemBuilder builder = new me.keano.azurite.utils.ItemBuilder(base);
        if (reflect) builder.setAmount(qty);
        builder.data(this, data);

        // name template
        String nameTpl = cfg().getUntranslatedString(p + ".NAME");
        if (nameTpl == null || nameTpl.isEmpty()) nameTpl = getDefaultName();
        String itemName = ItemUtils.getItemName(builder.toItemStack());
        String resolvedName = nameTpl
                .replace("%item_name%", itemName)
                .replace("%quantity%", String.valueOf(qty));
        builder.setName(resolvedName);

        // lore template
        List<String> loreTpl = cfg().getStringList(p + ".LORE");
        if (loreTpl == null || loreTpl.isEmpty()) loreTpl = getDefaultLore();
        List<String> resolved = new ArrayList<>();
        for (String line : loreTpl) {
            resolved.add(line
                    .replace("%price%", String.valueOf(getBuyPrice(category, id)))
                    .replace("%quantity%", String.valueOf(qty)));
        }
        builder.setLore(resolved);

        builder.setSkullOwner(player.getName());
        return builder.toItemStack();
    }

    /** Apply the default name/lore/price/quantity templates to an item. */
    public void applyDefaultsToItem(String category, String id) {
        String p = itemPath(category, id);
        cfg().set(p + ".NAME", getDefaultName());
        cfg().set(p + ".LORE", getDefaultLore());
        cfg().set(p + ".BUY_PRICE", getDefaultPrice());
        cfg().set(p + ".QUANTITY", getDefaultQuantity());
        save();
    }

    public void applyDefaultsToCategory(String category) {
        for (String id : getItemIds(category)) {
            applyDefaultsToItem(category, id);
        }
    }

    public void applyDefaultsToAll() {
        for (String cat : getCategoryNames()) {
            applyDefaultsToCategory(cat);
        }
    }

    // -- callback registration --

    public void awaitChatInput(Player player, Consumer<String> callback) {
        chatInputs.put(player.getUniqueId(), callback);
    }

    public Consumer<String> takeChatInput(Player player) {
        return chatInputs.remove(player.getUniqueId());
    }

    public boolean hasChatInput(Player player) {
        return chatInputs.containsKey(player.getUniqueId());
    }

    public void clearChatInput(Player player) {
        chatInputs.remove(player.getUniqueId());
    }

    public void awaitItemPick(Player player, Consumer<ItemStack> callback) {
        itemPicks.put(player.getUniqueId(), callback);
    }

    public Consumer<ItemStack> takeItemPick(Player player) {
        return itemPicks.remove(player.getUniqueId());
    }

    public boolean hasItemPick(Player player) {
        return itemPicks.containsKey(player.getUniqueId());
    }

    public void clearItemPick(Player player) {
        itemPicks.remove(player.getUniqueId());
    }
}
