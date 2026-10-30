package me.keano.azurite.modules.enderchest;

import lombok.Getter;
import lombok.Setter;
import me.keano.azurite.utils.CC;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * Copyright (c) 2025. Keano
 * Use or redistribution of source or file is
 * only permitted if given explicit permission.
 */
@Getter
@Setter
public class EnderChestRow {

    private int index;
    private String name;
    private String itemName;
    private Material itemMaterial;
    private short itemData;
    private List<String> lore;
    private List<String> permissions;
    private int pos1;
    private int pos2;

    public EnderChestRow(int index, String name) {
        this.index = index;
        this.name = name;
        this.itemName = name;
        this.itemMaterial = Material.THIN_GLASS;
        this.itemData = 0;
        this.lore = new ArrayList<>();
        this.permissions = new ArrayList<>();
        this.pos1 = -1;
        this.pos2 = -1;
    }

    public boolean hasPermission(Player player) {
        if (permissions.isEmpty()) return true;
        for (String perm : permissions) {
            if (player.hasPermission(perm)) return true;
        }
        return false;
    }

    public boolean isAssigned() {
        return pos1 >= 0 && pos2 >= 0;
    }

    @SuppressWarnings("deprecation")
    public ItemStack getItemStack() {
        ItemStack item = new ItemStack(itemMaterial, 1, itemData);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(CC.t(itemName));
        if (!lore.isEmpty()) {
            meta.setLore(CC.t(lore));
        }
        item.setItemMeta(meta);
        return item;
    }

    @SuppressWarnings("deprecation")
    public ItemStack getSpacerItem() {
        ItemStack item = new ItemStack(itemMaterial, 1, itemData);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(CC.t(itemName));
        if (!lore.isEmpty()) {
            meta.setLore(CC.t(lore));
        }
        item.setItemMeta(meta);
        return item;
    }

    public void saveToConfig(YamlConfiguration config) {
        config.set("index", index);
        config.set("name", name);
        config.set("itemName", itemName);
        config.set("itemMaterial", itemMaterial.name());
        config.set("itemData", (int) itemData);
        config.set("lore", lore);
        config.set("permissions", permissions);
        config.set("pos1", pos1);
        config.set("pos2", pos2);
    }

    public static EnderChestRow fromConfig(YamlConfiguration config) {
        EnderChestRow row = new EnderChestRow(
                config.getInt("index", 0),
                config.getString("name", "Row")
        );
        row.setItemName(config.getString("itemName", row.getName()));
        row.setItemMaterial(Material.valueOf(config.getString("itemMaterial", "THIN_GLASS")));
        row.setItemData((short) config.getInt("itemData", 0));
        row.setLore(config.getStringList("lore"));
        row.setPermissions(config.getStringList("permissions"));
        row.setPos1(config.getInt("pos1", -1));
        row.setPos2(config.getInt("pos2", -1));
        return row;
    }
}
