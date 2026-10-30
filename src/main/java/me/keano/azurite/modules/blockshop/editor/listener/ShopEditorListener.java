package me.keano.azurite.modules.blockshop.editor.listener;

import me.keano.azurite.modules.blockshop.editor.ShopEditorManager;
import me.keano.azurite.modules.framework.Module;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

import java.util.function.Consumer;

/**
 * Copyright (c) 2025. Keano
 * Use or redistribution of source or file is
 * only permitted if given explicit permission.
 */
public class ShopEditorListener extends Module<ShopEditorManager> implements Listener {

    public ShopEditorListener(ShopEditorManager manager) {
        super(manager);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent e) {
        Player player = e.getPlayer();
        Consumer<String> cb = getManager().takeChatInput(player);
        if (cb == null) return;
        e.setCancelled(true);
        final String msg = e.getMessage();
        // Run on main thread since most callbacks reopen menus / edit configs.
        org.bukkit.Bukkit.getScheduler().runTask(getInstance(), () -> cb.accept(msg));
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        Player player = e.getPlayer();
        if (!getManager().hasItemPick(player)) return;
        if (e.getAction() != org.bukkit.event.block.Action.LEFT_CLICK_AIR
                && e.getAction() != org.bukkit.event.block.Action.LEFT_CLICK_BLOCK) return;
        Consumer<ItemStack> cb = getManager().takeItemPick(player);
        if (cb == null) return;
        e.setCancelled(true);
        ItemStack inHand = getManager().getInstance().getVersionManager().getVersion().getItemInHand(player);
        if (inHand == null) {
            // re-await if empty hand
            getManager().awaitItemPick(player, cb);
            return;
        }
        final ItemStack item = inHand.clone();
        org.bukkit.Bukkit.getScheduler().runTask(getInstance(), () -> cb.accept(item));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        getManager().clearChatInput(e.getPlayer());
        getManager().clearItemPick(e.getPlayer());
    }
}
