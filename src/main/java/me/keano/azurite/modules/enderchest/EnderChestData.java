package me.keano.azurite.modules.enderchest;

import lombok.Getter;
import lombok.Setter;
import me.keano.azurite.utils.BukkitSerialization;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

/**
 * Copyright (c) 2025. Keano
 * Use or redistribution of source or file is
 * only permitted if given explicit permission.
 */
@Getter
@Setter
public class EnderChestData {

    private final UUID uuid;
    private ItemStack[] contents;

    public EnderChestData(UUID uuid) {
        this.uuid = uuid;
        this.contents = new ItemStack[54];
    }

    public void saveToConfig(YamlConfiguration config) {
        String serialized = BukkitSerialization.itemStackArrayToBase64(contents);
        config.set("contents", serialized);
    }

    public static EnderChestData fromConfig(UUID uuid, YamlConfiguration config) {
        EnderChestData data = new EnderChestData(uuid);
        String serialized = config.getString("contents");
        if (serialized != null && !serialized.isEmpty()) {
            ItemStack[] loaded = BukkitSerialization.itemStackArrayFromBase64(serialized);
            if (loaded != null && loaded.length > 0) {
                data.contents = loaded;
            }
        }
        return data;
    }
}
