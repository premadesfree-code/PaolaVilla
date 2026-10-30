package me.keano.azurite.modules.kiteditor;

import lombok.Getter;
import me.keano.azurite.HCF;
import me.keano.azurite.modules.framework.Manager;
import me.keano.azurite.modules.kits.Kit;
import me.keano.azurite.utils.CC;
import me.keano.azurite.utils.ItemBuilder;
import me.keano.azurite.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

/**
 * Copyright (c) 2025. Keano
 * Use or redistribution of source or file is
 * only permitted if given explicit permission.
 */
@Getter
public class KitEditorManager extends Manager {

    private KitEditorFileManager fileManager;

    private final Map<UUID, KitEditorSession> sessions;
    private final Map<UUID, KitEditorContext> contexts;
    private final Map<UUID, KitEditorPendingInput> pendingInputs;
    private final Map<UUID, KitEditorItemPick> pendingPicks;
    private final Map<UUID, String> contextKitName; // which kit a player is currently working on
    private final Map<UUID, Integer> loreSelectedIndex;
    private final Map<UUID, List<String>> loreWorkingCopy;
    private final Map<UUID, Boolean> lorePreviewMode;
    private final Map<UUID, BukkitTask> cyclingTasks;
    private final Map<UUID, Inventory> openInventories; // track the custom inventory per player

    // armor edit working copies: piece -> slot
    private final Map<UUID, Map<String, Integer>> armorWorkingSlots;

    public KitEditorManager(HCF instance) {
        super(instance);
        this.fileManager = new KitEditorFileManager(instance);
        this.sessions = new HashMap<>();
        this.contexts = new HashMap<>();
        this.pendingInputs = new HashMap<>();
        this.pendingPicks = new HashMap<>();
        this.contextKitName = new HashMap<>();
        this.loreSelectedIndex = new HashMap<>();
        this.loreWorkingCopy = new HashMap<>();
        this.lorePreviewMode = new HashMap<>();
        this.cyclingTasks = new HashMap<>();
        this.openInventories = new HashMap<>();
        this.armorWorkingSlots = new HashMap<>();
    }

    @Override
    public void enable() {
        fileManager.load();

        // Register existing non-system kits that might not have config yet
        for (Kit kit : getInstance().getKitManager().getKits().values()) {
            if (!kit.isSystemKit()) {
                fileManager.createDefaultKitConfig(kit.getName());
            }
        }

        registerListener(new KitEditorMenuListener(this));
        registerListener(new KitEditorChatListener(this));
    }

    @Override
    public void disable() {
        // Restore all active sessions
        for (KitEditorSession session : new ArrayList<>(sessions.values())) {
            if (session.getPlayer().isOnline()) {
                session.restore();
            }
        }
        sessions.clear();

        // Cancel all cycling tasks
        for (BukkitTask task : cyclingTasks.values()) {
            task.cancel();
        }
        cyclingTasks.clear();

        contexts.clear();
        pendingInputs.clear();
        pendingPicks.clear();
    }

    // --- Kit lifecycle hooks ---

    public void onKitCreate(String kitName) {
        fileManager.createDefaultKitConfig(kitName);
    }

    public void onKitDelete(String kitName) {
        fileManager.removeKitConfig(kitName);
    }

    // --- Context management ---

    public void setContext(Player player, KitEditorContext context) {
        contexts.put(player.getUniqueId(), context);
    }

    public KitEditorContext getContext(Player player) {
        return contexts.getOrDefault(player.getUniqueId(), KitEditorContext.NONE);
    }

    public void clearContext(Player player) {
        contexts.remove(player.getUniqueId());
    }

    public void setContextKit(Player player, String kitName) {
        contextKitName.put(player.getUniqueId(), kitName);
    }

    public String getContextKit(Player player) {
        return contextKitName.get(player.getUniqueId());
    }

    // --- Session management ---

