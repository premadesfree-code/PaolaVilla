package me.keano.azurite.modules.kiteditor;

import me.keano.azurite.modules.kits.Kit;
import me.keano.azurite.utils.CC;
import me.keano.azurite.utils.ItemBuilder;
import me.keano.azurite.utils.fanciful.FancyMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

/**
 * Copyright (c) 2025. Keano
 * Use or redistribution of source or file is
 * only permitted if given explicit permission.
 */
public class KitEditorMenuListener implements Listener {

    private final KitEditorManager manager;

    public KitEditorMenuListener(KitEditorManager manager) {
        this.manager = manager;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();

        KitEditorContext context = manager.getContext(player);
        if (context == KitEditorContext.NONE) return;

        Inventory topInventory = player.getOpenInventory().getTopInventory();
        Inventory clickedInventory = event.getClickedInventory();

        switch (context) {
            case MAIN_MENU:
                handleMainMenuClick(event, player, clickedInventory, topInventory);
                break;
            case KIT_SESSION:
                handleKitSessionClick(event, player, clickedInventory, topInventory);
                break;
            case ADMIN_MENU:
                handleAdminMenuClick(event, player, clickedInventory, topInventory);
                break;
            case KIT_CONFIG:
                handleKitConfigClick(event, player, clickedInventory, topInventory);
                break;
            case DECORATIVE_EDIT_MAIN:
                handleDecorativeEditMainClick(event, player, clickedInventory, topInventory);
                break;
            case DECORATIVE_EDIT_KIT:
                handleDecorativeEditKitClick(event, player, clickedInventory, topInventory);
                break;
            case SLOT_PICKER:
                handleSlotPickerClick(event, player, clickedInventory, topInventory);
                break;
            case ARMOR_EDIT:
                handleArmorEditClick(event, player, clickedInventory, topInventory);
                break;
            case REFILL_EDIT:
                handleRefillEditClick(event, player, clickedInventory, topInventory);
                break;
            case ACTION_EDIT:
                handleActionEditClick(event, player, clickedInventory, topInventory);
                break;
            default:
                break;
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        KitEditorContext context = manager.getContext(player);
        if (context == KitEditorContext.NONE) return;

        Inventory topInventory = player.getOpenInventory().getTopInventory();
        int topSize = topInventory.getSize();

        // For most contexts, cancel drags into top inventory
        switch (context) {
            case MAIN_MENU:
            case ADMIN_MENU:
            case KIT_CONFIG:
            case SLOT_PICKER:
            case ARMOR_EDIT:
                event.setCancelled(true);
                break;
            case KIT_SESSION:
                for (int slot : event.getRawSlots()) {
                    if (slot < topSize) {
                        event.setCancelled(true);
                        return;
                    }
                }
                break;
            case DECORATIVE_EDIT_MAIN:
            case DECORATIVE_EDIT_KIT:
            case REFILL_EDIT:
            case ACTION_EDIT:
                // Allow drags only on empty slots in top inventory for decorative editing
                for (int slot : event.getRawSlots()) {
                    if (slot < topSize) {
                        ItemStack existing = topInventory.getItem(slot);
                        if (existing != null && existing.getType() != Material.AIR && !manager.isCyclingPane(existing)) {
                            event.setCancelled(true);
                            return;
                        }
                    }
                }
                break;
            default:
                break;
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        Player player = (Player) event.getPlayer();
        KitEditorContext context = manager.getContext(player);
        if (context == KitEditorContext.NONE) return;

        KitEditorSession session = manager.getSession(player);

        // If in transition, don't process close
        if (session != null && session.isTransition()) {
            return;
        }

        switch (context) {
            case MAIN_MENU:
                manager.clearContext(player);
                manager.clearOpenInventory(player);
                break;
            case KIT_SESSION:
                handleKitSessionClose(player);
                break;
            case ADMIN_MENU:
                manager.clearContext(player);
                manager.clearOpenInventory(player);
                break;
            case KIT_CONFIG:
                manager.clearContext(player);
                manager.clearOpenInventory(player);
                break;
            case DECORATIVE_EDIT_MAIN:
                handleDecorativeEditMainClose(player);
                break;
            case DECORATIVE_EDIT_KIT:
                handleDecorativeEditKitClose(player);
                break;
            case SLOT_PICKER:
                manager.cancelCyclingTask(player);
                manager.clearContext(player);
                manager.clearOpenInventory(player);
                break;
            case ARMOR_EDIT:
                handleArmorEditClose(player);
                break;
            case REFILL_EDIT:
                handleRefillEditClose(player);
                break;
            case ACTION_EDIT:
                handleActionEditClose(player);
                break;
            default:
                break;
        }
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        if (manager.hasSession(player)) {
            event.setCancelled(true);
            event.getItemDrop().remove();
        }
    }

    @EventHandler
    public void onPickup(PlayerPickupItemEvent event) {
        Player player = event.getPlayer();
        if (manager.hasSession(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (manager.hasSession(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (manager.hasSession(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    // --- MAIN MENU ---

    private void handleMainMenuClick(InventoryClickEvent event, Player player, Inventory clickedInventory, Inventory topInventory) {
        event.setCancelled(true);

        if (clickedInventory == null || clickedInventory == player.getInventory()) return;

        int slot = event.getRawSlot();
        String kitName = findKitBySlot(slot, topInventory);

        if (kitName != null) {
            manager.clearContext(player);
            manager.clearOpenInventory(player);
            manager.openKitSession(player, kitName);
            return;
        }

        // Check action items (save/cancel/copy) - these would be from the kit's action items
        // In main menu, these aren't typically configured, but if they are, handle them
    }

    private String findKitBySlot(int slot, Inventory inventory) {
        for (Kit kit : manager.getEditableKits()) {
            int displaySlot = manager.getFileManager().getKitDisplaySlot(kit.getName());
            if (displaySlot == slot) {
                return kit.getName();
            }
        }
        return null;
    }

    // --- KIT SESSION ---

    private void handleKitSessionClick(InventoryClickEvent event, Player player, Inventory clickedInventory, Inventory topInventory) {
        KitEditorSession session = manager.getSession(player);
        if (session == null) return;

        int topSize = topInventory.getSize();

        // Allow player inventory interaction
        if (clickedInventory == player.getInventory()) {
            return;
        }

        // Cancel all top inventory clicks
        event.setCancelled(true);

        if (clickedInventory == null || clickedInventory != topInventory) return;

        int slot = event.getRawSlot();
        String kitName = session.getKitName();

        // Check refill items
        Map<Integer, ItemStack> refillItems = manager.getFileManager().getRefillItems(kitName);
        if (refillItems.containsKey(slot)) {
            String type = manager.getFileManager().getRefillType(kitName, slot);
            ItemStack refillItem = refillItems.get(slot);

            if (refillItem != null && refillItem.getType() != Material.AIR) {
                ItemStack toGive = refillItem.clone();
                if ("fill_inventory".equals(type)) {
                    toGive.setAmount(1);
                    for (int i = 0; i < 36; i++) {
                        ItemStack existing = player.getInventory().getItem(i);
                        if (existing == null || existing.getType() == Material.AIR) {
                            player.getInventory().setItem(i, toGive.clone());
                        }
                    }
                } else {
                    for (int i = 0; i < 36; i++) {
                        ItemStack existing = player.getInventory().getItem(i);
                        if (existing == null || existing.getType() == Material.AIR) {
                            player.getInventory().setItem(i, toGive.clone());
                        }
                    }
                }
                player.updateInventory();
            }
            return;
        }

        // Check action items
        int saveSlot = manager.getFileManager().getActionSlot(kitName, "save");
        int cancelSlot = manager.getFileManager().getActionSlot(kitName, "cancel");
        int copySlot = manager.getFileManager().getActionSlot(kitName, "copy");

        if (slot == saveSlot && saveSlot >= 0) {
            // Save
            ItemStack[] contents = player.getInventory().getContents();
            ItemStack[] armor = player.getInventory().getArmorContents();

            ItemStack[] saveContents = new ItemStack[36];
            for (int i = 0; i < 36; i++) {
                saveContents[i] = (contents[i] != null && contents[i].getType() != Material.AIR ? contents[i].clone() : null);
            }

            ItemStack[] saveArmor = new ItemStack[4];
            for (int i = 0; i < 4; i++) {
                saveArmor[i] = (i < armor.length && armor[i] != null && armor[i].getType() != Material.AIR ? armor[i].clone() : null);
            }

            manager.getFileManager().savePersonalKit(player.getUniqueId(), kitName, saveContents, saveArmor);

            session.setTransition(true);
            manager.endSession(player);
            manager.cancelCyclingTask(player);
            manager.clearContext(player);
            manager.clearOpenInventory(player);

            player.closeInventory();
            session.restore();
            session.setTransition(false);

            player.sendMessage(CC.t("&aKit saved!"));
            return;
        }

        if (slot == cancelSlot && cancelSlot >= 0) {
            session.setTransition(true);
            manager.endSession(player);
            manager.cancelCyclingTask(player);
            manager.clearContext(player);
            manager.clearOpenInventory(player);

            player.closeInventory();
            session.restore();
            session.setTransition(false);
            return;
        }

        if (slot == copySlot && copySlot >= 0) {
            Kit kit = manager.getInstance().getKitManager().getKit(kitName);
            if (kit != null) {
                player.getInventory().clear();
                for (int i = 0; i < kit.getContents().length; i++) {
                    ItemStack toSet = kit.getContents()[i];
                    if (toSet == null || toSet.getType() == Material.AIR) continue;
                    player.getInventory().setItem(i, toSet.clone());
                }
                for (int i = 0; i < kit.getArmorContents().length; i++) {
                    ItemStack toSet = kit.getArmorContents()[i];
                    if (toSet == null || toSet.getType() == Material.AIR) continue;
                    player.getInventory().setItem(36 + i, toSet.clone());
                }
                player.updateInventory();
            }
            return;
        }
    }

    private void handleKitSessionClose(Player player) {
        KitEditorSession session = manager.getSession(player);
        if (session == null) {
            manager.clearContext(player);
            manager.clearOpenInventory(player);
            return;
        }

        // Close without save = cancel
        session.setTransition(true);
        manager.endSession(player);
        manager.cancelCyclingTask(player);
        manager.clearContext(player);
        manager.clearOpenInventory(player);

        session.restore();
        session.setTransition(false);
    }

    // --- ADMIN MENU ---

    private void handleAdminMenuClick(InventoryClickEvent event, Player player, Inventory clickedInventory, Inventory topInventory) {
        event.setCancelled(true);

        if (clickedInventory == null || clickedInventory == player.getInventory()) return;

        int slot = event.getRawSlot();

        switch (slot) {
            case 2: // Edit title
                player.closeInventory();
                manager.clearContext(player);
                manager.clearOpenInventory(player);
                manager.setPendingInput(player, KitEditorPendingInput.MENU_TITLE);
                player.sendMessage(CC.t("&ePlease type the new Menu Title or type &cCancel &eto cancel"));
                break;

            case 3: // Edit rows
                player.closeInventory();
                manager.clearContext(player);
                manager.clearOpenInventory(player);
                manager.setPendingInput(player, KitEditorPendingInput.MENU_ROWS);
                player.sendMessage(CC.t("&ePlease type the rows &f(1-6) &efor /kiteditor menu"));
                break;

            case 4: // Edit decorative items
                manager.clearContext(player);
                manager.clearOpenInventory(player);
                manager.openDecorativeEditMain(player);
                break;

            case 5: // Toggle enable/disable
                boolean enabled = manager.getFileManager().isEnabled();
                manager.getFileManager().setEnabled(!enabled);
                manager.openAdminMenu(player);
                break;

            default:
                // Check kit items (slot 10+)
                if (slot >= 10) {
                    String kitName = findKitByAdminSlot(slot, topInventory);
                    if (kitName != null) {
                        manager.clearContext(player);
                        manager.clearOpenInventory(player);
                        manager.setContextKit(player, kitName);
                        manager.openKitConfigMenu(player, kitName);
                    }
                }
                break;
        }
    }

    private String findKitByAdminSlot(int slot, Inventory inventory) {
        ItemStack item = inventory.getItem(slot);
        if (item == null || item.getType() != Material.DIAMOND_HELMET) return null;

        for (Kit kit : manager.getEditableKits()) {
            if (slot == getAdminKitSlot(kit.getName())) {
                return kit.getName();
            }
        }
        return null;
    }

    private int getAdminKitSlot(String kitName) {
        // Kits are placed starting at slot 10, skipping pane slots
        // We store display_slot for the main menu, but admin menu auto-places
        List<Kit> kits = manager.getEditableKits();
        Set<Integer> blocked = manager.getAdminPaneSlots();
        int currentSlot = 10;

        for (Kit kit : kits) {
            while (blocked.contains(currentSlot) || currentSlot % 9 == 8) {
                currentSlot++;
            }
            if (kit.getName().equalsIgnoreCase(kitName)) {
                return currentSlot;
            }
            currentSlot++;
        }
        return -1;
    }

    // --- KIT CONFIG ---

    private void handleKitConfigClick(InventoryClickEvent event, Player player, Inventory clickedInventory, Inventory topInventory) {
        event.setCancelled(true);

        if (clickedInventory == null || clickedInventory == player.getInventory()) return;

        int slot = event.getRawSlot();
        String kitName = manager.getContextKit(player);
        if (kitName == null) return;

        switch (slot) {
            case 10: // Edit item name
                player.closeInventory();
                manager.setPendingInput(player, KitEditorPendingInput.KIT_ITEM_NAME);
                player.sendMessage(CC.t("&eType the value in chat. Type &cCancel &eto cancel"));
                break;

            case 11: // Lore editor
                handleLoreClick(event, player, kitName);
                break;

            case 12: // Select slot
                manager.clearContext(player);
                manager.clearOpenInventory(player);
                manager.openSlotPicker(player, kitName);
                break;

            case 13: // Kit decorative items edit
                manager.clearContext(player);
                manager.clearOpenInventory(player);
                manager.openDecorativeEditKit(player, kitName);
                break;

            case 14: // Armor items edit
                Kit kit = manager.getInstance().getKitManager().getKit(kitName);
                if (kit == null || !hasFullArmor(kit)) {
                    player.sendMessage(CC.t("&cThe Kit doesn't have &c&lArmor"));
                    return;
                }
                manager.clearContext(player);
                manager.clearOpenInventory(player);
                manager.openArmorEdit(player, kitName);
                break;

            case 15: // Edit kit menu rows
                player.closeInventory();
                manager.setPendingInput(player, KitEditorPendingInput.KIT_MENU_ROWS);
                player.sendMessage(CC.t("&eType the rows &f(1-6)"));
                break;

            case 16: // Edit kit menu title
                player.closeInventory();
                manager.setPendingInput(player, KitEditorPendingInput.KIT_MENU_TITLE);
                player.sendMessage(CC.t("&ePlease type Value"));
                break;

            case 17: // Refill items edit
                manager.clearContext(player);
                manager.clearOpenInventory(player);
                manager.openRefillEdit(player, kitName);
                break;

            case 19: // Action items edit
                manager.clearContext(player);
                manager.clearOpenInventory(player);
                manager.openActionEdit(player, kitName);
                break;

            default:
                break;
        }
    }

    private boolean hasFullArmor(Kit kit) {
        ItemStack[] armor = kit.getArmorContents();
        if (armor == null || armor.length < 4) return false;
        for (ItemStack piece : armor) {
            if (piece == null || piece.getType() == Material.AIR) return false;
        }
        return true;
    }

    private void handleLoreClick(InventoryClickEvent event, Player player, String kitName) {
        List<String> lore = manager.getLoreWorkingCopy().getOrDefault(player.getUniqueId(), new ArrayList<>(manager.getFileManager().getKitItemLore(kitName)));
        manager.getLoreWorkingCopy().put(player.getUniqueId(), lore);

        int selected = manager.getLoreSelectedIndex().getOrDefault(player.getUniqueId(), 0);

        if (event.isShiftClick() && event.isLeftClick()) {
            // Add new line
            player.closeInventory();
            manager.setPendingInput(player, KitEditorPendingInput.LORE_ADD);
            player.sendMessage(CC.t("&eType the value in chat. &eType &cCancel &eto cancel"));
            return;
        }

        if (event.isShiftClick() && event.isRightClick()) {
            // Delete selected line
            if (selected >= 0 && selected < lore.size()) {
                lore.remove(selected);
                if (!lore.isEmpty()) {
                    selected = Math.min(selected, lore.size() - 1);
                } else {
                    selected = 0;
                }
                manager.getLoreSelectedIndex().put(player.getUniqueId(), selected);
                manager.getFileManager().setKitItemLore(kitName, lore);
                manager.openKitConfigMenu(player, kitName);
            }
            return;
        }

        if (!event.isShiftClick() && event.isLeftClick()) {
            // Scroll up
            if (selected > 0) {
                selected--;
                manager.getLoreSelectedIndex().put(player.getUniqueId(), selected);
                manager.openKitConfigMenu(player, kitName);
            }
            return;
        }

        if (!event.isShiftClick() && event.isRightClick()) {
            // Scroll down
            if (selected < lore.size() - 1) {
                selected++;
                manager.getLoreSelectedIndex().put(player.getUniqueId(), selected);
                manager.openKitConfigMenu(player, kitName);
            }
            return;
        }

        if (event.getClick() == org.bukkit.event.inventory.ClickType.MIDDLE) {
            // Edit selected line
            if (selected >= 0 && selected < lore.size()) {
                final String currentLine = lore.get(selected);
                player.closeInventory();
                manager.setPendingInput(player, KitEditorPendingInput.LORE_EDIT);
                player.sendMessage(CC.t("&eType the new lore line text or &fPress here to copy current text"));

                FancyMessage fancyMessage = new FancyMessage("&a[Copy]")
                        .tooltip("&eClick to copy current text")
                        .suggest(currentLine);
                fancyMessage.send(player);
            }
            return;
        }

        if (event.getClick() == org.bukkit.event.inventory.ClickType.DROP) {
            // Toggle preview mode
            boolean preview = manager.getLorePreviewMode().getOrDefault(player.getUniqueId(), false);
            preview = !preview;
            manager.getLorePreviewMode().put(player.getUniqueId(), preview);
            manager.openKitConfigMenu(player, kitName);
            return;
        }
    }

    // --- DECORATIVE EDIT MAIN ---

    private void handleDecorativeEditMainClick(InventoryClickEvent event, Player player, Inventory clickedInventory, Inventory topInventory) {
        if (clickedInventory == player.getInventory()) {
            event.setCancelled(true);
            return;
        }

        int slot = event.getRawSlot();

        // Kit items cannot be moved
        String kitName = findKitBySlot(slot, topInventory);
        if (kitName != null) {
            event.setCancelled(true);
            return;
        }

        // Existing decorative items can be removed (shift click)
        Map<Integer, ItemStack> decorativeItems = manager.getFileManager().getGlobalDecorativeItems();
        if (decorativeItems.containsKey(slot)) {
            if (event.isShiftClick()) {
                // Remove
                topInventory.setItem(slot, null);
                event.setCancelled(true);
            } else {
                event.setCancelled(true);
            }
            return;
        }

        // Empty slots can receive items - allow placement
        // Don't cancel - let the item be placed
    }

    private void handleDecorativeEditMainClose(Player player) {
        Inventory inventory = manager.getOpenInventory(player);
        if (inventory != null) {
            // Save decorative items
            Map<Integer, ItemStack> items = new HashMap<>();
            Set<Integer> kitSlots = new HashSet<>();
            for (Kit kit : manager.getEditableKits()) {
                int ds = manager.getFileManager().getKitDisplaySlot(kit.getName());
                if (ds >= 0) kitSlots.add(ds);
            }

            for (int i = 0; i < inventory.getSize(); i++) {
                ItemStack item = inventory.getItem(i);
                if (item == null || item.getType() == Material.AIR) continue;
                if (kitSlots.contains(i)) continue; // Don't save kit items
                items.put(i, item.clone());
            }
            manager.getFileManager().setGlobalDecorativeItems(items);
        }

        manager.clearContext(player);
        manager.clearOpenInventory(player);

        player.sendMessage(CC.t("&aSaved!"));

        // Reopen admin menu
        Bukkit.getScheduler().runTaskLater(manager.getInstance(), () -> {
            manager.openAdminMenu(player);
        }, 1L);
    }

    // --- DECORATIVE EDIT KIT ---

    private void handleDecorativeEditKitClick(InventoryClickEvent event, Player player, Inventory clickedInventory, Inventory topInventory) {
        if (clickedInventory == player.getInventory()) {
            event.setCancelled(true);
            return;
        }

        int slot = event.getRawSlot();
        String kitName = manager.getContextKit(player);
        if (kitName == null) return;

        // Action items / armor items / refill items cannot be moved
        // Only allow placing on truly empty slots and removing existing decorative items

        Map<Integer, ItemStack> decorativeItems = manager.getFileManager().getKitDecorativeItems(kitName);
        if (decorativeItems.containsKey(slot)) {
            if (event.isShiftClick()) {
                topInventory.setItem(slot, null);
                event.setCancelled(true);
            } else {
                event.setCancelled(true);
            }
            return;
        }

        // Check if this slot is used by action items, armor, or refill
        int saveSlot = manager.getFileManager().getActionSlot(kitName, "save");
        int cancelSlot = manager.getFileManager().getActionSlot(kitName, "cancel");
        int copySlot = manager.getFileManager().getActionSlot(kitName, "copy");

        if (slot == saveSlot || slot == cancelSlot || slot == copySlot) {
            event.setCancelled(true);
            return;
        }

        // Check armor slots
        if (slot == manager.getFileManager().getArmorSlot(kitName, "helmet") ||
                slot == manager.getFileManager().getArmorSlot(kitName, "chestplate") ||
                slot == manager.getFileManager().getArmorSlot(kitName, "leggings") ||
                slot == manager.getFileManager().getArmorSlot(kitName, "boots")) {
            event.setCancelled(true);
            return;
        }

        // Check refill items
        Map<Integer, ItemStack> refillItems = manager.getFileManager().getRefillItems(kitName);
        if (refillItems.containsKey(slot)) {
            event.setCancelled(true);
            return;
        }

        // Empty slot - allow placement
    }

    private void handleDecorativeEditKitClose(Player player) {
        Inventory inventory = manager.getOpenInventory(player);
        String kitName = manager.getContextKit(player);

        if (inventory != null && kitName != null) {
            Map<Integer, ItemStack> items = new HashMap<>();
            Set<Integer> reserved = getKitReservedSlots(kitName);

            for (int i = 0; i < inventory.getSize(); i++) {
                if (reserved.contains(i)) continue;
                ItemStack item = inventory.getItem(i);
                if (item == null || item.getType() == Material.AIR) continue;
                items.put(i, item.clone());
            }
            manager.getFileManager().setKitDecorativeItems(kitName, items);
        }

        manager.clearContext(player);
        manager.clearOpenInventory(player);

        Bukkit.getScheduler().runTaskLater(manager.getInstance(), () -> {
            if (kitName != null) manager.openKitConfigMenu(player, kitName);
        }, 1L);
    }

    private Set<Integer> getKitReservedSlots(String kitName) {
        Set<Integer> reserved = new HashSet<>();
        int saveSlot = manager.getFileManager().getActionSlot(kitName, "save");
        int cancelSlot = manager.getFileManager().getActionSlot(kitName, "cancel");
        int copySlot = manager.getFileManager().getActionSlot(kitName, "copy");
        if (saveSlot >= 0) reserved.add(saveSlot);
        if (cancelSlot >= 0) reserved.add(cancelSlot);
        if (copySlot >= 0) reserved.add(copySlot);

        int helm = manager.getFileManager().getArmorSlot(kitName, "helmet");
        int chest = manager.getFileManager().getArmorSlot(kitName, "chestplate");
        int legs = manager.getFileManager().getArmorSlot(kitName, "leggings");
        int boots = manager.getFileManager().getArmorSlot(kitName, "boots");
        if (helm >= 0) reserved.add(helm);
        if (chest >= 0) reserved.add(chest);
        if (legs >= 0) reserved.add(legs);
        if (boots >= 0) reserved.add(boots);

        reserved.addAll(manager.getFileManager().getRefillItems(kitName).keySet());
        return reserved;
    }

    // --- SLOT PICKER ---

    private void handleSlotPickerClick(InventoryClickEvent event, Player player, Inventory clickedInventory, Inventory topInventory) {
        event.setCancelled(true);

        if (clickedInventory == null || clickedInventory == player.getInventory()) return;

        int slot = event.getRawSlot();
        String kitName = manager.getContextKit(player);
        if (kitName == null) return;

        // Only allow clicking on cycling panes
        ItemStack item = topInventory.getItem(slot);
        if (item == null || !manager.isCyclingPane(item)) return;

        // Check that the slot isn't already occupied by another kit's display
        for (Kit kit : manager.getEditableKits()) {
            if (kit.getName().equalsIgnoreCase(kitName)) continue;
            int ds = manager.getFileManager().getKitDisplaySlot(kit.getName());
            if (ds == slot) return; // Can't place here
        }

        // Also check decorative items
        Map<Integer, ItemStack> decorativeItems = manager.getFileManager().getGlobalDecorativeItems();
        if (decorativeItems.containsKey(slot)) return;

        manager.getFileManager().setKitDisplaySlot(kitName, slot);

        manager.cancelCyclingTask(player);
        manager.clearContext(player);
        manager.clearOpenInventory(player);
        player.closeInventory();

        Bukkit.getScheduler().runTaskLater(manager.getInstance(), () -> {
            manager.openKitConfigMenu(player, kitName);
        }, 1L);
    }

    // --- ARMOR EDIT ---

    private void handleArmorEditClick(InventoryClickEvent event, Player player, Inventory clickedInventory, Inventory topInventory) {
        event.setCancelled(true);

        if (clickedInventory == null || clickedInventory == player.getInventory()) return;

        int slot = event.getRawSlot();
        String kitName = manager.getContextKit(player);
        if (kitName == null) return;

        ItemStack item = topInventory.getItem(slot);

        // Click on cycling pane
        if (item != null && manager.isCyclingPane(item)) {
            String piece;
            if (event.isShiftClick() && event.isLeftClick()) {
                piece = "leggings";
            } else if (event.isShiftClick() && event.isRightClick()) {
                piece = "boots";
            } else if (!event.isShiftClick() && event.isLeftClick()) {
                piece = "helmet";
            } else if (!event.isShiftClick() && event.isRightClick()) {
                piece = "chestplate";
            } else {
                return;
            }

            // Remove old slot's bedrock if set
            int oldSlot = manager.getFileManager().getArmorSlot(kitName, piece);
            if (oldSlot >= 0 && oldSlot < topInventory.getSize()) {
                topInventory.setItem(oldSlot, manager.createColoredPane(0));
            }

            // Set new slot
            manager.getFileManager().setArmorSlot(kitName, piece, slot);
            topInventory.setItem(slot, createArmorMarker(piece));
            player.updateInventory();
            return;
        }

        // Click on bedrock (armor marker) - remove it
        if (item != null && item.getType() == Material.BEDROCK) {
            String piece = getArmorPieceFromItem(item);
            if (piece != null) {
                manager.getFileManager().setArmorSlot(kitName, piece, -1);
                topInventory.setItem(slot, manager.createColoredPane(0));
                player.updateInventory();
            }
            return;
        }
    }

    private ItemStack createArmorMarker(String piece) {
        String name;
        switch (piece) {
            case "helmet":
                name = "&c&lHelmet";
                break;
            case "chestplate":
                name = "&c&lChestplate";
                break;
            case "leggings":
                name = "&c&lLeggings";
                break;
            case "boots":
                name = "&c&lBoots";
                break;
            default:
                name = "&c&lArmor";
        }
        ItemBuilder builder = new ItemBuilder(Material.BEDROCK);
        builder.setName(name);
        builder.setLore("&7", "&eClick to remove!", "&7");
        return builder.toItemStack();
    }

    private String getArmorPieceFromItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        String name = item.getItemMeta().getDisplayName();
        if (name == null) return null;
        if (name.contains("Helmet")) return "helmet";
        if (name.contains("Chestplate")) return "chestplate";
        if (name.contains("Leggings")) return "leggings";
        if (name.contains("Boots")) return "boots";
        return null;
    }

    private void handleArmorEditClose(Player player) {
        manager.cancelCyclingTask(player);
        String kitName = manager.getContextKit(player);
        manager.clearContext(player);
        manager.clearOpenInventory(player);

        // Armor slots are already saved on click, just reopen config
        Bukkit.getScheduler().runTaskLater(manager.getInstance(), () -> {
            if (kitName != null) manager.openKitConfigMenu(player, kitName);
        }, 1L);
    }

    // --- REFILL EDIT ---

    private void handleRefillEditClick(InventoryClickEvent event, Player player, Inventory clickedInventory, Inventory topInventory) {
        String kitName = manager.getContextKit(player);
        if (kitName == null) return;

        KitEditorItemPick pick = manager.getPendingPick(player);

        if (pick != null && pick.isActive()) {
            // Waiting for player to click an item in their inventory
            if (clickedInventory == player.getInventory()) {
                event.setCancelled(true);
                ItemStack clickedItem = event.getCurrentItem();
                if (clickedItem == null || clickedItem.getType() == Material.AIR) return;

                int targetSlot = pick.getSlot();

                if (pick.getMode() == KitEditorItemPick.PickMode.REFILL) {
                    String type;
                    ItemStack itemToSave;
                    if (event.isRightClick()) {
                        // fill_inventory - amount 1
                        type = "fill_inventory";
                        itemToSave = clickedItem.clone();
                        itemToSave.setAmount(1);
                    } else {
                        // fill_exact
                        type = "fill_exact";
                        itemToSave = clickedItem.clone();
                    }

                    manager.getFileManager().setRefillItem(kitName, targetSlot, itemToSave, type);
                    topInventory.setItem(targetSlot, itemToSave.clone());
                }

                pick.setActive(false);
                manager.clearPendingPick(player);
                player.updateInventory();

                // Reopen refill edit
                manager.clearContext(player);
                manager.clearOpenInventory(player);
                player.closeInventory();
                Bukkit.getScheduler().runTaskLater(manager.getInstance(), () -> {
                    manager.openRefillEdit(player, kitName);
                }, 1L);
            } else {
                event.setCancelled(true);
            }
            return;
        }

        event.setCancelled(true);

        if (clickedInventory == null || clickedInventory == player.getInventory()) return;

        int slot = event.getRawSlot();
        ItemStack item = topInventory.getItem(slot);

        if (item != null && manager.isCyclingPane(item)) {
            // Click on cycling pane - start item pick
            manager.setPendingPick(player, new KitEditorItemPick(KitEditorItemPick.PickMode.REFILL, kitName, slot));
            player.closeInventory();
            player.sendMessage(CC.t("&a&lCLICK AN ITEM IN YOUR INVENTORY BUT READ THIS:"));
            player.sendMessage(CC.t("&eLeft Click to select that item (it will copy the amount, name, lore, etc.)"));
            player.sendMessage(CC.t("&eRight Click to make clicking that item fill your inventory with it (it will copy the amount, name, lore, etc.)"));

            // Set context back so we still handle clicks
            manager.setContext(player, KitEditorContext.REFILL_EDIT);
        }
    }

    private void handleRefillEditClose(Player player) {
        KitEditorItemPick pick = manager.getPendingPick(player);
        if (pick != null && pick.isActive()) {
            // Closed while waiting for pick - cancel the pick
            manager.clearPendingPick(player);
        }

        manager.cancelCyclingTask(player);
        String kitName = manager.getContextKit(player);
        manager.clearContext(player);
        manager.clearOpenInventory(player);

        Bukkit.getScheduler().runTaskLater(manager.getInstance(), () -> {
            if (kitName != null) manager.openKitConfigMenu(player, kitName);
        }, 1L);
    }

    // --- ACTION EDIT ---

    private void handleActionEditClick(InventoryClickEvent event, Player player, Inventory clickedInventory, Inventory topInventory) {
        String kitName = manager.getContextKit(player);
        if (kitName == null) return;

        KitEditorItemPick pick = manager.getPendingPick(player);

        if (pick != null && pick.isActive()) {
            if (clickedInventory == player.getInventory()) {
                event.setCancelled(true);
                ItemStack clickedItem = event.getCurrentItem();
                if (clickedItem == null || clickedItem.getType() == Material.AIR) return;

                int targetSlot = pick.getSlot();

                if (pick.getMode() == KitEditorItemPick.PickMode.ACTION) {
                    if (event.isShiftClick() && event.isLeftClick()) {
                        // Copy item - name only, material
                        ItemStack copyItem = clickedItem.clone();
                        manager.getFileManager().setActionItem(kitName, "copy", copyItem);
                        manager.getFileManager().setActionSlot(kitName, "copy", targetSlot);
                        topInventory.setItem(targetSlot, copyItem);
                    } else if (event.isRightClick()) {
                        // Cancel item
                        ItemStack cancelItem = clickedItem.clone();
                        manager.getFileManager().setActionItem(kitName, "cancel", cancelItem);
                        manager.getFileManager().setActionSlot(kitName, "cancel", targetSlot);
                        topInventory.setItem(targetSlot, cancelItem);
                    } else if (event.isLeftClick()) {
                        // Save item
                        ItemStack saveItem = clickedItem.clone();
                        manager.getFileManager().setActionItem(kitName, "save", saveItem);
                        manager.getFileManager().setActionSlot(kitName, "save", targetSlot);
                        topInventory.setItem(targetSlot, saveItem);
                    }
                }

                pick.setActive(false);
                manager.clearPendingPick(player);
                player.updateInventory();

                manager.clearContext(player);
                manager.clearOpenInventory(player);
                player.closeInventory();
                Bukkit.getScheduler().runTaskLater(manager.getInstance(), () -> {
                    manager.openActionEdit(player, kitName);
                }, 1L);
            } else {
                event.setCancelled(true);
            }
            return;
        }

        event.setCancelled(true);

        if (clickedInventory == null || clickedInventory == player.getInventory()) return;

        int slot = event.getRawSlot();
        ItemStack item = topInventory.getItem(slot);

        if (item != null && manager.isCyclingPane(item)) {
            manager.setPendingPick(player, new KitEditorItemPick(KitEditorItemPick.PickMode.ACTION, kitName, slot));
            player.closeInventory();
            player.sendMessage(CC.t("&a&lCLICK AN ITEM IN YOUR INVENTORY BUT READ THIS:"));
            player.sendMessage(CC.t("&eLeft Click to set this item as a Save Kit item (Name, lore, etc. will be saved)"));
            player.sendMessage(CC.t("&eRight Click to set this item as a Cancel item (Name, lore, etc. will be saved)"));
            player.sendMessage(CC.t("&eShift + Left Click to set this item as a Copy Kit item (Name will be saved)"));

            manager.setContext(player, KitEditorContext.ACTION_EDIT);
        }
    }

    private void handleActionEditClose(Player player) {
        KitEditorItemPick pick = manager.getPendingPick(player);
        if (pick != null && pick.isActive()) {
            manager.clearPendingPick(player);
        }

        manager.cancelCyclingTask(player);
        String kitName = manager.getContextKit(player);
        manager.clearContext(player);
        manager.clearOpenInventory(player);

        Bukkit.getScheduler().runTaskLater(manager.getInstance(), () -> {
            if (kitName != null) manager.openKitConfigMenu(player, kitName);
        }, 1L);
    }
}
