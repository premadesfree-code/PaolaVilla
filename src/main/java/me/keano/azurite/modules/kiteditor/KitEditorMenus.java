package me.keano.azurite.modules.kiteditor;

import me.keano.azurite.modules.kits.Kit;
import me.keano.azurite.utils.CC;
import me.keano.azurite.utils.ItemBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

/**
 * Copyright (c) 2025. Keano
 * Use or redistribution of source or file is
 * only permitted if given explicit permission.
 */
public class KitEditorMenus {

    // --- MAIN MENU (player-facing) ---

    public static void openMainMenu(KitEditorManager manager, Player player) {
        if (!manager.getFileManager().isEnabled()) {
            player.sendMessage(CC.t("&cKitEditor Currently Disabled"));
            return;
        }

        String title = manager.getFileManager().getTitle();
        int rows = manager.getFileManager().getRows();
        int size = rows * 9;

        Inventory inventory = Bukkit.createInventory(null, size, CC.t(title));

        // Place decorative items
        Map<Integer, ItemStack> decorativeItems = manager.getFileManager().getGlobalDecorativeItems();
        for (Map.Entry<Integer, ItemStack> entry : decorativeItems.entrySet()) {
            if (entry.getKey() >= 0 && entry.getKey() < size) {
                inventory.setItem(entry.getKey(), entry.getValue().clone());
            }
        }

        // Place kit display items
        for (Kit kit : manager.getEditableKits()) {
            int displaySlot = manager.getFileManager().getKitDisplaySlot(kit.getName());
            if (displaySlot >= 0 && displaySlot < size) {
                inventory.setItem(displaySlot, manager.createKitDisplayItem(kit.getName()));
            }
        }

        manager.setContext(player, KitEditorContext.MAIN_MENU);
        manager.setOpenInventory(player, inventory);
        player.openInventory(inventory);
    }

    private static Material resolveMaterial(String modernName, String legacyName) {
        try {
            return Material.valueOf(modernName);
        } catch (IllegalArgumentException e) {
            return Material.valueOf(legacyName);
        }
    }

    // --- ADMIN MENU ---

    public static void openAdminMenu(KitEditorManager manager, Player player) {
        int size = 45; // 5 rows
        Inventory inventory = Bukkit.createInventory(null, size, CC.t("&8Editing Kit Editor"));

        // Fill pane slots
        Set<Integer> paneSlots = manager.getAdminPaneSlots();
        ItemStack pane = manager.createBlackPane();
        for (int slot : paneSlots) {
            inventory.setItem(slot, pane);
        }

        // Slot 2 - Edit Title
        ItemBuilder titleItem = new ItemBuilder(Material.NAME_TAG);
        titleItem.setName("&e&lEdit Kit Editor Menu Title");
        titleItem.setLore(
                "&7",
                "&6Current: " + manager.getFileManager().getTitle(),
                "&7",
                "&eClick to edit"
        );
        inventory.setItem(2, titleItem.toItemStack());

        // Slot 3 - Edit Rows
        ItemBuilder rowsItem = new ItemBuilder(Material.ANVIL);
        rowsItem.setName("&e&lEdit Menu Rows");
        rowsItem.setLore(
                "&7",
                "&6Current: &f" + manager.getFileManager().getRows(),
                "&7",
                "&eClick to edit."
        );
        inventory.setItem(3, rowsItem.toItemStack());

        // Slot 4 - Edit Decorative Items
        ItemBuilder decorItem = new ItemBuilder(resolveMaterial("PLAYER_HEAD", "SKULL_ITEM"), 1, (byte) 1);
        decorItem.setName("&e&lEdit Decorative Items");
        decorItem.setLore("&7", "&eClick to edit", "&7");
        inventory.setItem(4, decorItem.toItemStack());

        // Slot 5 - Toggle Enable/Disable
        boolean enabled = manager.getFileManager().isEnabled();
        ItemBuilder toggleItem;
        if (enabled) {
            toggleItem = new ItemBuilder(Material.EMERALD);
            toggleItem.setName("&c&lDisable Kit Editor");
        } else {
            toggleItem = new ItemBuilder(Material.REDSTONE);
            toggleItem.setName("&a&lEnable Kit Editor");
        }
        inventory.setItem(5, toggleItem.toItemStack());

        // Place kit items starting at slot 10, skipping pane slots
        List<Kit> kits = manager.getEditableKits();
        int currentSlot = 10;
        for (Kit kit : kits) {
            while (paneSlots.contains(currentSlot)) {
                currentSlot++;
            }
            if (currentSlot >= size) break;

            ItemBuilder kitItem = new ItemBuilder(Material.DIAMOND_HELMET);
            kitItem.setName("&f" + kit.getName());
            inventory.setItem(currentSlot, kitItem.toItemStack());
            currentSlot++;
        }

        manager.setContext(player, KitEditorContext.ADMIN_MENU);
        manager.setOpenInventory(player, inventory);
        player.openInventory(inventory);
    }

