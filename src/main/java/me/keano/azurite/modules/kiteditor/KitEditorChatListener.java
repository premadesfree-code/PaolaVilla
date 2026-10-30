package me.keano.azurite.modules.kiteditor;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

/**
 * Copyright (c) 2025. Keano
 * Use or redistribution of source or file is
 * only permitted if given explicit permission.
 */
public class KitEditorChatListener implements Listener {

    private final KitEditorManager manager;

    public KitEditorChatListener(KitEditorManager manager) {
        this.manager = manager;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        KitEditorPendingInput pending = manager.getPendingInput(player);

        if (pending == null) return;

        event.setCancelled(true);

        final String message = event.getMessage();
        final KitEditorPendingInput input = pending;

        manager.clearPendingInput(player);

        Bukkit.getScheduler().runTask(manager.getInstance(), () -> manager.handleChatInput(player, input, message));
    }
}
