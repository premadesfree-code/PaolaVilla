package me.keano.azurite.modules.pvpclass.type.yeti;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import lombok.Getter;
import me.keano.azurite.HCF;
import me.keano.azurite.modules.pvpclass.PvPClass;
import me.keano.azurite.modules.pvpclass.PvPClassManager;
import me.keano.azurite.modules.pvpclass.cooldown.CustomCooldown;
import me.keano.azurite.modules.teams.type.EventTeam;
import me.keano.azurite.modules.teams.type.SafezoneTeam;
import me.keano.azurite.utils.ItemUtils;
import me.keano.azurite.utils.Tasks;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Yeti PvP class.
 *
 * Passives and armor are loaded from classes.yml like every other class.
 * The helmet is visually replaced with a configurable skull texture mask via
 * ProtocolLib packets (SET_SLOT, WINDOW_ITEMS, ENTITY_EQUIPMENT) so other
 * players see the mask while the real helmet stays server-side.
 */
@Getter
public class YetiClass extends PvPClass {

    private final Map<Integer, YetiMaskData> maskData;
    private final Map<UUID, Set<Location>> cubes;

    private CustomCooldown cubeCooldown;
    private CustomCooldown snowballCooldown;

    private String maskTexture;
    private int cubeCooldownSeconds;
    private int snowballCooldownSeconds;

    public YetiClass(PvPClassManager manager) {
        super(manager, "Yeti");

        this.maskData = new HashMap<>();
        this.cubes = new HashMap<>();

        this.load();

        manager.registerListener(new YetiCubeItem(this));
        manager.registerListener(new YetiSnowballItem(this));

        if (Bukkit.getPluginManager().getPlugin("ProtocolLib") != null) {
            ProtocolLibrary.getProtocolManager().addPacketListener(new YetiSetSlotListener());
            ProtocolLibrary.getProtocolManager().addPacketListener(new YetiWindowItemsListener());
            ProtocolLibrary.getProtocolManager().addPacketListener(new YetiEquipmentListener());
        }
    }

    @Override
    public void load() {
        this.cubeCooldown = new CustomCooldown(this, getScoreboardConfig().getString("YETI_CLASS.CUBE"));
        this.snowballCooldown = new CustomCooldown(this, getScoreboardConfig().getString("YETI_CLASS.SNOWBALL"));
        this.maskTexture = getClassesConfig().getString("YETI_CLASS.MASK_TEXTURE");
        this.cubeCooldownSeconds = getClassesConfig().getInt("YETI_CLASS.CUBE_ITEM.COOLDOWN");
        this.snowballCooldownSeconds = getClassesConfig().getInt("YETI_CLASS.SNOWBALL_ITEM.COOLDOWN");
    }

    @Override
    public void handleEquip(Player player) {
        YetiMaskData data = new YetiMaskData(player, maskTexture);
        maskData.put(player.getEntityId(), data);
        Tasks.executeLater(getManager(), 1L, data::sendSetSlot);
    }

    @Override
    public void handleUnequip(Player player) {
        maskData.remove(player.getEntityId());
        removeCube(player);
    }

    @Override
    public void reload() {
        this.load();
        this.loadEffectsArmor();
    }

    public void addCube(Player player, Set<Location> locations) {
        cubes.put(player.getUniqueId(), locations);
    }