    // --- KIT CONFIG MENU ---

    public static void openKitConfigMenu(KitEditorManager manager, Player player, String kitName) {
        int rows = 4;
        int size = rows * 9;
        String title = "&8Editing " + kitName;

        Inventory inventory = Bukkit.createInventory(null, size, CC.t(title));

        // Fill with black panes
        ItemStack pane = manager.createBlackPane();
        for (int i = 0; i < size; i++) {
            inventory.setItem(i, pane);
        }

        // Slot 10 - Edit Item Name
        ItemBuilder nameItem = new ItemBuilder(Material.NAME_TAG);
        nameItem.setName("&e&lEdit Item Name");
        nameItem.setLore(
                "&7",
                "&6Current: " + manager.getFileManager().getKitItemName(kitName),
                "&7",
                "&eClick to edit."
        );
        inventory.setItem(10, nameItem.toItemStack());

        // Slot 11 - Lore editor
        ItemBuilder loreItem = new ItemBuilder(resolveMaterial("COMMAND_BLOCK_MINECART", "COMMAND_MINECART"));
        loreItem.setName("&e&lLore Text");

        List<String> lore = manager.getLoreWorkingCopy().getOrDefault(player.getUniqueId(), new ArrayList<>(manager.getFileManager().getKitItemLore(kitName)));
        manager.getLoreWorkingCopy().put(player.getUniqueId(), lore);
        int selected = manager.getLoreSelectedIndex().getOrDefault(player.getUniqueId(), 0);

        List<String> loreDisplay = new ArrayList<>();
        if (lore.isEmpty()) {
            loreDisplay.add("&7");
        } else {
            for (int i = 0; i < lore.size(); i++) {
                if (i == selected) {
                    loreDisplay.add("&a" + i + ". &8" + lore.get(i));
                } else {
                    loreDisplay.add("&7" + i + ". &6" + lore.get(i));
                }
            }
            loreDisplay.add("&7");
        }
        loreDisplay.add("&aShift Left Click to add new line");
        loreDisplay.add("&cShift Right Click to delete a line");
        loreDisplay.add("&eLeft and Right to Scroll");
        loreDisplay.add("&bMiddle Click to edit Text");
        loreDisplay.add("&9Press Q to preview Item");

        loreItem.setLore(loreDisplay);
        inventory.setItem(11, loreItem.toItemStack());

        // Slot 12 - Select Slot
        ItemBuilder slotItem = new ItemBuilder(Material.HOPPER_MINECART);
        slotItem.setName("&e&lSelect Slot KitEditor Menu");
        slotItem.setLore("&7", "&eClick to edit.", "&7");
        inventory.setItem(12, slotItem.toItemStack());

        // Slot 13 - Kit decorative items
        ItemBuilder kitDecorItem = new ItemBuilder(Material.CHEST);
        kitDecorItem.setName("&e&lKit Editor " + kitName + " Menu");
        kitDecorItem.setLore(
                "&7Clicking here, you can place decorative items",
                "&7in the menu for editing the kit " + kitName,
                "&7",
                "&eClick to edit"
        );
        inventory.setItem(13, kitDecorItem.toItemStack());

        // Slot 14 - Armor edit or preview
        boolean preview = manager.getLorePreviewMode().getOrDefault(player.getUniqueId(), false);
        if (preview) {
            // Show the actual kit display item
            inventory.setItem(14, manager.createKitDisplayItem(kitName));
        } else {
            ItemBuilder armorItem = new ItemBuilder(Material.DIAMOND_CHESTPLATE);
            armorItem.setName("&e&lEdit Armor Items");
            inventory.setItem(14, armorItem.toItemStack());
        }

        // Slot 15 - Edit rows
        ItemBuilder kitRowsItem = new ItemBuilder(Material.ANVIL);
        kitRowsItem.setName("&e&lEdit Editor " + kitName + " Rows");
        kitRowsItem.setLore(
                "&7",
                "&6Current: &f" + manager.getFileManager().getKitMenuRows(kitName),
                "&7",
                "&eClick to edit."
        );
        inventory.setItem(15, kitRowsItem.toItemStack());

        // Slot 16 - Edit title
        ItemBuilder kitTitleItem = new ItemBuilder(resolveMaterial("OAK_SIGN", "SIGN"));
        kitTitleItem.setName("&e&lEdit Editor " + kitName + " Title");
        kitTitleItem.setLore(
                "&7",
                "&6Current: " + manager.getFileManager().getKitMenuTitle(kitName),
                "&7",
                "&eClick to edit"
        );
        inventory.setItem(16, kitTitleItem.toItemStack());

        // Slot 17 - Refill items
        ItemBuilder refillItem = new ItemBuilder(Material.EMERALD);
        refillItem.setName("&e&lEdit Refill Items");
        refillItem.setLore("&7", "&eClick to edit.", "&7");
        inventory.setItem(17, refillItem.toItemStack());

        // Slot 19 - Action items
        ItemBuilder actionItem = new ItemBuilder(Material.GOLD_INGOT);
        actionItem.setName("&e&lSave-Copy-Clear-Cancel Items Editor");
        actionItem.setLore("&7", "&eClick to edit.", "&7");
        inventory.setItem(19, actionItem.toItemStack());

        manager.setContext(player, KitEditorContext.KIT_CONFIG);
        manager.setContextKit(player, kitName);
        manager.setOpenInventory(player, inventory);
        player.openInventory(inventory);
    }

