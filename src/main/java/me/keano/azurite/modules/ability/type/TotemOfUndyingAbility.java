package me.keano.azurite.modules.ability.type;

import me.keano.azurite.HCF;
import me.keano.azurite.modules.ability.Ability;
import me.keano.azurite.modules.ability.AbilityManager;
import me.keano.azurite.modules.ability.extra.AbilityUseType;
import me.keano.azurite.modules.pvpclass.type.rogue.RogueBackstabEvent;
import me.keano.azurite.utils.Tasks;
import net.minecraft.server.v1_8_R3.EnumParticle;
import net.minecraft.server.v1_8_R3.PacketPlayOutGameStateChange;
import net.minecraft.server.v1_8_R3.PacketPlayOutNamedSoundEffect;
import net.minecraft.server.v1_8_R3.PacketPlayOutWorldParticles;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.craftbukkit.v1_8_R3.CraftSound;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Copyright (c) 2025. Keano
 * Use or redistribution of source or file is
 * only permitted if given explicit permission.
 */
public class TotemOfUndyingAbility extends Ability {

    private static Set<UUID> cache;

    private final int seconds;

    public TotemOfUndyingAbility(AbilityManager manager) {
        super(
                manager,
                AbilityUseType.INTERACT,
                "Totem Of Undying"
        );
        this.cache = new HashSet<>();
        this.seconds = getAbilitiesConfig().getInt("TOTEM_OF_UNDYING.SECONDS");
    }

    public boolean hasTotemEffect(Player player) {
        return cache.contains(player.getUniqueId());
    }

    /**
     * Called by ArcherClass and RogueClass BEFORE they apply damage via setHealth().
     * Returns true if the totem saved the player (damage should be cancelled).
     * Returns false if the totem is not active or would not save (damage proceeds normally).
     */
    public static boolean trySaveFromLethalDamage(Player player, double incomingDamage) {
        if (cache == null) return false;
        if (!cache.contains(player.getUniqueId())) return false;

        double finalHealth = player.getHealth() - incomingDamage;
        if (finalHealth > 0.0) return false;

        triggerSave(player);
        return true;
    }

    /**
     * Called by RogueClass to check if the totem should prevent a backstab death.
     * Returns true if the totem saved the player.
     */
    public static boolean trySaveFromBackstab(Player player, double backstabDamage) {
        return trySaveFromLethalDamage(player, backstabDamage);
    }

    private static void triggerSave(Player player) {
        cache.remove(player.getUniqueId());
        player.setHealth(player.getMaxHealth());

        HCF instance = (HCF) Bukkit.getPluginManager().getPlugin("Azurite");
        if (instance == null) return;

        TotemOfUndyingAbility ability = (TotemOfUndyingAbility) instance.getAbilityManager().getAbility("TotemOfUndying");
        if (ability == null) return;

        for (String s : ability.getLanguageConfig().getStringList("ABILITIES.TOTEM_OF_UNDYING.SAVED")) {
            player.sendMessage(s.replace("%ability%", ability.getDisplayName()));
        }

        CraftPlayer cp = (CraftPlayer) player;
        cp.getHandle().playerConnection.sendPacket(new PacketPlayOutGameStateChange(10, 0.0F));
    }

    @Override
    public void onClick(Player player) {
        if (cannotUse(player)) return;
        if (hasCooldown(player)) return;

        takeItem(player);
        applyCooldown(player);

        cache.add(player.getUniqueId());

        for (String s : getLanguageConfig().getStringList("ABILITIES.TOTEM_OF_UNDYING.ACTIVATED")) {
            player.sendMessage(s.replace("%seconds%", String.valueOf(seconds)));
        }

        Tasks.executeLater(getManager(), seconds * 20L, () -> {
            if (!cache.remove(player.getUniqueId())) return;
            if (!player.isOnline()) return;

            for (String s : getLanguageConfig().getStringList("ABILITIES.TOTEM_OF_UNDYING.EXPIRED")) {
                player.sendMessage(s.replace("%ability%", getDisplayName()));
            }
        });

        org.bukkit.Location loc = player.getLocation().clone().add(0.0, 1.0, 0.5);

        PacketPlayOutNamedSoundEffect soundPacket = new PacketPlayOutNamedSoundEffect(
                CraftSound.getSound(Sound.DIG_STONE),
                loc.getX(), loc.getY(), loc.getZ(),
                3.0F, 1.0F
        );

        PacketPlayOutWorldParticles particlePacket = new PacketPlayOutWorldParticles(
                EnumParticle.BLOCK_CRACK, true,
                (float) loc.getX(), (float) loc.getY(), (float) loc.getZ(),
                0.0F, 0.0F, 0.0F, 5.0F, 10, 1, Material.EMERALD_BLOCK.getId()
        );

        for (Player nearby : loc.getWorld().getPlayers()) {
            if (nearby.getLocation().distanceSquared(loc) <= 12.5 * 12.5) {
                CraftPlayer cp = (CraftPlayer) nearby;
                cp.getHandle().playerConnection.sendPacket(soundPacket);
                cp.getHandle().playerConnection.sendPacket(particlePacket);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player)) return;

        Player player = (Player) e.getEntity();
        if (!cache.contains(player.getUniqueId())) return;

        double finalHealth = player.getHealth() - e.getFinalDamage();
        if (finalHealth > 0.0) return;

        cache.remove(player.getUniqueId());
        e.setCancelled(true);
        player.setHealth(player.getMaxHealth());

        for (String s : getLanguageConfig().getStringList("ABILITIES.TOTEM_OF_UNDYING.SAVED")) {
            player.sendMessage(s.replace("%ability%", getDisplayName()));
        }

        CraftPlayer cp = (CraftPlayer) player;
        cp.getHandle().playerConnection.sendPacket(new PacketPlayOutGameStateChange(10, 0.0F));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBackstab(RogueBackstabEvent e) {
        if (!(e.getEntity() instanceof Player)) return;
        if (!cache.contains(e.getEntity().getUniqueId())) return;

        Player player = (Player) e.getEntity();
        double finalHealth = player.getHealth() - e.getDamage();
        if (finalHealth > 0.0) return;

        cache.remove(player.getUniqueId());
        e.setDamage(0D);
        player.setHealth(player.getMaxHealth());

        for (String s : getLanguageConfig().getStringList("ABILITIES.TOTEM_OF_UNDYING.SAVED")) {
            player.sendMessage(s.replace("%ability%", getDisplayName()));
        }

        CraftPlayer cp = (CraftPlayer) player;
        cp.getHandle().playerConnection.sendPacket(new PacketPlayOutGameStateChange(10, 0.0F));
    }
}
