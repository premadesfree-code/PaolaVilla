package me.keano.azurite.modules.enderchest;

import me.keano.azurite.HCF;
import me.keano.azurite.utils.CC;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Copyright (c) 2025. Keano
 * Use or redistribution of source or file is
 * only permitted if given explicit permission.
 */
public class EnderChestListener implements Listener {

    private final HCF instance;
    private final EnderChestManager manager;

    private final Map<UUID, EnderChestSession> sessions;
    private final Map<UUID, Consumer<String>> chatInputs;
    private final Set<UUID> transitioning;

    public EnderChestListener(HCF instance, EnderChestManager manager) {
        this.instance = instance;
        this.manager = manager;
        this.sessions = new HashMap<>();
        this.chatInputs = new HashMap<>();
        this.transitioning = new HashSet<>();
        Bukkit.getPluginManager().registerEvents(this, instance);
    }

    public void registerChatInput(Player player, Consumer<String> callback) {
        chatInputs.put(player.getUniqueId(), callback);
    }

    @SuppressWarnings("deprecation")
    public void openEnderChest(Player player) {
        EnderChestData data = manager.getPlayerData(player.getUniqueId());
        int size = manager.getEcSize();
        Inventory inv = Bukkit.createInventory(null, size, CC.t(manager.getEcTitle()));

        for (int i = 0; i < size && i < data.getContents().length; i++) {
            ItemStack content = data.getContents()[i];
            if (content != null && content.getType() != Material.AIR) {
                inv.setItem(i, content);
            }
        }

        for (int i = 0; i < size; i++) {
            ItemStack rowItem = manager.getRowItemForSlot(player, i);
            if (rowItem != null) {
                inv.setItem(i, rowItem);
            }
        }

        UUID uuid = player.getUniqueId();
        transitioning.add(uuid);
        player.openInventory(inv);
        sessions.put(uuid, new EnderChestSession(EnderChestSession.Type.PLAYER_EC));
        transitioning.remove(uuid);
    }

    @SuppressWarnings("deprecation")
    public void openEditor(Player player) {
        Inventory inv = Bukkit.createInventory(null, 45, CC.t("&8EnderChest Editor"));
        fillBorders(inv);

        inv.setItem(1, createItem(Material.HOPPER, "&e&lEdit EnderChest Rows",
                "&7", "&6Current: &f" + manager.getEcRows(), "&7", "&eClick to edit"));
        inv.setItem(2, createItem(Material.BARRIER, "&c&lDelete Rows",
                "&7", "&6Current: " + manager.getRows().size(), "&7", "&eClick to toggle"));
        inv.setItem(4, createItem(Material.NETHER_STAR, "&a&lCreate Rows"));
        inv.setItem(6, createItem(Material.SIGN, "&e&lEnderChest Menu Title",
                "&7", "&6Current: " + manager.getEcTitle(), "&7", "&eClick to edit"));

        for (int i = 0; i < manager.getRows().size(); i++) {
            int slot = 10 + i;
            if (slot >= 45) break;
            EnderChestRow row = manager.getRows().get(i);
            inv.setItem(slot, row.getItemStack());
        }

        UUID uuid = player.getUniqueId();
        transitioning.add(uuid);
        player.openInventory(inv);
        sessions.put(uuid, new EnderChestSession(EnderChestSession.Type.EDITOR));
        transitioning.remove(uuid);
    }

