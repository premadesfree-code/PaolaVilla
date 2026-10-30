package me.keano.azurite.modules.kits.commands.kiteditor;

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
public class KitEditorCommand extends Command {

    public KitEditorCommand(CommandManager manager) {
        super(manager, "kiteditor");
        this.setPermissible("azurite.kiteditor");
    }

    @Override
    public List<String> aliases() {
        return Collections.emptyList();
    }

    @Override
    public List<String> usage() {
        return Collections.singletonList(
                "&cUsage: /kiteditor [editor]"
        );
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sendMessage(sender, Config.PLAYER_ONLY);
            return;
        }

        Player player = (Player) sender;

        try {
            if (args.length > 0 && args[0].equalsIgnoreCase("editor")) {
                if (!player.hasPermission("azurite.kiteditor.editor")) {
                    sendMessage(sender, Config.INSUFFICIENT_PERM);
                    return;
                }
                getInstance().getKitEditorManager().openAdminMenu(player);
                return;
            }

            getInstance().getKitEditorManager().openMainMenu(player);
        } catch (Exception e) {
            player.sendMessage(me.keano.azurite.utils.CC.t("&cKitEditor Error: " + e.getMessage()));
            e.printStackTrace();
        }
    }
}