    // --- DECORATIVE EDIT MAIN ---

    public static void openDecorativeEditMain(KitEditorManager manager, Player player) {
        String title = manager.getFileManager().getTitle() + " &8Editing Mode";
        int rows = manager.getFileManager().getRows();
        int size = rows * 9;

        Inventory inventory = Bukkit.createInventory(null, size, CC.t(title));

        // Place existing decorative items
        Map<Integer, ItemStack> decorativeItems = manager.getFileManager().getGlobalDecorativeItems();
        for (Map.Entry<Integer, ItemStack> entry : decorativeItems.entrySet()) {
            if (entry.getKey() >= 0 && entry.getKey() < size) {
                inventory.setItem(entry.getKey(), entry.getValue().clone());
            }
        }

        // Place kit display items (read-only)
        for (Kit kit : manager.getEditableKits()) {
            int displaySlot = manager.getFileManager().getKitDisplaySlot(kit.getName());
            if (displaySlot >= 0 && displaySlot < size) {
                inventory.setItem(displaySlot, manager.createKitDisplayItem(kit.getName()));
            }
        }

        manager.setContext(player, KitEditorContext.DECORATIVE_EDIT_MAIN);
        manager.setOpenInventory(player, inventory);
        player.openInventory(inventory);
    }

    // --- DECORATIVE EDIT KIT ---