    public void removeCube(Player player) {
        Set<Location> locations = cubes.remove(player.getUniqueId());
        if (locations == null) return;

        for (Location location : locations) {
            if (location.getBlock().getType() == Material.ICE) {
                location.getBlock().setType(Material.AIR);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player killer = event.getEntity().getKiller();

        if (killer == null) return;
        if (!players.contains(killer.getUniqueId())) return;
        if (!cubes.containsKey(killer.getUniqueId())) return;

        event.getEntity().setLastDamageCause(new YetiFreezeEvent(
                event.getEntity(), killer, EntityDamageEvent.DamageCause.CUSTOM, 0.0
        ));
    }

    public boolean checkYeti(Player player, boolean message) {
        if (getInstance().getTimerManager().getPvpTimer().hasTimer(player) ||
                getInstance().getTimerManager().getInvincibilityTimer().hasTimer(player)) {
            if (message) {
                player.sendMessage(getLanguageConfig().getString("PVP_CLASSES.YETI_CLASS.CANNOT_USE_PVPTIMER"));
            }
            return true;
        }

        if (getInstance().getTeamManager().getClaimManager().getTeam(player.getLocation()) instanceof SafezoneTeam) {
            if (message) {
                player.sendMessage(getLanguageConfig().getString("PVP_CLASSES.YETI_CLASS.CANNOT_USE_SAFEZONE"));
            }
            return true;
        }

        return false;
    }

    public boolean isInEventClaim(Player player) {
        return getInstance().getTeamManager().getClaimManager().getTeam(player.getLocation()) instanceof EventTeam;
    }

    /**
     * Holds the bukkit mask item used to override the helmet in packets.
     */
    public class YetiMaskData {

        private final Player player;
        private final ItemStack bukkitMask;

        public YetiMaskData(Player player, String texture) {
            this.player = player;
            ItemStack helmet = player.getInventory().getHelmet();
            ItemStack mask = new ItemStack(ItemUtils.getMat("SKULL_ITEM"), 1, (short) 3);

            mask = me.keano.azurite.modules.masks.SkullUtils.applyTexture(mask, texture);

            ItemMeta meta = mask.getItemMeta();
            if (meta != null) {
                if (helmet != null && helmet.hasItemMeta()) {
                    ItemMeta helmetMeta = helmet.getItemMeta();
                    if (helmetMeta.hasDisplayName()) {
                        meta.setDisplayName(helmetMeta.getDisplayName());
                    } else {
                        meta.setDisplayName(ChatColor.AQUA + "Yeti");
                    }
                    if (helmetMeta.hasLore()) {
                        meta.setLore(helmetMeta.getLore());
                    }
                } else {
                    meta.setDisplayName(ChatColor.AQUA + "Yeti");
                }
                mask.setItemMeta(meta);
            }

            if (helmet != null && helmet.getEnchantments().size() > 0) {
                mask.addUnsafeEnchantments(helmet.getEnchantments());
            }

            this.bukkitMask = mask;
        }

        public void sendSetSlot() {
            player.updateInventory();
        }

        public ItemStack getBukkitMask() {
            return bukkitMask;
        }
    }

    private class YetiSetSlotListener extends PacketAdapter {

        public YetiSetSlotListener() {
            super(HCF.getPlugin(HCF.class), ListenerPriority.NORMAL, PacketType.Play.Server.SET_SLOT);
        }

        @Override
        public void onPacketSending(PacketEvent event) {
            YetiMaskData data = maskData.get(event.getPlayer().getEntityId());
            if (data == null) return;

            int slot = event.getPacket().getIntegers().read(1);
            if (slot != 5) return;

            ItemStack item = event.getPacket().getItemModifier().read(0);
            if (item == null || item.getType() != armor[0]) return;

            event.getPacket().getItemModifier().write(0, data.getBukkitMask());
        }
    }

    private class YetiWindowItemsListener extends PacketAdapter {

        public YetiWindowItemsListener() {
            super(HCF.getPlugin(HCF.class), ListenerPriority.NORMAL, PacketType.Play.Server.WINDOW_ITEMS);
        }

        @Override
        public void onPacketSending(PacketEvent event) {
            YetiMaskData data = maskData.get(event.getPlayer().getEntityId());
            if (data == null) return;

            ItemStack[] list = event.getPacket().getItemArrayModifier().read(0);
            if (list == null || list.length <= 5) return;

            ItemStack item = list[5];
            if (item == null || item.getType() != armor[0]) return;

            list[5] = data.getBukkitMask();
            event.getPacket().getItemArrayModifier().write(0, list);
        }
    }

    private class YetiEquipmentListener extends PacketAdapter {

        public YetiEquipmentListener() {
            super(HCF.getPlugin(HCF.class), ListenerPriority.NORMAL, PacketType.Play.Server.ENTITY_EQUIPMENT);
        }

        @Override
        public void onPacketSending(PacketEvent event) {
            int equipmentSlot = event.getPacket().getIntegers().getValues().size() > 1
                    ? event.getPacket().getIntegers().read(1) : -1;
            if (equipmentSlot != 4) return;

            int entityId = event.getPacket().getIntegers().read(0);
            YetiMaskData data = maskData.get(entityId);
            if (data == null) return;

            ItemStack item = event.getPacket().getItemModifier().read(0);
            if (item == null || item.getType() == Material.AIR) return;

            event.getPacket().getItemModifier().write(0, data.getBukkitMask());
        }
    }
}
