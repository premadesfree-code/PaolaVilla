package me.keano.azurite.modules.enderchest;

/**
 * Copyright (c) 2025. Keano
 * Use or redistribution of source or file is
 * only permitted if given explicit permission.
 */
public class EnderChestSession {

    public enum Type {
        PLAYER_EC,
        EDITOR,
        ROW_EDITOR,
        PERMISSIONS_EDITOR,
        SLOT_PICKER
    }

    public Type type;
    public int rowIndex;
    public int selectedLoreLine;
    public boolean previewMode;
    public boolean deleteMode;
    public boolean itemPickMode;
    public int pos1;

    public EnderChestSession(Type type) {
        this.type = type;
        this.rowIndex = -1;
        this.selectedLoreLine = 0;
        this.previewMode = false;
        this.deleteMode = false;
        this.itemPickMode = false;
        this.pos1 = -1;
    }

    public EnderChestSession(Type type, int rowIndex) {
        this(type);
        this.rowIndex = rowIndex;
    }

    public EnderChestSession(Type type, int rowIndex, int selectedLoreLine) {
        this(type, rowIndex);
        this.selectedLoreLine = selectedLoreLine;
    }

    public EnderChestSession(Type type, int rowIndex, int selectedLoreLine, boolean previewMode) {
        this(type, rowIndex, selectedLoreLine);
        this.previewMode = previewMode;
    }
}
