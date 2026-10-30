package me.keano.azurite.modules.kiteditor;

import lombok.Getter;
import me.keano.azurite.HCF;
import me.keano.azurite.utils.CC;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Copyright (c) 2025. Keano
 * Use or redistribution of source or file is
 * only permitted if given explicit permission.
 */
@Getter
public class KitEditorFileManager {

    private final HCF instance;
    private final File folder;
    private final File menusFile;
    private final File playersFolder;
    private YamlConfiguration menusConfig;

    public KitEditorFileManager(HCF instance) {
        this.instance = instance;
        this.folder = new File(instance.getDataFolder(), "KitEditor");
        this.menusFile = new File(folder, "menus.yml");
        this.playersFolder = new File(folder, "players");
    }

    public void load() {
        if (!folder.exists()) folder.mkdirs();
        if (!playersFolder.exists()) playersFolder.mkdirs();

        if (!menusFile.exists()) {
            try {
                menusFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        menusConfig = YamlConfiguration.loadConfiguration(menusFile);

        if (!menusConfig.contains("kiteditor")) {
            menusConfig.set("kiteditor.enabled", true);
            menusConfig.set("kiteditor.title", "&8Kit Editor");
            menusConfig.set("kiteditor.rows", 4);
            saveMenus();
        }
    }

    public void saveMenus() {
        try {
            menusConfig.save(menusFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void reloadMenus() {
        menusConfig = YamlConfiguration.loadConfiguration(menusFile);
    }

    public boolean isEnabled() {
        return menusConfig.getBoolean("kiteditor.enabled", true);
    }

    public void setEnabled(boolean enabled) {
        menusConfig.set("kiteditor.enabled", enabled);
        saveMenus();
    }

    public String getTitle() {
        return menusConfig.getString("kiteditor.title", "&8Kit Editor");
    }

    public void setTitle(String title) {
        menusConfig.set("kiteditor.title", title);
        saveMenus();
    }

    public int getRows() {
        return menusConfig.getInt("kiteditor.rows", 4);
    }

    public void setRows(int rows) {
        menusConfig.set("kiteditor.rows", rows);
        saveMenus();
    }

    // --- Global decorative items ---

    public Map<Integer, ItemStack> getGlobalDecorativeItems() {
        return loadItems("kiteditor.decorative_items");
    }

    public void setGlobalDecorativeItems(Map<Integer, ItemStack> items) {
        saveItems("kiteditor.decorative_items", items);
    }

    // --- Per-kit config ---

    public boolean hasKitConfig(String kitName) {
        return menusConfig.contains("kiteditor.kits." + kitName);
    }

    public void createDefaultKitConfig(String kitName) {
        String base = "kiteditor.kits." + kitName;
        if (!menusConfig.contains(base + ".display_slot")) {
            menusConfig.set(base + ".display_slot", -1);
        }
        if (!menusConfig.contains(base + ".item_name")) {
            menusConfig.set(base + ".item_name", "&f" + kitName);
        }
        if (!menusConfig.contains(base + ".item_lore")) {
            menusConfig.set(base + ".item_lore", new ArrayList<>());
        }
        if (!menusConfig.contains(base + ".menu_title")) {
            menusConfig.set(base + ".menu_title", "&8Editing " + kitName);
        }
        if (!menusConfig.contains(base + ".menu_rows")) {
            menusConfig.set(base + ".menu_rows", 4);
        }
        if (!menusConfig.contains(base + ".armor_slots.helmet")) {
            menusConfig.set(base + ".armor_slots.helmet", -1);
        }
        if (!menusConfig.contains(base + ".armor_slots.chestplate")) {
            menusConfig.set(base + ".armor_slots.chestplate", -1);
        }
        if (!menusConfig.contains(base + ".armor_slots.leggings")) {
            menusConfig.set(base + ".armor_slots.leggings", -1);
        }
        if (!menusConfig.contains(base + ".armor_slots.boots")) {
            menusConfig.set(base + ".armor_slots.boots", -1);
        }
        if (!menusConfig.contains(base + ".decorative_items")) {
            menusConfig.createSection(base + ".decorative_items");
        }
        if (!menusConfig.contains(base + ".refill_items")) {
            menusConfig.createSection(base + ".refill_items");
        }
        if (!menusConfig.contains(base + ".action_items.save_slot")) {
            menusConfig.set(base + ".action_items.save_slot", -1);
        }
        if (!menusConfig.contains(base + ".action_items.cancel_slot")) {
            menusConfig.set(base + ".action_items.cancel_slot", -1);
        }
        if (!menusConfig.contains(base + ".action_items.copy_slot")) {
            menusConfig.set(base + ".action_items.copy_slot", -1);
        }
        saveMenus();
    }

    public void removeKitConfig(String kitName) {
        menusConfig.set("kiteditor.kits." + kitName, null);
        saveMenus();
    }

    public int getKitDisplaySlot(String kitName) {
        return menusConfig.getInt("kiteditor.kits." + kitName + ".display_slot", -1);
    }

    public void setKitDisplaySlot(String kitName, int slot) {
        menusConfig.set("kiteditor.kits." + kitName + ".display_slot", slot);
        saveMenus();
    }

    public String getKitItemName(String kitName) {
        return menusConfig.getString("kiteditor.kits." + kitName + ".item_name", "&f" + kitName);
    }

    public void setKitItemName(String kitName, String name) {
        menusConfig.set("kiteditor.kits." + kitName + ".item_name", name);
        saveMenus();
    }

    public List<String> getKitItemLore(String kitName) {
        List<String> lore = menusConfig.getStringList("kiteditor.kits." + kitName + ".item_lore");
        return lore != null ? lore : new ArrayList<>();
    }

    public void setKitItemLore(String kitName, List<String> lore) {
        menusConfig.set("kiteditor.kits." + kitName + ".item_lore", lore);
        saveMenus();
    }

    public String getKitMenuTitle(String kitName) {
        return menusConfig.getString("kiteditor.kits." + kitName + ".menu_title", "&8Editing " + kitName);
    }

    public void setKitMenuTitle(String kitName, String title) {
        menusConfig.set("kiteditor.kits." + kitName + ".menu_title", title);
        saveMenus();
    }

    public int getKitMenuRows(String kitName) {
        return menusConfig.getInt("kiteditor.kits." + kitName + ".menu_rows", 4);
    }

    public void setKitMenuRows(String kitName, int rows) {
        menusConfig.set("kiteditor.kits." + kitName + ".menu_rows", rows);
        saveMenus();
    }

    public int getArmorSlot(String kitName, String piece) {
        return menusConfig.getInt("kiteditor.kits." + kitName + ".armor_slots." + piece, -1);
    }

    public void setArmorSlot(String kitName, String piece, int slot) {
        menusConfig.set("kiteditor.kits." + kitName + ".armor_slots." + piece, slot);
        saveMenus();
    }

    public Map<Integer, ItemStack> getKitDecorativeItems(String kitName) {
        return loadItems("kiteditor.kits." + kitName + ".decorative_items");
    }

    public void setKitDecorativeItems(String kitName, Map<Integer, ItemStack> items) {
        saveItems("kiteditor.kits." + kitName + ".decorative_items", items);
    }

    // --- Refill items ---

    public Map<Integer, ItemStack> getRefillItems(String kitName) {
        String base = "kiteditor.kits." + kitName + ".refill_items";
        Map<Integer, ItemStack> map = new HashMap<>();
        ConfigurationSection section = menusConfig.getConfigurationSection(base);
        if (section == null) return map;

        for (String key : section.getKeys(false)) {
            try {
                int slot = Integer.parseInt(key);
                String path = base + "." + key;
                ItemStack item = loadItemFromPath(path);
                if (item != null) {
                    map.put(slot, item);
                }
            } catch (NumberFormatException ignored) {
            }
        }
        return map;
    }

    public String getRefillType(String kitName, int slot) {
        return menusConfig.getString("kiteditor.kits." + kitName + ".refill_items." + slot + ".type", "fill_exact");
    }

    public void setRefillItems(String kitName, Map<Integer, ItemStack> items) {
        String base = "kiteditor.kits." + kitName + ".refill_items";
        menusConfig.set(base, null);
        for (Map.Entry<Integer, ItemStack> entry : items.entrySet()) {
            String path = base + "." + entry.getKey();
            saveItemToPath(path, entry.getValue());
        }
        saveMenus();
    }

    public void setRefillItem(String kitName, int slot, ItemStack item, String type) {
        String path = "kiteditor.kits." + kitName + ".refill_items." + slot;
        saveItemToPath(path, item);
        menusConfig.set(path + ".type", type);
        saveMenus();
    }

    // --- Action items ---

    public int getActionSlot(String kitName, String action) {
        return menusConfig.getInt("kiteditor.kits." + kitName + ".action_items." + action + "_slot", -1);
    }

    public void setActionSlot(String kitName, String action, int slot) {
        menusConfig.set("kiteditor.kits." + kitName + ".action_items." + action + "_slot", slot);
        saveMenus();
    }

    public ItemStack getActionItem(String kitName, String action) {
        return loadItemFromPath("kiteditor.kits." + kitName + ".action_items." + action + "_item");
    }

    public void setActionItem(String kitName, String action, ItemStack item) {
        saveItemToPath("kiteditor.kits." + kitName + ".action_items." + action + "_item", item);
        saveMenus();
    }

    // --- Player personal kits ---

    public boolean hasPersonalKit(UUID uuid, String kitName) {
        File file = new File(playersFolder, uuid.toString() + ".yml");
        if (!file.exists()) return false;
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        return config.contains("kits." + kitName);
    }

    public ItemStack[] getPersonalKitContents(UUID uuid, String kitName) {
        File file = new File(playersFolder, uuid.toString() + ".yml");
        if (!file.exists()) return null;
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        if (!config.contains("kits." + kitName)) return null;

        ItemStack[] contents = new ItemStack[36];
        for (int i = 0; i < 36; i++) {
            contents[i] = loadItemFromPathConfig(config, "kits." + kitName + ".contents." + i);
        }
        return contents;
    }

    public ItemStack[] getPersonalKitArmor(UUID uuid, String kitName) {
        File file = new File(playersFolder, uuid.toString() + ".yml");
        if (!file.exists()) return null;
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        if (!config.contains("kits." + kitName)) return null;

        ItemStack[] armor = new ItemStack[4];
        for (int i = 0; i < 4; i++) {
            armor[i] = loadItemFromPathConfig(config, "kits." + kitName + ".armor." + i);
        }
        return armor;
    }

    public void savePersonalKit(UUID uuid, String kitName, ItemStack[] contents, ItemStack[] armor) {
        File file = new File(playersFolder, uuid.toString() + ".yml");
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

        String base = "kits." + kitName;
        config.set(base + ".contents", null);
        config.set(base + ".armor", null);

        for (int i = 0; i < 36; i++) {
            if (contents[i] == null || contents[i].getType() == Material.AIR) continue;
            saveItemToPathConfig(config, base + ".contents." + i, contents[i]);
        }

        for (int i = 0; i < 4; i++) {
            if (armor[i] == null || armor[i].getType() == Material.AIR) continue;
            saveItemToPathConfig(config, base + ".armor." + i, armor[i]);
        }

        try {
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void removePersonalKit(UUID uuid, String kitName) {
        File file = new File(playersFolder, uuid.toString() + ".yml");
        if (!file.exists()) return;
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        config.set("kits." + kitName, null);
        try {
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // --- Item serialization helpers ---

    private Map<Integer, ItemStack> loadItems(String path) {
        Map<Integer, ItemStack> map = new HashMap<>();
        ConfigurationSection section = menusConfig.getConfigurationSection(path);
        if (section == null) return map;

        for (String key : section.getKeys(false)) {
            try {
                int slot = Integer.parseInt(key);
                ItemStack item = loadItemFromPath(path + "." + key);
                if (item != null) {
                    map.put(slot, item);
                }
            } catch (NumberFormatException ignored) {
            }
        }
        return map;
    }

    private void saveItems(String path, Map<Integer, ItemStack> items) {
        menusConfig.set(path, null);
        for (Map.Entry<Integer, ItemStack> entry : items.entrySet()) {
            saveItemToPath(path + "." + entry.getKey(), entry.getValue());
        }
        saveMenus();
    }

    private ItemStack loadItemFromPath(String path) {
        return loadItemFromPathConfig(menusConfig, path);
    }

    @SuppressWarnings("deprecation")
    private ItemStack loadItemFromPathConfig(YamlConfiguration config, String path) {
        if (!config.contains(path) || !config.contains(path + ".material")) return null;

        String materialName = config.getString(path + ".material");
        Material material = Material.matchMaterial(materialName);
        if (material == null) return null;

        int amount = config.getInt(path + ".amount", 1);
        int data = config.getInt(path + ".data", 0);

        ItemStack item = new ItemStack(material, amount, (short) data);

        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String name = config.getString(path + ".name");
            if (name != null && !name.isEmpty()) {
                meta.setDisplayName(CC.t(name));
            }

            List<String> lore = config.getStringList(path + ".lore");
            if (lore != null && !lore.isEmpty()) {
                List<String> translated = new ArrayList<>();
                for (String line : lore) {
                    translated.add(CC.t(line));
                }
                meta.setLore(translated);
            }

            item.setItemMeta(meta);
        }

        ConfigurationSection enchSection = config.getConfigurationSection(path + ".enchantments");
        if (enchSection != null) {
            for (String enchKey : enchSection.getKeys(false)) {
                Enchantment ench = Enchantment.getByName(enchKey);
                if (ench != null) {
                    int level = enchSection.getInt(enchKey);
                    item.addUnsafeEnchantment(ench, level);
                }
            }
        }

        return item;
    }

    private void saveItemToPath(String path, ItemStack item) {
        saveItemToPathConfig(menusConfig, path, item);
    }

    @SuppressWarnings("deprecation")
    private void saveItemToPathConfig(YamlConfiguration config, String path, ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            config.set(path, null);
            return;
        }

        config.set(path + ".material", item.getType().name());
        config.set(path + ".amount", item.getAmount());
        config.set(path + ".data", (int) item.getDurability());

        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            if (meta.hasDisplayName()) {
                config.set(path + ".name", meta.getDisplayName());
            } else {
                config.set(path + ".name", "");
            }

            if (meta.hasLore()) {
                config.set(path + ".lore", meta.getLore());
            } else {
                config.set(path + ".lore", new ArrayList<>());
            }
        }

        if (item.getEnchantments() != null && !item.getEnchantments().isEmpty()) {
            for (Map.Entry<Enchantment, Integer> entry : item.getEnchantments().entrySet()) {
                config.set(path + ".enchantments." + entry.getKey().getName(), entry.getValue());
            }
        }
    }
}
