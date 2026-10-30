package me.keano.azurite.modules.commands.type.essential;

import me.keano.azurite.HCF;
import me.keano.azurite.modules.commands.CommandManager;
import me.keano.azurite.modules.enderchest.EnderChestListener;
import me.keano.azurite.modules.enderchest.EnderChestManager;
import me.keano.azurite.modules.framework.Config;
import me.keano.azurite.modules.framework.commands.Command;
import me.keano.azurite.utils.CC;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Copyright (c) 2025. Keano
 * Use or redistribution of source or file is
 * only permitted if given explicit permission.
 */
public class EChestCommand extends Command {

    public EChestCommand(CommandManager manager) {
        super(
                manager,
                "echest"
        );
        this.setPermissible("azurite.echest");
    }

    @Override
    public List<String> aliases() {
        return Arrays.asList(
                "ec",
                "openechest"
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

        if (!sender.hasPermission("azurite.echest")) {
            sendMessage(sender, Config.INSUFFICIENT_PERM);
            return;
        }

        Player player = (Player) sender;
        HCF instance = getInstance();
        EnderChestManager ecManager = instance.getEnderChestManager();
        EnderChestListener ecListener = ecManager != null ? instance.getEnderChestListener() : null;

        if (ecManager == null || ecListener == null) {
            player.openInventory(player.getEnderChest());
            return;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("editor")) {
            if (!player.hasPermission("azurite.echest.editor")) {
                sendMessage(sender, Config.INSUFFICIENT_PERM);
                return;
            }
            ecListener.openEditor(player);
            return;
        }

        ecListener.openEnderChest(player);
    }
}
