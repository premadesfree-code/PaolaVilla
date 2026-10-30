package me.keano.azurite.modules.blockshop.editor.menu;

import me.keano.azurite.HCF;
import me.keano.azurite.utils.ItemBuilder;
import me.keano.azurite.utils.ItemUtils;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * Shared helpers for editor menus: border glass, quick item stacks.
 */
public final class EditorItems {

    private EditorItems() {
    }

    public static ItemStack borderGlass(HCF instance) {
        return new ItemBuilder(ItemUtils.getMat("STAINED_GLASS_PANE"))
                .data(instance.getBlockshopManager(), 15)
                .setName("&7")
                .toItemStack();
    }

    public static ItemStack borderGlassBlack(HCF instance) {
        return new ItemBuilder(ItemUtils.getMat("STAINED_GLASS_PANE"))
                .data(instance.getBlockshopManager(), 15)
                .setName("&7")
                .toItemStack();
    }

    public static ItemStack named(HCF instance, String mat, int data, String name, List<String> lore) {
        ItemBuilder b = new ItemBuilder(ItemUtils.getMat(mat));
        if (data != 0) b.data(instance.getBlockshopManager(), data);
        b.setName(name);
        if (lore != null && !lore.isEmpty()) b.setLore(lore);
        return b.toItemStack();
    }

    public static ItemStack named(HCF instance, String mat, String name, List<String> lore) {
        return named(instance, mat, 0, name, lore);
    }
}