    public static void openDecorativeEditKit(KitEditorManager manager, Player player, String kitName) {
        String title = manager.getFileManager().getKitMenuTitle(kitName);
        int rows = manager.getFileManager().getKitMenuRows(kitName);
        int size = rows * 9;

        Inventory inventory = Bukkit.createInventory(null, size, CC.t(title));

        // Place kit decorative items
        Map<Integer, ItemStack> decorativeItems = manager.getFileManager().getKitDecorativeItems(kitName);
        for (Map.Entry<Integer, ItemStack> entry : decorativeItems.entrySet()) {
            if (entry.getKey() >= 0 && entry.getKey() < size) {
                inventory.setItem(entry.getKey(), entry.getValue().clone());
            }
        }

        // Place armor display items
        Kit kit = manager.getInstance().getKitManager().getKit(kitName);
        if (kit != null && kit.getArmorContents() != null) {
            int helmSlot = manager.getFileManager().getArmorSlot(kitName, "helmet");
            int chestSlot = manager.getFileManager().getArmorSlot(kitName, "chestplate");
            int legsSlot = manager.getFileManager().getArmorSlot(kitName, "leggings");
            int bootsSlot = manager.getFileManager().getArmorSlot(kitName, "boots");

            if (helmSlot >= 0 && helmSlot < size && kit.getArmorContents().length > 0 && kit.getArmorContents()[0] != null)
                inventory.setItem(helmSlot, kit.getArmorContents()[0].clone());
            if (chestSlot >= 0 && chestSlot < size && kit.getArmorContents().length > 1 && kit.getArmorContents()[1] != null)
                inventory.setItem(chestSlot, kit.getArmorContents()[1].clone());
            if (legsSlot >= 0 && legsSlot < size && kit.getArmorContents().length > 2 && kit.getArmorContents()[2] != null)
                inventory.setItem(legsSlot, kit.getArmorContents()[2].clone());
            if (bootsSlot >= 0 && bootsSlot < size && kit.getArmorContents().length > 3 && kit.getArmorContents()[3] != null)
                inventory.setItem(bootsSlot, kit.getArmorContents()[3].clone());
        }

        // Place refill items
        Map<Integer, ItemStack> refillItems = manager.getFileManager().getRefillItems(kitName);
        for (Map.Entry<Integer, ItemStack> entry : refillItems.entrySet()) {
            if (entry.getKey() >= 0 && entry.getKey() < size) {
                inventory.setItem(entry.getKey(), entry.getValue().clone());
            }
        }

        // Place action items
        int saveSlot = manager.getFileManager().getActionSlot(kitName, "save");
        int cancelSlot = manager.getFileManager().getActionSlot(kitName, "cancel");
        int copySlot = manager.getFileManager().getActionSlot(kitName, "copy");

        if (saveSlot >= 0 && saveSlot < size) {
            ItemStack saveItem = manager.getFileManager().getActionItem(kitName, "save");
            if (saveItem != null) inventory.setItem(saveSlot, saveItem.clone());
        }
        if (cancelSlot >= 0 && cancelSlot < size) {
            ItemStack cancelItem = manager.getFileManager().getActionItem(kitName, "cancel");
            if (cancelItem != null) inventory.setItem(cancelSlot, cancelItem.clone());
        }
        if (copySlot >= 0 && copySlot < size) {
            ItemStack copyItem = manager.getFileManager().getActionItem(kitName, "copy");
            if (copyItem != null) inventory.setItem(copySlot, copyItem.clone());
        }

        manager.setContext(player, KitEditorContext.DECORATIVE_EDIT_KIT);
        manager.setContextKit(player, kitName);
        manager.setOpenInventory(player, inventory);
        player.openInventory(inventory);
    }

    // --- SLOT PICKER ---

    public static void openSlotPicker(KitEditorManager manager, Player player, String kitName) {
        String title = manager.getFileManager().getTitle();
        int rows = manager.getFileManager().getRows();
        int size = rows * 9;

        Inventory inventory = Bukkit.createInventory(null, size, CC.t(title));

        // Place existing items (decorative + kit items)
        Map<Integer, ItemStack> decorativeItems = manager.getFileManager().getGlobalDecorativeItems();
        for (Map.Entry<Integer, ItemStack> entry : decorativeItems.entrySet()) {
            if (entry.getKey() >= 0 && entry.getKey() < size) {
                inventory.setItem(entry.getKey(), entry.getValue().clone());
            }
        }

        Set<Integer> occupiedSlots = new HashSet<>(decorativeItems.keySet());
        for (Kit kit : manager.getEditableKits()) {
            int ds = manager.getFileManager().getKitDisplaySlot(kit.getName());
            if (ds >= 0 && ds < size) {
                inventory.setItem(ds, manager.createKitDisplayItem(kit.getName()));
                occupiedSlots.add(ds);
            }
        }

        // Fill empty slots with cycling panes
        Set<Integer> cycleSlots = new HashSet<>();
        for (int i = 0; i < size; i++) {
            if (!occupiedSlots.contains(i)) {
                cycleSlots.add(i);
            }
        }

        // Set initial panes with name
        for (int slot : cycleSlots) {
            ItemStack pane = manager.createColoredPane(0);
            ItemMeta meta = pane.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(CC.t("&e&lClick here to set here the slot"));
                pane.setItemMeta(meta);
            }
            inventory.setItem(slot, pane);
        }