    public KitEditorSession startSession(Player player, String kitName) {
        KitEditorSession session = new KitEditorSession(player, kitName);
        session.snapshot();
        sessions.put(player.getUniqueId(), session);
        return session;
    }

    public KitEditorSession getSession(Player player) {
        return sessions.get(player.getUniqueId());
    }

    public boolean hasSession(Player player) {
        return sessions.containsKey(player.getUniqueId());
    }

    public void endSession(Player player) {
        sessions.remove(player.getUniqueId());
    }

    // --- Chat input ---

    public void setPendingInput(Player player, KitEditorPendingInput input) {
        pendingInputs.put(player.getUniqueId(), input);
    }

    public KitEditorPendingInput getPendingInput(Player player) {
        return pendingInputs.get(player.getUniqueId());
    }

    public void clearPendingInput(Player player) {
        pendingInputs.remove(player.getUniqueId());
    }

    // --- Item pick ---

    public void setPendingPick(Player player, KitEditorItemPick pick) {
        pendingPicks.put(player.getUniqueId(), pick);
    }

    public KitEditorItemPick getPendingPick(Player player) {
        return pendingPicks.get(player.getUniqueId());
    }

    public void clearPendingPick(Player player) {
        pendingPicks.remove(player.getUniqueId());
    }

    // --- Cycling tasks ---

    public void startCyclingTask(Player player, Inventory inventory, Set<Integer> slots) {
        cancelCyclingTask(player);

        BukkitTask task = Bukkit.getScheduler().runTaskTimer(getInstance(), new Runnable() {
            int colorIndex = 0;
            final int[] colors = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15};

            @Override
            public void run() {
                if (!player.isOnline() || inventory == null) {
                    cancelCyclingTask(player);
                    return;
                }

                int color = colors[colorIndex % colors.length];
                colorIndex++;

                ItemStack pane = createColoredPane(color);

                for (int slot : slots) {
                    if (slot < 0 || slot >= inventory.getSize()) continue;
                    ItemStack current = inventory.getItem(slot);
                    if (current == null || current.getType() == Material.AIR || isCyclingPane(current)) {
                        inventory.setItem(slot, pane);
                    }
                }

                player.updateInventory();
            }
        }, 0L, 20L);

