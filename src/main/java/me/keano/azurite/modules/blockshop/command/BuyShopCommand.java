package me.keano.azurite.modules.blockshop.command;

import me.keano.azurite.modules.blockshop.editor.ShopEditorManager;
import me.keano.azurite.modules.blockshop.editor.menu.EditorShopMenu;
import me.keano.azurite.modules.commands.CommandManager;
import me.keano.azurite.modules.framework.Config;
import me.keano.azurite.modules.framework.commands.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;

public class BuyShopCommand extends Command {

    public BuyShopCommand(CommandManager manager) {
        super(manager, "buyshop");
    }

    @Override
    public List<String> aliases() {
        return Arrays.asList("buy", "buyblocks");
    }

    @Override
    public List<String> usage() {
        return null;
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

        ShopEditorManager ed = getInstance().getShopEditorManager();
        String defCat = ed.getDefaultCategory();
        if (!defCat.isEmpty() && ed.categoryExists(defCat)) {
            new EditorShopMenu(getInstance().getMenuManager(), player, defCat).open();
            return;
        }

        player.sendMessage(getLanguageConfig().getString("BLOCKSHOP.NO_DEFAULT_CATEGORY"));
    }
}