        manager.setContext(player, KitEditorContext.SLOT_PICKER);
        manager.setContextKit(player, kitName);
        manager.setOpenInventory(player, inventory);
        player.openInventory(inventory);

        // Start cycling task
        manager.startCyclingTask(player, inventory, cycleSlots);
    }

    // --- ARMOR EDIT ---

    public static void openArmorEdit(KitEditorManager manager, Player player, String kitName) {
        String title = manager.getFileManager().getKitMenuTitle(kitName);
        int rows = manager.getFileManager().getKitMenuRows(kitName);
        int size = rows * 9;

        Inventory inventory = Bukkit.createInventory(null, size, CC.t(title));

        // Determine occupied slots (decorative, armor markers, etc.)
        Set<Integer> occupiedSlots = new HashSet<>();

        // Existing decorative items
        Map<Integer, ItemStack> decorItems = manager.getFileManager().getKitDecorativeItems(kitName);
        for (Map.Entry<Integer, ItemStack> entry : decorItems.entrySet()) {
            if (entry.getKey() >= 0 && entry.getKey() < size) {
                inventory.setItem(entry.getKey(), entry.getValue().clone());
                occupiedSlots.add(entry.getKey());
            }
        }

        // Existing armor markers
        String[] pieces = {"helmet", "chestplate", "leggings", "boots"};
        for (String piece : pieces) {
            int slot = manager.getFileManager().getArmorSlot(kitName, piece);
            if (slot >= 0 && slot < size) {
                ItemStack marker = createArmorMarkerItem(piece);
                inventory.setItem(slot, marker);
                occupiedSlots.add(slot);
            }
        }

        // Fill empty slots with cycling panes
        Set<Integer> cycleSlots = new HashSet<>();
        for (int i = 0; i < size; i++) {
            if (!occupiedSlots.contains(i)) {
                cycleSlots.add(i);
            }
        }

        for (int slot : cycleSlots) {
            ItemStack pane = manager.createColoredPane(0);
            ItemMeta meta = pane.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(" ");
                meta.setLore(Arrays.asList(
                        CC.t("&eLeft Click to set Helmet"),
                        CC.t("&6Right Click to set Chestplate"),
                        CC.t("&bShift + Left Click to set Leggings"),
                        CC.t("&9Shift + Right Click to set Boots"),
                        CC.t("&7"),
                        CC.t("&7Click the same spot to remove it from where you placed it.")
                ));
                pane.setItemMeta(meta);
            }
            inventory.setItem(slot, pane);
        }

        manager.setContext(player, KitEditorContext.ARMOR_EDIT);
        manager.setContextKit(player, kitName);
        manager.setOpenInventory(player, inventory);
        player.openInventory(inventory);

        manager.startCyclingTask(player, inventory, cycleSlots);
    }

    @SuppressWarnings("deprecation")
    private static ItemStack createArmorMarkerItem(String piece) {
        String name;
        switch (piece) {
            case "helmet": name = "&c&lHelmet"; break;
            case "chestplate": name = "&c&lChestplate"; break;
            case "leggings": name = "&c&lLeggings"; break;
            case "boots": name = "&c&lBoots"; break;
            default: name = "&c&lArmor";
        }
        ItemBuilder builder = new ItemBuilder(Material.BEDROCK);
        builder.setName(name);
        builder.setLore("&7", "&eClick to remove!", "&7");
        return builder.toItemStack();
    }

    // --- REFILL EDIT ---

    public static void openRefillEdit(KitEditorManager manager, Player player, String kitName) {
        String title = manager.getFileManager().getKitMenuTitle(kitName);
        int rows = manager.getFileManager().getKitMenuRows(kitName);
        int size = rows * 9;

        Inventory inventory = Bukkit.createInventory(null, size, CC.t(title));

        // Place existing items
        Set<Integer> occupiedSlots = new HashSet<>();

        Map<Integer, ItemStack> decorItems = manager.getFileManager().getKitDecorativeItems(kitName);
        for (Map.Entry<Integer, ItemStack> entry : decorItems.entrySet()) {
            if (entry.getKey() >= 0 && entry.getKey() < size) {
                inventory.setItem(entry.getKey(), entry.getValue().clone());
                occupiedSlots.add(entry.getKey());
            }
        }

        Map<Integer, ItemStack> refillItems = manager.getFileManager().getRefillItems(kitName);
        for (Map.Entry<Integer, ItemStack> entry : refillItems.entrySet()) {
            if (entry.getKey() >= 0 && entry.getKey() < size) {
                inventory.setItem(entry.getKey(), entry.getValue().clone());
                occupiedSlots.add(entry.getKey());
            }
        }

        // Place armor markers
        String[] pieces = {"helmet", "chestplate", "leggings", "boots"};
        for (String piece : pieces) {
            int slot = manager.getFileManager().getArmorSlot(kitName, piece);
            if (slot >= 0 && slot < size) {
                Kit kit = manager.getInstance().getKitManager().getKit(kitName);
                if (kit != null) {
                    int idx = getArmorIndex(piece);
                    if (kit.getArmorContents().length > idx && kit.getArmorContents()[idx] != null) {
                        inventory.setItem(slot, kit.getArmorContents()[idx].clone());
                        occupiedSlots.add(slot);
                    }
                }
            }
        }

        // Action items
        int saveSlot = manager.getFileManager().getActionSlot(kitName, "save");
        int cancelSlot = manager.getFileManager().getActionSlot(kitName, "cancel");
        int copySlot = manager.getFileManager().getActionSlot(kitName, "copy");
        if (saveSlot >= 0 && saveSlot < size) {
            ItemStack saveItem = manager.getFileManager().getActionItem(kitName, "save");
            if (saveItem != null) { inventory.setItem(saveSlot, saveItem.clone()); occupiedSlots.add(saveSlot); }
        }
        if (cancelSlot >= 0 && cancelSlot < size) {
            ItemStack cancelItem = manager.getFileManager().getActionItem(kitName, "cancel");
            if (cancelItem != null) { inventory.setItem(cancelSlot, cancelItem.clone()); occupiedSlots.add(cancelSlot); }
        }
        if (copySlot >= 0 && copySlot < size) {
            ItemStack copyItem = manager.getFileManager().getActionItem(kitName, "copy");
            if (copyItem != null) { inventory.setItem(copySlot, copyItem.clone()); occupiedSlots.add(copySlot); }
        }

        // Fill empty with cycling panes
        Set<Integer> cycleSlots = new HashSet<>();
        for (int i = 0; i < size; i++) {
            if (!occupiedSlots.contains(i)) {
                cycleSlots.add(i);
            }
        }

        for (int slot : cycleSlots) {
            ItemStack pane = manager.createColoredPane(0);
            ItemMeta meta = pane.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(CC.t("&e&lClick here and then read the chat."));
                pane.setItemMeta(meta);
            }
            inventory.setItem(slot, pane);
        }

        manager.setContext(player, KitEditorContext.REFILL_EDIT);
        manager.setContextKit(player, kitName);
        manager.setOpenInventory(player, inventory);
        player.openInventory(inventory);

        manager.startCyclingTask(player, inventory, cycleSlots);
    }

    // --- ACTION EDIT ---

    public static void openActionEdit(KitEditorManager manager, Player player, String kitName) {
        String title = manager.getFileManager().getKitMenuTitle(kitName);
        int rows = manager.getFileManager().getKitMenuRows(kitName);
        int size = rows * 9;

        Inventory inventory = Bukkit.createInventory(null, size, CC.t(title));

        Set<Integer> occupiedSlots = new HashSet<>();

        // Decorative items
        Map<Integer, ItemStack> decorItems = manager.getFileManager().getKitDecorativeItems(kitName);
        for (Map.Entry<Integer, ItemStack> entry : decorItems.entrySet()) {
            if (entry.getKey() >= 0 && entry.getKey() < size) {
                inventory.setItem(entry.getKey(), entry.getValue().clone());
                occupiedSlots.add(entry.getKey());
            }
        }

        // Refill items
        Map<Integer, ItemStack> refillItems = manager.getFileManager().getRefillItems(kitName);
        for (Map.Entry<Integer, ItemStack> entry : refillItems.entrySet()) {
            if (entry.getKey() >= 0 && entry.getKey() < size) {
                inventory.setItem(entry.getKey(), entry.getValue().clone());
                occupiedSlots.add(entry.getKey());
            }
        }

        // Armor markers
        String[] pieces = {"helmet", "chestplate", "leggings", "boots"};
        for (String piece : pieces) {
            int slot = manager.getFileManager().getArmorSlot(kitName, piece);
            if (slot >= 0 && slot < size) {
                Kit kit = manager.getInstance().getKitManager().getKit(kitName);
                if (kit != null) {
                    int idx = getArmorIndex(piece);
                    if (kit.getArmorContents().length > idx && kit.getArmorContents()[idx] != null) {
                        inventory.setItem(slot, kit.getArmorContents()[idx].clone());
                        occupiedSlots.add(slot);
                    }
                }
            }
        }

        // Existing action items
        int saveSlot = manager.getFileManager().getActionSlot(kitName, "save");
        int cancelSlot = manager.getFileManager().getActionSlot(kitName, "cancel");
        int copySlot = manager.getFileManager().getActionSlot(kitName, "copy");
        if (saveSlot >= 0 && saveSlot < size) {
            ItemStack saveItem = manager.getFileManager().getActionItem(kitName, "save");
            if (saveItem != null) { inventory.setItem(saveSlot, saveItem.clone()); occupiedSlots.add(saveSlot); }
        }
        if (cancelSlot >= 0 && cancelSlot < size) {
            ItemStack cancelItem = manager.getFileManager().getActionItem(kitName, "cancel");
            if (cancelItem != null) { inventory.setItem(cancelSlot, cancelItem.clone()); occupiedSlots.add(cancelSlot); }
        }
        if (copySlot >= 0 && copySlot < size) {
            ItemStack copyItem = manager.getFileManager().getActionItem(kitName, "copy");
            if (copyItem != null) { inventory.setItem(copySlot, copyItem.clone()); occupiedSlots.add(copySlot); }
        }

        // Fill empty with cycling panes
        Set<Integer> cycleSlots = new HashSet<>();
        for (int i = 0; i < size; i++) {
            if (!occupiedSlots.contains(i)) {
                cycleSlots.add(i);
            }
        }

        for (int slot : cycleSlots) {
            ItemStack pane = manager.createColoredPane(0);
            ItemMeta meta = pane.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(CC.t("&e&lClick here and then read the chat."));
                pane.setItemMeta(meta);
            }
            inventory.setItem(slot, pane);
        }

        manager.setContext(player, KitEditorContext.ACTION_EDIT);
        manager.setContextKit(player, kitName);
        manager.setOpenInventory(player, inventory);
        player.openInventory(inventory);

        manager.startCyclingTask(player, inventory, cycleSlots);
    }

    // --- KIT SESSION (player-facing) ---

    public static void openKitSession(KitEditorManager manager, Player player, String kitName) {
        Kit kit = manager.getInstance().getKitManager().getKit(kitName);
        if (kit == null) return;

        String title = manager.getFileManager().getKitMenuTitle(kitName);
        int rows = manager.getFileManager().getKitMenuRows(kitName);
        int size = rows * 9;

        Inventory inventory = Bukkit.createInventory(null, size, CC.t(title));

        // Start session and snapshot
        KitEditorSession session = manager.startSession(player, kitName);

        // Load kit items into player inventory
        player.getInventory().clear();

        ItemStack[] contentsToLoad;
        ItemStack[] armorToLoad;

        UUID uuid = player.getUniqueId();
        if (manager.getFileManager().hasPersonalKit(uuid, kitName)) {
            contentsToLoad = manager.getFileManager().getPersonalKitContents(uuid, kitName);
            armorToLoad = manager.getFileManager().getPersonalKitArmor(uuid, kitName);
        } else {
            contentsToLoad = kit.getContents();
            armorToLoad = kit.getArmorContents();
        }

        if (contentsToLoad != null) {
            for (int i = 0; i < contentsToLoad.length && i < 36; i++) {
                ItemStack toSet = contentsToLoad[i];
                if (toSet == null || toSet.getType() == Material.AIR) continue;
                player.getInventory().setItem(i, toSet.clone());
            }
        }

        if (armorToLoad != null) {
            for (int i = 0; i < armorToLoad.length && i < 4; i++) {
                ItemStack toSet = armorToLoad[i];
                if (toSet == null || toSet.getType() == Material.AIR) continue;
                player.getInventory().setItem(36 + i, toSet.clone());
            }
        }

        player.updateInventory();

        // Place decorative items in menu
        Map<Integer, ItemStack> decorItems = manager.getFileManager().getKitDecorativeItems(kitName);
        for (Map.Entry<Integer, ItemStack> entry : decorItems.entrySet()) {
            if (entry.getKey() >= 0 && entry.getKey() < size) {
                inventory.setItem(entry.getKey(), entry.getValue().clone());
            }
        }

        // Place armor display items
        if (kit.getArmorContents() != null) {
            int helmSlot = manager.getFileManager().getArmorSlot(kitName, "helmet");
            int chestSlot = manager.getFileManager().getArmorSlot(kitName, "chestplate");
            int legsSlot = manager.getFileManager().getArmorSlot(kitName, "leggings");
            int bootsSlot = manager.getFileManager().getArmorSlot(kitName, "boots");

            if (helmSlot >= 0 && helmSlot < size && kit.getArmorContents().length > 0 && kit.getArmorContents()[0] != null)
                inventory.setItem(helmSlot, kit.getArmorContents()[0].clone());
            if (chestSlot >= 0 && chestSlot < size && kit.getArmorContents().length > 1 && kit.getArmorContents()[1] != null)
                inventory.setItem(chestSlot, kit.getArmorContents()[1].clone());
            if (legsSlot >= 0 && legsSlot < size && kit.getArmorContents().length > 2 && kit.getArmorContents()[2] != null)
                inventory.setItem(legsSlot, kit.getArmorContents()[2].clone());
            if (bootsSlot >= 0 && bootsSlot < size && kit.getArmorContents().length > 3 && kit.getArmorContents()[3] != null)
                inventory.setItem(bootsSlot, kit.getArmorContents()[3].clone());
        }

        // Place refill items
        Map<Integer, ItemStack> refillItems = manager.getFileManager().getRefillItems(kitName);
        for (Map.Entry<Integer, ItemStack> entry : refillItems.entrySet()) {
            if (entry.getKey() >= 0 && entry.getKey() < size) {
                inventory.setItem(entry.getKey(), entry.getValue().clone());
            }
        }

        // Place action items
        int saveSlot = manager.getFileManager().getActionSlot(kitName, "save");
        int cancelSlot = manager.getFileManager().getActionSlot(kitName, "cancel");
        int copySlot = manager.getFileManager().getActionSlot(kitName, "copy");

        if (saveSlot >= 0 && saveSlot < size) {
            ItemStack saveItem = manager.getFileManager().getActionItem(kitName, "save");
            if (saveItem != null) inventory.setItem(saveSlot, saveItem.clone());
        }
        if (cancelSlot >= 0 && cancelSlot < size) {
            ItemStack cancelItem = manager.getFileManager().getActionItem(kitName, "cancel");
            if (cancelItem != null) inventory.setItem(cancelSlot, cancelItem.clone());
        }
        if (copySlot >= 0 && copySlot < size) {
            ItemStack copyItem = manager.getFileManager().getActionItem(kitName, "copy");
            if (copyItem != null) inventory.setItem(copySlot, copyItem.clone());
        }

        manager.setContext(player, KitEditorContext.KIT_SESSION);
        manager.setOpenInventory(player, inventory);
        player.openInventory(inventory);
    }

    private static int getArmorIndex(String piece) {
        switch (piece) {
            case "helmet": return 0;
            case "chestplate": return 1;
            case "leggings": return 2;
            case "boots": return 3;
            default: return 0;
        }
    }
}