        cyclingTasks.put(player.getUniqueId(), task);
    }

    public void cancelCyclingTask(Player player) {
        BukkitTask task = cyclingTasks.remove(player.getUniqueId());
        if (task != null) {
            task.cancel();
        }
    }

    @SuppressWarnings("deprecation")
    public ItemStack createColoredPane(int color) {
        Material material;
        if (Utils.isModernVer()) {
            material = Material.valueOf("WHITE_STAINED_GLASS_PANE");
        } else {
            material = Material.valueOf("STAINED_GLASS_PANE");
        }
        ItemStack pane = new ItemStack(material, 1, (short) color);
        ItemMeta meta = pane.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(" ");
            pane.setItemMeta(meta);
        }
        return pane;
    }

    public boolean isCyclingPane(ItemStack item) {
        if (item == null) return false;
        String typeName = item.getType().name();
        return typeName.equals("STAINED_GLASS_PANE") || typeName.equals("WHITE_STAINED_GLASS_PANE");
    }

    // --- Open inventory tracking ---

    public void setOpenInventory(Player player, Inventory inventory) {
        openInventories.put(player.getUniqueId(), inventory);
    }

    public Inventory getOpenInventory(Player player) {
        return openInventories.get(player.getUniqueId());
    }

    public void clearOpenInventory(Player player) {
        openInventories.remove(player.getUniqueId());
    }

    // --- Helper to get non-system kits sorted ---

    public List<Kit> getEditableKits() {
        List<Kit> kits = new ArrayList<>();
        for (Kit kit : getInstance().getKitManager().getKits().values()) {
            if (!kit.isSystemKit()) {
                kits.add(kit);
            }
        }
        return kits;
    }

    // --- Admin menu slot helper (find next available from 10, skipping pane slots) ---

    public int findNextAdminSlot(Inventory inventory, int start) {
        Set<Integer> blocked = getAdminPaneSlots();
        for (int i = start; i < inventory.getSize(); i++) {
            if (blocked.contains(i)) continue;
            ItemStack item = inventory.getItem(i);
            if (item == null || item.getType() == Material.AIR) {
                return i;
            }
        }
        return -1;
    }

    public Set<Integer> getAdminPaneSlots() {
        Set<Integer> slots = new HashSet<>();
        // first row 0-8
        for (int i = 0; i < 9; i++) slots.add(i);
        // last row 36-44
        for (int i = 36; i < 45; i++) slots.add(i);
        // sides
        slots.add(9);
        slots.add(17);
        slots.add(18);
        slots.add(26);
        slots.add(27);
        slots.add(35);
        return slots;
    }

    // --- Decorative pane for admin menus ---

    @SuppressWarnings("deprecation")
    public ItemStack createBlackPane() {
        Material material;
        if (Utils.isModernVer()) {
            material = Material.valueOf("BLACK_STAINED_GLASS_PANE");
        } else {
            material = Material.valueOf("STAINED_GLASS_PANE");
        }
        ItemStack pane = new ItemStack(material, 1, Utils.isModernVer() ? (short) 0 : (short) 15);
        ItemMeta meta = pane.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(CC.t("&7"));
            pane.setItemMeta(meta);
        }
        return pane;
    }

    @SuppressWarnings("deprecation")
    public ItemStack createBlackPaneForKit() {
        return createBlackPane();
    }

    // --- Kit display item for menus ---

    public ItemStack createKitDisplayItem(String kitName) {
        String name = fileManager.getKitItemName(kitName);
        List<String> lore = fileManager.getKitItemLore(kitName);

        ItemBuilder builder = new ItemBuilder(Material.DIAMOND_HELMET);
        builder.setName(name);

        if (!lore.isEmpty()) {
            builder.setLore(lore);
        }

        return builder.toItemStack();
    }

    // --- Chat input handler ---

    public void handleChatInput(Player player, KitEditorPendingInput input, String message) {
        String kitName = getContextKit(player);

        switch (input) {
            case MENU_TITLE:
                if (message.equalsIgnoreCase("cancel")) {
                    openAdminMenu(player);
                    return;
                }
                fileManager.setTitle(message);
                openAdminMenu(player);
                break;

            case MENU_ROWS:
                try {
                    int rows = Integer.parseInt(message);
                    if (rows < 1 || rows > 6) {
                        player.sendMessage(CC.t("&cCancelled"));
                        openAdminMenu(player);
                        return;
                    }
                    fileManager.setRows(rows);
                } catch (NumberFormatException e) {
                    player.sendMessage(CC.t("&cCancelled"));
                }
                openAdminMenu(player);
                break;

            case KIT_ITEM_NAME:
                if (message.equalsIgnoreCase("cancel")) {
                    openKitConfigMenu(player, kitName);
                    return;
                }
                fileManager.setKitItemName(kitName, message);
                openKitConfigMenu(player, kitName);
                break;

            case KIT_MENU_ROWS:
                try {
                    int rows = Integer.parseInt(message);
                    if (rows < 1 || rows > 6) {
                        player.sendMessage(CC.t("&cCancelled"));
                        openKitConfigMenu(player, kitName);
                        return;
                    }
                    fileManager.setKitMenuRows(kitName, rows);
                } catch (NumberFormatException e) {
                    player.sendMessage(CC.t("&cCancelled"));
                }
                openKitConfigMenu(player, kitName);
                break;

            case KIT_MENU_TITLE:
                if (message.equalsIgnoreCase("cancel")) {
                    openKitConfigMenu(player, kitName);
                    return;
                }
                fileManager.setKitMenuTitle(kitName, message);
                openKitConfigMenu(player, kitName);
                break;

            case LORE_ADD:
                if (message.equalsIgnoreCase("cancel")) {
                    openKitConfigMenu(player, kitName);
                    return;
                }
                List<String> loreAdd = loreWorkingCopy.getOrDefault(player.getUniqueId(), new ArrayList<>());
                loreAdd.add(message);
                loreWorkingCopy.put(player.getUniqueId(), loreAdd);
                loreSelectedIndex.put(player.getUniqueId(), loreAdd.size() - 1);
                fileManager.setKitItemLore(kitName, loreAdd);
                openKitConfigMenu(player, kitName);
                break;

            case LORE_EDIT:
                if (message.equalsIgnoreCase("cancel")) {
                    openKitConfigMenu(player, kitName);
                    return;
                }
                List<String> loreEdit = loreWorkingCopy.getOrDefault(player.getUniqueId(), new ArrayList<>());
                int idx = loreSelectedIndex.getOrDefault(player.getUniqueId(), 0);
                if (idx >= 0 && idx < loreEdit.size()) {
                    loreEdit.set(idx, message);
                    loreWorkingCopy.put(player.getUniqueId(), loreEdit);
                    fileManager.setKitItemLore(kitName, loreEdit);
                }
                openKitConfigMenu(player, kitName);
                break;

            default:
                break;
        }
    }

    // --- Menu openers (delegate to KitEditorMenus) ---

    public void openMainMenu(Player player) {
        KitEditorMenus.openMainMenu(this, player);
    }

    public void openAdminMenu(Player player) {
        KitEditorMenus.openAdminMenu(this, player);
    }

    public void openKitConfigMenu(Player player, String kitName) {
        KitEditorMenus.openKitConfigMenu(this, player, kitName);
    }

    public void openDecorativeEditMain(Player player) {
        KitEditorMenus.openDecorativeEditMain(this, player);
    }

    public void openDecorativeEditKit(Player player, String kitName) {
        KitEditorMenus.openDecorativeEditKit(this, player, kitName);
    }

    public void openSlotPicker(Player player, String kitName) {
        KitEditorMenus.openSlotPicker(this, player, kitName);
    }

    public void openArmorEdit(Player player, String kitName) {
        KitEditorMenus.openArmorEdit(this, player, kitName);
    }

    public void openRefillEdit(Player player, String kitName) {
        KitEditorMenus.openRefillEdit(this, player, kitName);
    }

    public void openActionEdit(Player player, String kitName) {
        KitEditorMenus.openActionEdit(this, player, kitName);
    }

    public void openKitSession(Player player, String kitName) {
        KitEditorMenus.openKitSession(this, player, kitName);
    }

    // --- Personal kit equip logic ---

    public void equipPersonalKit(Player player, Kit kit) {
        UUID uuid = player.getUniqueId();
        String kitName = kit.getName();

        if (fileManager.hasPersonalKit(uuid, kitName)) {
            ItemStack[] contents = fileManager.getPersonalKitContents(uuid, kitName);
            ItemStack[] armor = fileManager.getPersonalKitArmor(uuid, kitName);

            player.getInventory().clear();

            if (contents != null) {
                for (int i = 0; i < contents.length && i < 36; i++) {
                    ItemStack toSet = contents[i];
                    if (toSet == null || toSet.getType() == Material.AIR) continue;
                    player.getInventory().setItem(i, toSet.clone());
                }
            }

            if (armor != null) {
                for (int i = 0; i < armor.length && i < 4; i++) {
                    ItemStack toSet = armor[i];
                    if (toSet == null || toSet.getType() == Material.AIR) continue;
                    player.getInventory().setItem(36 + i, toSet.clone());
                }
            }

            kit.getCooldown().applyCooldown(player, kit.getSeconds());
            player.updateInventory();
        } else {
            kit.equip(player);
        }
    }
}
