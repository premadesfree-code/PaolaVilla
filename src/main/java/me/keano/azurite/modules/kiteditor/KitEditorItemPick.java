package me.keano.azurite.modules.kiteditor;

import lombok.Getter;
import lombok.Setter;

/**
 * Copyright (c) 2025. Keano
 * Use or redistribution of source or file is
 * only permitted if given explicit permission.
 */
@Getter
@Setter
public class KitEditorItemPick {

    public enum PickMode { REFILL, ACTION }

    private final PickMode mode;
    private final String kitName;
    private final int slot;
    private boolean active;

    public KitEditorItemPick(PickMode mode, String kitName, int slot) {
        this.mode = mode;
        this.kitName = kitName;
        this.slot = slot;
        this.active = true;
    }
}
