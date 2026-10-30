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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Apply Configuration Menu - dynamic size based on category count.
 * Clicking a category applies defaults to that category.
 * Clicking the special item starts a 5-second countdown, then applies to all.
 */
public class ApplyConfigMenu extends Menu {

    private BukkitTask countdownTask;
    private boolean counting = false;
    private int remaining = 5;

    public ApplyConfigMenu(MenuManager manager, Player player) {
        super(manager, player, "&8Apply Default Configuration", computeSize(manager), false);
    }

    private static int computeSize(MenuManager manager) {
        ShopEditorManager ed = manager.getInstance().getShopEditorManager();
        int count = ed.getCategoryNames().size();
        int rows = (count / 9) + 1; // +1 row for the special item
        return Math.min(rows, 6) * 9;
    }

    private ShopEditorManager ed() {
        return getInstance().getShopEditorManager();
    }

    @Override
    public Map<Integer, Button> getButtons(Player player) {
        Map<Integer, Button> map = new HashMap<>();

        int idx = 0;
        for (String cat : ed().getCategoryNames()) {
            int s = idx + 1;
            if (s > size) break;
            final String category = cat;
            map.put(s, new Button() {
                @Override
                public void onClick(InventoryClickEvent e) {
                    e.setCancelled(true);
                    if (counting) return;
                    ed().applyDefaultsToCategory(category);
                    player.sendMessage(getLanguageConfig().getString("BLOCKSHOP.EDITOR.APPLIED_CATEGORY")
                            .replace("%category%", category));
                    new ApplyConfigMenu(getManager(), player).open();
                }

                @Override
                public ItemStack getItemStack() {
                    String mat = ed().getCategoryMaterial(category);
                    ItemStack base = new ItemStack(ItemUtils.getMat(mat));
                    ItemBuilder b = new ItemBuilder(base);
                    b.setName(ed().getCategoryName(category));
                    List<String> lore = ed().getCategoryLore(category);
                    if (lore != null && !lore.isEmpty()) b.setLore(lore);
                    return b.toItemStack();
                }
            });
            idx++;
        }

        // Special item in the next available slot
        int specialSlot = idx + 1;
        if (specialSlot <= size) {
            map.put(specialSlot, new Button() {
                @Override
                public void onClick(InventoryClickEvent e) {
                    e.setCancelled(true);
                    if (counting) return;
                    startCountdown();
                }

                @Override
                public ItemStack getItemStack() {
                    if (counting) {
                        return new ItemBuilder(ItemUtils.getMat("BEDROCK"))
                                .setName("&a&lTime Remaining " + remaining + " seconds...")
                                .setLore(Arrays.asList(
                                        "&7",
                                        "&6&lApplying to&7: &f" + ed().countAllItems() + " Items",
                                        "&7&lWait 5 seconds to confirm",
                                        "&7"
                                )).toItemStack();
                    }
                    return new ItemBuilder(ItemUtils.getMat("COMMAND_MINECART"))
                            .setName("&e&lDefault Configuration")
                            .setLore(Arrays.asList(
                                    "&7",
                                    "&6Apply to all Categories...",
                                    "&7",
                                    "&eClick to Apply :|"
                            )).toItemStack();
                }
            });
        }

        return map;
    }

    private void startCountdown() {
        counting = true;
        remaining = 5;

        // Update the item immediately to countdown state
        update();

        countdownTask = org.bukkit.Bukkit.getScheduler().runTaskTimer(getInstance(), new Runnable() {
            @Override
            public void run() {
                if (!player.isOnline()) {
                    if (countdownTask != null) {
                        countdownTask.cancel();
                        countdownTask = null;
                    }
                    counting = false;
                    return;
                }
                remaining--;
                if (remaining > 0) {
                    // Update the countdown item name
                    update();
                } else {
                    // Apply to all and revert
                    if (countdownTask != null) {
                        countdownTask.cancel();
                        countdownTask = null;
                    }
                    ed().applyDefaultsToAll();
                    player.sendMessage(getLanguageConfig().getString("BLOCKSHOP.EDITOR.APPLIED"));
                    counting = false;
                    new ApplyConfigMenu(getManager(), player).open();
                }
            }
        }, 20L, 20L); // 1 second intervals
    }

    @Override
    public void onClose() {
        if (countdownTask != null) {
            countdownTask.cancel();
            countdownTask = null;
        }
        counting = false;
    }
}