    @SuppressWarnings("deprecation")
    public void openRowEditor(Player player, int rowIndex) {
        EnderChestRow row = manager.getRow(rowIndex);
        if (row == null) return;

        Inventory inv = Bukkit.createInventory(null, 27, CC.t("&8Editing - " + row.getName()));

        fillRowEditorBorders(inv);

        ItemStack rowItem = row.getItemStack();
        ItemMeta rowItemMeta = rowItem.getItemMeta();
        java.util.List<String> rowItemLore = new java.util.ArrayList<>();
        rowItemLore.add(CC.t("&7"));
        rowItemLore.add(CC.t("&eClick to edit"));
        rowItemMeta.setLore(rowItemLore);
        rowItem.setItemMeta(rowItemMeta);
        inv.setItem(10, rowItem);

        inv.setItem(11, createItem(Material.HOPPER, "&e&lEdit Row Item Name",
                "&7", "&6Current: &f" + row.getItemName(),
                "&7", "&eClick to edit."));
        inv.setItem(12, createItem(Material.QUARTZ, "&e&lEdit Permissions", "&7", "&eClick to edit."));
        inv.setItem(13, createItem(Material.ENDER_CHEST, "&e&lEdit EnderChest Rows", "&7", "&eClick to edit"));
        inv.setItem(14, createItem(Material.BARRIER, "&e&lUndo Rows", "&7", "&eClick to Undo.", "&7"));
        inv.setItem(15, buildLoreItem(row, 0, false));
        inv.setItem(16, createItem(Material.BED, "&c&lBack", "&7", "&eClick to go back."));

        EnderChestSession session = new EnderChestSession(EnderChestSession.Type.ROW_EDITOR, rowIndex, 0, false);
        UUID uuid = player.getUniqueId();
        transitioning.add(uuid);
        player.openInventory(inv);
        sessions.put(uuid, session);
        transitioning.remove(uuid);
    }

    @SuppressWarnings("deprecation")
    public void openPermissionsEditor(Player player, int rowIndex) {
        EnderChestRow row = manager.getRow(rowIndex);
        if (row == null) return;

        Inventory inv = Bukkit.createInventory(null, 45, CC.t("&8Permissions Editor"));
        fillBorders(inv);
        inv.setItem(4, createItem(Material.NETHER_STAR, "&a&lCreate Permission"));
        inv.setItem(5, createBorderPane());

        for (int i = 0; i < row.getPermissions().size(); i++) {
            int slot = 10 + i;
            if (slot >= 45) break;
            String perm = row.getPermissions().get(i);
            inv.setItem(slot, createItem(Material.PAPER, "&c" + perm,
                    "&7", "&eClick to Remove!", "&7"));
        }

        UUID uuid = player.getUniqueId();
        transitioning.add(uuid);
        player.openInventory(inv);
        sessions.put(uuid, new EnderChestSession(
                EnderChestSession.Type.PERMISSIONS_EDITOR, rowIndex));
        transitioning.remove(uuid);
    }

