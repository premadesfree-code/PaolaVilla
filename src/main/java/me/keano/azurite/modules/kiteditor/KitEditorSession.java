package me.keano.azurite.modules.kiteditor;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Copyright (c) 2025. Keano
 * Use or redistribution of source or file is
 * only permitted if given explicit permission.
 */
@Getter
@Setter
public class KitEditorSession {

    private final Player player;
    private final String kitName;

    private ItemStack[] originalContents;
    private ItemStack[] originalArmor;

    private boolean transition; // true when switching menus so close handler doesn't restore

    public KitEditorSession(Player player, String kitName) {
        this.player = player;
        this.kitName = kitName;
        this.transition = false;
    }

    public void snapshot() {
        ItemStack[] contents = player.getInventory().getContents();
        ItemStack[] armor = player.getInventory().getArmorContents();

        this.originalContents = new ItemStack[contents.length];
        this.originalArmor = new ItemStack[armor.length];

        for (int i = 0; i < contents.length; i++) {
            originalContents[i] = (contents[i] != null ? contents[i].clone() : null);
        }

        for (int i = 0; i < armor.length; i++) {
            originalArmor[i] = (armor[i] != null ? armor[i].clone() : null);
        }
    }

    public void restore() {
        player.getInventory().setContents(originalContents);
        player.getInventory().setArmorContents(originalArmor);
        player.updateInventory();
    }
}
