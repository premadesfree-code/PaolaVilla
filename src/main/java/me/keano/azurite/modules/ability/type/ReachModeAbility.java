package me.keano.azurite.modules.ability.type;

import me.keano.azurite.modules.ability.Ability;
import me.keano.azurite.modules.ability.AbilityManager;
import me.keano.azurite.modules.ability.extra.AbilityUseType;
import net.minecraft.server.v1_8_R3.PacketPlayOutExperience;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Copyright (c) 2025. Keano
 * Use or redistribution of source or file is
 * only permitted if given explicit permission.
 */
public class ReachModeAbility extends Ability {

    private final Set<UUID> cache;
    private final double reach;
    private final int duration;
    private final long durationMillis;

    public ReachModeAbility(AbilityManager manager) {
        super(
                manager,
                AbilityUseType.INTERACT,
                "Reach Mode"
        );
        this.cache = new HashSet<>();
        this.reach = getAbilitiesConfig().getDouble("REACH_MODE.REACH");
        this.duration = getAbilitiesConfig().getInt("REACH_MODE.DURATION");
        this.durationMillis = duration * 1000L;
    }

    public boolean hasReachMode(Player player) {
        return cache.contains(player.getUniqueId());
    }

    @Override
    public void onClick(Player player) {
        if (cannotUse(player)) return;
        if (hasCooldown(player)) return;

        takeItem(player);
        applyCooldown(player);

        cache.add(player.getUniqueId());

        for (String s : getLanguageConfig().getStringList("ABILITIES.REACH_MODE.ACTIVATED")) {
            player.sendMessage(s.replace("%duration%", String.valueOf(duration)));
        }

        final UUID uuid = player.getUniqueId();
        final long startTime = System.currentTimeMillis();

        new BukkitRunnable() {
            @Override
            public void run() {
                Player p = getInstance().getServer().getPlayer(uuid);
                if (p == null || !p.isOnline()) {
                    cancel();
                    return;
                }

                long expires = startTime + durationMillis;

                if (expires < System.currentTimeMillis()) {
                    cache.remove(uuid);
                    cancel();
                    sendXPBar(p, p.getExp(), p.getLevel(), 0.0F);
                    for (String s : getLanguageConfig().getStringList("ABILITIES.REACH_MODE.EXPIRED")) {
                        p.sendMessage(s.replace("%ability%", getDisplayName()));
                    }
                    return;
                }

                long millisLeft = expires - System.currentTimeMillis();
                float percentLeft = (float) millisLeft / durationMillis;
                sendXPBar(p, percentLeft, (int) (millisLeft / 1000), p.getExp());
            }
        }.runTaskTimer(getInstance(), 1L, 1L);
    }

    private void sendXPBar(Player player, float bar, int level, float exp) {
        CraftPlayer cp = (CraftPlayer) player;
        cp.getHandle().playerConnection.sendPacket(new PacketPlayOutExperience(bar, cp.getHandle().expTotal, level));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent e) {
        cache.remove(e.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInteract(PlayerInteractEvent e) {
        if (e.getAction() != Action.LEFT_CLICK_AIR && e.getAction() != Action.LEFT_CLICK_BLOCK) return;

        Player player = e.getPlayer();
        if (!cache.contains(player.getUniqueId())) return;

        Player target = getTargetPlayer(player, reach);
        if (target == null) return;

        e.setCancelled(true);
        CraftPlayer cp = (CraftPlayer) player;
        cp.getHandle().attack(((CraftPlayer) target).getHandle());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player)) return;
        if (e.getEntity() instanceof Player && e.getEntity().getUniqueId().equals(e.getDamager().getUniqueId())) return;

        Player attacker = (Player) e.getDamager();
        if (!cache.contains(attacker.getUniqueId())) return;

        getInstance().getVersionManager().getVersion().playEffect(
                attacker.getLocation().clone().add(0, 1, 0),
                "VILLAGER_THUNDERCLOUD",
                null
        );
    }

    private Player getTargetPlayer(Player player, double range) {
        Player targetPlayer = null;
        double targetDistance = Double.MAX_VALUE;
        Location playerPos = player.getEyeLocation();
        Vector playerDir = playerPos.getDirection();
        Vector3D playerStart = new Vector3D(playerPos);
        Vector3D playerEnd = playerStart.add(new Vector3D(playerDir).multiply(range));

        for (Player p : player.getWorld().getPlayers()) {
            if (p == player) continue;
            if (p.isDead() || !p.isOnline()) continue;

            Location targetEye = p.getEyeLocation();
            Vector3D targetPos = new Vector3D(p.getLocation());
            Vector3D min = targetPos.add(-0.5, 0.0, -0.5);
            Vector3D max = targetPos.add(0.5, 2.0, 0.5);

            double distance = targetEye.distanceSquared(playerPos);
            if (distance > (range + 0.5) * (range + 0.5)) continue;
            if (hasIntersection(playerStart, playerEnd, min, max) && distance < targetDistance) {
                targetPlayer = p;
                targetDistance = distance;
            }
        }
        return targetPlayer;
    }

    private boolean hasIntersection(Vector3D p1, Vector3D p2, Vector3D min, Vector3D max) {
        double epsilon = 0.0001;
        Vector3D d = p2.subtract(p1).multiply(0.5);
        Vector3D e = max.subtract(min).multiply(0.5);
        Vector3D c = p1.add(d).subtract(min.add(max).multiply(0.5));
        Vector3D ad = d.abs();

        if (Math.abs(c.x) > e.x + ad.x) return false;
        if (Math.abs(c.y) > e.y + ad.y) return false;
        if (Math.abs(c.z) > e.z + ad.z) return false;

        if (Math.abs(d.y * c.z - d.z * c.y) > e.y * ad.z + e.z * ad.y + epsilon) return false;
        if (Math.abs(d.z * c.x - d.x * c.z) > e.z * ad.x + e.x * ad.z + epsilon) return false;
        return Math.abs(d.x * c.y - d.y * c.x) <= e.x * ad.y + e.y * ad.x + epsilon;
    }

    private static class Vector3D {
        final double x, y, z;

        Vector3D(double x, double y, double z) {
            this.x = x; this.y = y; this.z = z;
        }

        Vector3D(Vector v) {
            this.x = v.getX(); this.y = v.getY(); this.z = v.getZ();
        }

        Vector3D(Location loc) {
            this.x = loc.getX(); this.y = loc.getY(); this.z = loc.getZ();
        }

        Vector3D add(Vector3D o) {
            return new Vector3D(x + o.x, y + o.y, z + o.z);
        }

        Vector3D add(double dx, double dy, double dz) {
            return new Vector3D(x + dx, y + dy, z + dz);
        }

        Vector3D subtract(Vector3D o) {
            return new Vector3D(x - o.x, y - o.y, z - o.z);
        }

        Vector3D multiply(double f) {
            return new Vector3D(x * f, y * f, z * f);
        }

        Vector3D abs() {
            return new Vector3D(Math.abs(x), Math.abs(y), Math.abs(z));
        }
    }
}