    @SuppressWarnings("deprecation")
    public void openSlotPicker(Player player, int rowIndex) {
        EnderChestRow row = manager.getRow(rowIndex);
        if (row == null) return;

        int size = manager.getEcSize();
        Inventory inv = Bukkit.createInventory(null, size, CC.t(manager.getEcTitle()));

        for (int i = 0; i < size; i++) {
            EnderChestRow slotRow = manager.getRowBySlot(i);
            if (slotRow != null && slotRow != row && slotRow.isAssigned()
                    && i >= slotRow.getPos1() && i <= slotRow.getPos2()) {
                inv.setItem(i, slotRow.getItemStack());
            } else if (slotRow != row || !row.isAssigned() || i < row.getPos1() || i > row.getPos2()) {
                inv.setItem(i, createAnimatedPane("&e&lLeft Click to set Pos1"));
            }
        }

        UUID uuid = player.getUniqueId();
        transitioning.add(uuid);
        player.openInventory(inv);
        sessions.put(uuid, new EnderChestSession(
                EnderChestSession.Type.SLOT_PICKER, rowIndex, -1));
        transitioning.remove(uuid);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;
        Player player = (Player) e.getWhoClicked();
        UUID uuid = player.getUniqueId();

        EnderChestSession session = sessions.get(uuid);
        if (session == null) return;

        if (e.getClickedInventory() == null) {
            if (session.type != EnderChestSession.Type.PLAYER_EC) {
                e.setCancelled(true);
            }
            return;
        }

        boolean topInv = e.getClickedInventory().equals(e.getView().getTopInventory());

        if (session.itemPickMode && session.type == EnderChestSession.Type.ROW_EDITOR && !topInv) {
            e.setCancelled(true);
            ItemStack clicked = e.getCurrentItem();
            if (clicked == null || clicked.getType() == Material.AIR) return;
            session.itemPickMode = false;
            EnderChestRow row = manager.getRow(session.rowIndex);
            if (row != null) {
                row.setItemMaterial(clicked.getType());
                row.setItemData(clicked.getDurability());
                manager.saveRow(row);
                player.sendMessage(CC.t("&aRow item updated!"));
                openRowEditor(player, session.rowIndex);
            }
            return;
        }

        switch (session.type) {
            case PLAYER_EC:
                handlePlayerEcClick(e, player, session);
                break;
            case EDITOR:
                e.setCancelled(true);
                if (topInv) handleEditorClick(e, player, session);
                break;
            case ROW_EDITOR:
                e.setCancelled(true);
                if (topInv) handleRowEditorClick(e, player, session);
                break;
            case PERMISSIONS_EDITOR:
                e.setCancelled(true);
                if (topInv) handlePermissionsClick(e, player, session);
                break;
            case SLOT_PICKER:
                e.setCancelled(true);
                if (topInv) handleSlotPickerClick(e, player, session);
                break;
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent e) {
        if (!(e.getWhoClicked() instanceof Player)) return;
        UUID uuid = e.getWhoClicked().getUniqueId();
        EnderChestSession session = sessions.get(uuid);
        if (session == null) return;

        if (session.type == EnderChestSession.Type.PLAYER_EC) {
            int invSize = e.getView().getTopInventory().getSize();
            for (int slot : e.getRawSlots()) {
                if (slot < invSize && manager.isSlotLocked((Player) e.getWhoClicked(), slot)) {
                    e.setCancelled(true);
                    return;
                }
            }
        } else {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent e) {
        if (!(e.getPlayer() instanceof Player)) return;
        Player player = (Player) e.getPlayer();
        UUID uuid = player.getUniqueId();
        if (transitioning.contains(uuid)) return;
        EnderChestSession session = sessions.remove(uuid);
        if (session == null) return;

        if (session.type == EnderChestSession.Type.PLAYER_EC) {
            Inventory inv = e.getView().getTopInventory();
            EnderChestData data = manager.getPlayerData(uuid);
            ItemStack[] contents = new ItemStack[manager.getEcSize()];
            for (int i = 0; i < contents.length && i < inv.getSize(); i++) {
                ItemStack item = inv.getItem(i);
                if (manager.isSlotLocked(player, i)) {
                    contents[i] = data.getContents().length > i ? data.getContents()[i] : null;
                } else {
                    contents[i] = item;
                }
            }
            data.setContents(contents);
            manager.savePlayerData(uuid);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncPlayerChatEvent e) {
        Player player = e.getPlayer();
        Consumer<String> callback = chatInputs.remove(player.getUniqueId());
        if (callback == null) return;
        e.setCancelled(true);
        String msg = e.getMessage();
        Bukkit.getScheduler().runTask(instance, () -> callback.accept(msg));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent e) {
        UUID uuid = e.getPlayer().getUniqueId();
        chatInputs.remove(uuid);
        sessions.remove(uuid);
        transitioning.remove(uuid);
    }

    private void handlePlayerEcClick(InventoryClickEvent e, Player player, EnderChestSession session) {
        int slot = e.getRawSlot();
        int invSize = e.getView().getTopInventory().getSize();

        if (slot < invSize) {
            if (manager.isSlotLocked(player, slot)) {
                e.setCancelled(true);
                return;
            }
        }

        if (e.isShiftClick() && e.getClickedInventory().equals(player.getInventory())) {
            ItemStack shifted = e.getCurrentItem();
            if (shifted == null || shifted.getType() == Material.AIR) return;
            Inventory top = e.getView().getTopInventory();
            int targetSlot = -1;
            for (int i = 0; i < top.getSize(); i++) {
                ItemStack existing = top.getItem(i);
                if (existing != null && existing.isSimilar(shifted) && existing.getAmount() < existing.getMaxStackSize()) {
                    targetSlot = i;
                    break;
                }
            }
            if (targetSlot == -1) {
                for (int i = 0; i < top.getSize(); i++) {
                    ItemStack existing = top.getItem(i);
                    if (existing == null || existing.getType() == Material.AIR) {
                        targetSlot = i;
                        break;
                    }
                }
            }
            if (targetSlot != -1 && manager.isSlotLocked(player, targetSlot)) {
                e.setCancelled(true);
            }
        }
    }

    private void handleEditorClick(InventoryClickEvent e, Player player, EnderChestSession session) {
        int slot = e.getSlot();

        if (slot == 1) {
            player.closeInventory();
            player.sendMessage(CC.t("&eType the number of rows (1-6) in chat. Type &cCancel &eto cancel"));
            registerChatInput(player, msg -> {
                if (msg.equalsIgnoreCase("cancel")) {
                    openEditor(player);
                    return;
                }
                try {
                    int rows = Integer.parseInt(msg);
                    if (rows < 1 || rows > 6) {
                        player.sendMessage(CC.t("&cInvalid number. Must be 1-6."));
                        openEditor(player);
                        return;
                    }
                    manager.setEcRows(rows);
                    manager.saveSettings();
                    openEditor(player);
                } catch (NumberFormatException ex) {
                    player.sendMessage(CC.t("&cInvalid number. Must be 1-6."));
                    openEditor(player);
                }
            });
        } else if (slot == 2) {
            session.deleteMode = !session.deleteMode;
            player.sendMessage(CC.t(session.deleteMode ? "&cDelete mode ON" : "&aDelete mode OFF"));
            openEditor(player);
        } else if (slot == 4) {
            player.closeInventory();
            player.sendMessage(CC.t("&ePlease type a name for this new row, or type &cCancel &eto cancel."));
            registerChatInput(player, msg -> {
                if (msg.equalsIgnoreCase("cancel")) {
                    openEditor(player);
                    return;
                }
                manager.createRow(msg);
                openEditor(player);
            });
        } else if (slot == 6) {
            player.closeInventory();
            player.sendMessage(CC.t("&eType the value in chat. &eType &cCancel &eto cancel."));
            registerChatInput(player, msg -> {
                if (msg.equalsIgnoreCase("cancel")) {
                    openEditor(player);
                    return;
                }
                manager.setEcTitle(msg);
                manager.saveSettings();
                openEditor(player);
            });
        } else if (slot >= 10) {
            int rowIndex = slot - 10;
            if (rowIndex < manager.getRows().size()) {
                if (session.deleteMode) {
                    manager.deleteRow(rowIndex);
                    openEditor(player);
                } else {
                    openRowEditor(player, rowIndex);
                }
            }
        }
    }

    private void handleRowEditorClick(InventoryClickEvent e, Player player, EnderChestSession session) {
        int slot = e.getSlot();
        EnderChestRow row = manager.getRow(session.rowIndex);
        if (row == null) {
            player.closeInventory();
            return;
        }

        if (slot == 10) {
            session.itemPickMode = true;
            player.sendMessage(CC.t("&eClick an item in your inventory to set it as the row item."));
        } else if (slot == 11) {
            player.closeInventory();
            player.sendMessage(CC.t("&eType the value in chat. &eType &cCancel &eto cancel."));
            registerChatInput(player, msg -> {
                if (msg.equalsIgnoreCase("cancel")) {
                    openRowEditor(player, session.rowIndex);
                    return;
                }
                row.setItemName(msg);
                manager.saveRow(row);
                openRowEditor(player, session.rowIndex);
            });
        } else if (slot == 12) {
            openPermissionsEditor(player, session.rowIndex);
        } else if (slot == 13) {
            openSlotPicker(player, session.rowIndex);
        } else if (slot == 14) {
            row.setPos1(-1);
            row.setPos2(-1);
            manager.saveRow(row);
            player.sendMessage(CC.t("&aSlot assignment removed."));
            openRowEditor(player, session.rowIndex);
        } else if (slot == 15) {
            handleLoreClick(e, player, session, row);
        } else if (slot == 16) {
            openEditor(player);
        }
    }

    private void handleLoreClick(InventoryClickEvent e, Player player, EnderChestSession session, EnderChestRow row) {
        boolean shift = e.isShiftClick();
        boolean left = e.isLeftClick();
        boolean right = e.isRightClick();
        boolean middle = e.getClick().name().equals("MIDDLE");
        boolean drop = e.getClick().name().equals("DROP");

        if (drop) {
            session.previewMode = !session.previewMode;
            refreshRowEditor(player, session, row);
            return;
        }

        if (middle) {
            if (row.getLore().isEmpty()) return;
            player.closeInventory();
            player.sendMessage(CC.t("&eType the new lore line text or &fPress here to copy current text"));
            registerChatInput(player, msg -> {
                row.getLore().set(session.selectedLoreLine, msg);
                manager.saveRow(row);
                openRowEditor(player, session.rowIndex);
            });
            return;
        }

        if (shift && left) {
            player.closeInventory();
            player.sendMessage(CC.t("&eType the value in chat. &eType &cCancel &eto cancel."));
            registerChatInput(player, msg -> {
                if (msg.equalsIgnoreCase("cancel")) {
                    openRowEditor(player, session.rowIndex);
                    return;
                }
                row.getLore().add(msg);
                manager.saveRow(row);
                openRowEditor(player, session.rowIndex);
            });
            return;
        }

        if (shift && right) {
            if (row.getLore().isEmpty()) return;
            if (session.selectedLoreLine >= row.getLore().size())
                session.selectedLoreLine = 0;
            row.getLore().remove(session.selectedLoreLine);
            if (session.selectedLoreLine > 0) session.selectedLoreLine--;
            manager.saveRow(row);
            refreshRowEditor(player, session, row);
            return;
        }

        if (left) {
            if (row.getLore().isEmpty()) return;
            session.selectedLoreLine = Math.max(0, session.selectedLoreLine - 1);
            refreshRowEditor(player, session, row);
            return;
        }

        if (right) {
            if (row.getLore().isEmpty()) return;
            session.selectedLoreLine = Math.min(row.getLore().size() - 1, session.selectedLoreLine + 1);
            refreshRowEditor(player, session, row);
        }
    }

    private void handlePermissionsClick(InventoryClickEvent e, Player player, EnderChestSession session) {
        int slot = e.getSlot();
        EnderChestRow row = manager.getRow(session.rowIndex);
        if (row == null) return;

        if (slot == 4) {
            player.closeInventory();
            player.sendMessage(CC.t("&eType the value in chat. &eType &cCancel &eto cancel."));
            registerChatInput(player, msg -> {
                if (msg.equalsIgnoreCase("cancel")) {
                    openPermissionsEditor(player, session.rowIndex);
                    return;
                }
                row.getPermissions().add("enderchest." + msg);
                manager.saveRow(row);
                openPermissionsEditor(player, session.rowIndex);
            });
        } else if (slot >= 10) {
            int permIndex = slot - 10;
            if (permIndex < row.getPermissions().size()) {
                row.getPermissions().remove(permIndex);
                manager.saveRow(row);
                openPermissionsEditor(player, session.rowIndex);
            }
        }
    }

    private void handleSlotPickerClick(InventoryClickEvent e, Player player, EnderChestSession session) {
        int slot = e.getSlot();
        EnderChestRow row = manager.getRow(session.rowIndex);
        if (row == null) return;

        int invSize = e.getView().getTopInventory().getSize();
        if (slot < 0 || slot >= invSize) return;

        if (e.isLeftClick()) {
            if (manager.getRowBySlot(slot) != null && manager.getRowBySlot(slot) != row) return;
            session.pos1 = slot;
            row.setPos1(slot);
            for (int i = 0; i < invSize; i++) {
                EnderChestRow slotRow = manager.getRowBySlot(i);
                if (slotRow != null && slotRow != row && slotRow.isAssigned()
                        && i >= slotRow.getPos1() && i <= slotRow.getPos2()) continue;
                if (slotRow != row || i != slot) {
                    ItemStack item = e.getView().getTopInventory().getItem(i);
                    if (item != null && item.getType() == Material.STAINED_GLASS_PANE) {
                        e.getView().getTopInventory().setItem(i, createAnimatedPane("&e&lRight Click to set Pos2"));
                    }
                }
            }
        } else if (e.isRightClick()) {
            if (session.pos1 < 0) {
                player.sendMessage(CC.t("&cSet Pos1 first."));
                return;
            }
            int pos1 = Math.min(session.pos1, slot);
            int pos2 = Math.max(session.pos1, slot);
            row.setPos1(pos1);
            row.setPos2(pos2);
            manager.saveRow(row);
            player.closeInventory();
            openRowEditor(player, session.rowIndex);
        }
    }

    private void refreshRowEditor(Player player, EnderChestSession session, EnderChestRow row) {
        Inventory inv = player.getOpenInventory().getTopInventory();
        inv.setItem(15, buildLoreItem(row, session.selectedLoreLine, session.previewMode));
    }

    private ItemStack buildLoreItem(EnderChestRow row, int selectedLine, boolean preview) {
        if (preview) {
            return row.getItemStack();
        }
        java.util.List<String> lore = new java.util.ArrayList<>();
        lore.add("&7");
        for (int i = 0; i < row.getLore().size(); i++) {
            String prefix = (i == selectedLine) ? "&a" + i + ". " : "&7" + i + ". ";
            lore.add(prefix + row.getLore().get(i));
        }
        if (!row.getLore().isEmpty()) lore.add("&7");
        lore.add("&aShift Left Click to add new line");
        lore.add("&cShift Right Click to delete a line");
        lore.add("&eLeft and Right to Scroll");
        lore.add("&bMiddle Click to edit Text");
        lore.add("&9Press Q to preview item");
        return createItem(Material.COMMAND_MINECART, "&e&lLore Text", lore.toArray(new String[0]));
    }

    @SuppressWarnings("deprecation")
    private void fillBorders(Inventory inv) {
        ItemStack black = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 15);
        ItemMeta meta = black.getItemMeta();
        meta.setDisplayName(CC.t("&7"));
        black.setItemMeta(meta);
        for (int i = 0; i < 9; i++) inv.setItem(i, black);
        inv.setItem(9, black);
        inv.setItem(17, black);
        inv.setItem(18, black);
        inv.setItem(26, black);
        inv.setItem(27, black);
        inv.setItem(35, black);
        for (int i = 36; i < 45; i++) inv.setItem(i, black);
    }

    @SuppressWarnings("deprecation")
    private void fillRowEditorBorders(Inventory inv) {
        ItemStack black = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 15);
        ItemMeta meta = black.getItemMeta();
        meta.setDisplayName(CC.t("&7"));
        black.setItemMeta(meta);
        for (int i = 0; i < inv.getSize(); i++) {
            inv.setItem(i, black);
        }
    }

    @SuppressWarnings("deprecation")
    private ItemStack createItem(Material mat, String name, String... lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(CC.t(name));
        java.util.List<String> loreList = new java.util.ArrayList<>();
        for (String l : lore) loreList.add(CC.t(l));
        meta.setLore(loreList);
        item.setItemMeta(meta);
        return item;
    }

    @SuppressWarnings("deprecation")
    private ItemStack createAnimatedPane(String name) {
        ItemStack item = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 4);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(CC.t(name));
        item.setItemMeta(meta);
        return item;
    }

    @SuppressWarnings("deprecation")
    private ItemStack createBorderPane() {
        ItemStack item = new ItemStack(Material.STAINED_GLASS_PANE, 1, (short) 15);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(CC.t("&7"));
        item.setItemMeta(meta);
        return item;
    }

}
