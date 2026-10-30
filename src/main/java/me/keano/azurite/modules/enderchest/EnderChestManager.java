package me.keano.azurite.modules.enderchest;

import lombok.Getter;
import lombok.Setter;
import me.keano.azurite.HCF;
import me.keano.azurite.modules.framework.Manager;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Copyright (c) 2025. Keano
 * Use or redistribution of source or file is
 * only permitted if given explicit permission.
 */
@Getter
@Setter
public class EnderChestManager extends Manager {

    private final List<EnderChestRow> rows;
    private final Map<UUID, EnderChestData> playerData;

    private File dataFolder;
    private File playersFolder;

    private String ecTitle;
    private int ecRows;

    public EnderChestManager(HCF instance) {
        super(instance);
        this.rows = new ArrayList<>();
        this.playerData = new HashMap<>();
        this.load();
    }

    public void load() {
        this.dataFolder = new File(getInstance().getDataFolder(), "Enderchest");
        if (!dataFolder.exists()) dataFolder.mkdirs();

        this.playersFolder = new File(dataFolder, "players");
        if (!playersFolder.exists()) playersFolder.mkdirs();

        rows.clear();

        File settingsFile = new File(dataFolder, "settings.yml");
        YamlConfiguration settings;

        if (!settingsFile.exists()) {
            try {
                settingsFile.createNewFile();
                settings = new YamlConfiguration();
                settings.set("title", "&8EnderChest");
                settings.set("rows", 3);
                settings.save(settingsFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        settings = YamlConfiguration.loadConfiguration(settingsFile);
        this.ecTitle = settings.getString("title", "&8EnderChest");
        this.ecRows = settings.getInt("rows", 3);

        File[] rowFiles = dataFolder.listFiles((dir, name) ->
                name.startsWith("row_") && name.endsWith(".yml"));

        if (rowFiles != null) {
            for (File rowFile : rowFiles) {
                YamlConfiguration config = YamlConfiguration.loadConfiguration(rowFile);
                rows.add(EnderChestRow.fromConfig(config));
            }
        }
    }

    public void saveSettings() {
        File settingsFile = new File(dataFolder, "settings.yml");
        try {
            YamlConfiguration settings = new YamlConfiguration();
            settings.set("title", ecTitle);
            settings.set("rows", ecRows);
            settings.save(settingsFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void saveRow(EnderChestRow row) {
        File rowFile = new File(dataFolder, "row_" + row.getIndex() + ".yml");
        try {
            YamlConfiguration config = new YamlConfiguration();
            row.saveToConfig(config);
            config.save(rowFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void deleteRowFile(int index) {
        File rowFile = new File(dataFolder, "row_" + index + ".yml");
        if (rowFile.exists()) rowFile.delete();
    }

    public EnderChestRow createRow(String name) {
        int index = rows.size();
        EnderChestRow row = new EnderChestRow(index, name);
        rows.add(row);
        saveRow(row);
        return row;
    }

    public void deleteRow(int index) {
        rows.remove(index);
        deleteRowFile(index);

        for (int i = 0; i < rows.size(); i++) {
            EnderChestRow row = rows.get(i);
            int oldIndex = row.getIndex();
            row.setIndex(i);
            if (oldIndex != i) deleteRowFile(oldIndex);
            saveRow(row);
        }

        for (UUID uuid : new ArrayList<>(playerData.keySet())) {
            savePlayerData(uuid);
        }
    }

    public EnderChestRow getRow(int index) {
        if (index < 0 || index >= rows.size()) return null;
        return rows.get(index);
    }

    public EnderChestRow getRowBySlot(int slot) {
        for (EnderChestRow row : rows) {
            if (slot >= row.getPos1() && slot <= row.getPos2()) return row;
        }
        return null;
    }

    public boolean isSlotLocked(Player player, int slot) {
        EnderChestRow row = getRowBySlot(slot);
        if (row == null) return false;
        if (row.getPos1() < 0 || row.getPos2() < 0) return false;
        return !row.hasPermission(player);
    }

    public ItemStack getRowItemForSlot(Player player, int slot) {
        EnderChestRow row = getRowBySlot(slot);
        if (row == null) return null;
        if (row.getPos1() < 0 || row.getPos2() < 0) return null;
        if (row.hasPermission(player)) return null;
        if (slot == row.getPos1()) return row.getItemStack();
        return row.getSpacerItem();
    }

    public EnderChestData getPlayerData(UUID uuid) {
        return playerData.computeIfAbsent(uuid, this::loadPlayerData);
    }

    public EnderChestData loadPlayerData(UUID uuid) {
        File playerFile = new File(playersFolder, uuid.toString() + ".yml");
        if (!playerFile.exists()) return new EnderChestData(uuid);

        YamlConfiguration config = YamlConfiguration.loadConfiguration(playerFile);
        return EnderChestData.fromConfig(uuid, config);
    }

    public void savePlayerData(UUID uuid) {
        EnderChestData data = playerData.get(uuid);
        if (data == null) return;

        File playerFile = new File(playersFolder, uuid.toString() + ".yml");
        try {
            YamlConfiguration config = new YamlConfiguration();
            data.saveToConfig(config);
            config.save(playerFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public int getEcSize() {
        return ecRows * 9;
    }

    @Override
    public void reload() {
        playerData.clear();
        this.load();
    }

    @Override
    public void disable() {
        for (UUID uuid : playerData.keySet()) {
            savePlayerData(uuid);
        }
    }
}
