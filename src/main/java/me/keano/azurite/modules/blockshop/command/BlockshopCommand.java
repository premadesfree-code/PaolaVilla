package me.keano.azurite.modules.blockshop.command;

import me.keano.azurite.modules.blockshop.editor.ShopEditorManager;
import me.keano.azurite.modules.blockshop.editor.menu.EditorShopMenu;
import me.keano.azurite.modules.blockshop.editor.menu.ShopEditorMenu;
import me.keano.azurite.modules.commands.CommandManager;
import me.keano.azurite.modules.framework.Config;
import me.keano.azurite.modules.framework.commands.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

/**
 * Copyright (c) 2025. Keano
 * Use or redistribution of source or file is
 * only permitted if given explicit permission.
 */
public class BlockshopCommand extends Command {

    public BlockshopCommand(CommandManager manager) {
        super(
                manager,
                "blockshop"
        );
    }

    @Override
    public List<String> aliases() {
        return Collections.singletonList(
                "shop"
        );
    }

    @Override
    public List<String> usage() {
        return Collections.emptyList();
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sendMessage(sender, Config.PLAYER_ONLY);
            return;
        }

        Player player = (Player) sender;

        if (getInstance().getBlockshopManager().cannotUseShop(player)) {
            return;
        }

        if (args.length >= 1 && args[0].equalsIgnoreCase("editor")) {
            if (!player.hasPermission(ShopEditorManager.PERM)) {
                sendMessage(sender, Config.INSUFFICIENT_PERM);
                return;
            }
            new ShopEditorMenu(getInstance().getMenuManager(), player).open();
            return;
        }

        // If the editor has a default category configured, open that directly.
        ShopEditorManager ed = getInstance().getShopEditorManager();
        String defCat = ed.getDefaultCategory();
        if (!defCat.isEmpty() && ed.categoryExists(defCat)) {
            new EditorShopMenu(getInstance().getMenuManager(), player, defCat).open();
            return;
        }

        // No default category configured via editor.
        player.sendMessage(getLanguageConfig().getString("BLOCKSHOP.NO_DEFAULT_CATEGORY"));
    }
}