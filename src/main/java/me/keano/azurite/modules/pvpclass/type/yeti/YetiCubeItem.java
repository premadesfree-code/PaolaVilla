package me.keano.azurite.modules.pvpclass.type.yeti;

import me.keano.azurite.modules.teams.Team;
import me.keano.azurite.modules.teams.type.EventTeam;
import me.keano.azurite.modules.teams.type.SafezoneTeam;
import me.keano.azurite.utils.Serializer;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The Yeti cube consumable. Right-clicking packed ice while in Yeti class
 * spawns a hollow ice sphere around the player and grants configurable effects.
 * The ice disappears when the effects expire.
 */
public class YetiCubeItem implements Listener {

    private final YetiClass yetiClass;

    public YetiCubeItem(YetiClass yetiClass) {
        this.yetiClass = yetiClass;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (!event.getAction().name().contains("RIGHT")) return;

        Player player = event.getPlayer();
        ItemStack hand = yetiClass.getManager().getItemInHand(player);

        if (hand == null) return;
        if (!yetiClass.getPlayers().contains(player.getUniqueId())) return;

        Material cubeMaterial = me.keano.azurite.utils.ItemUtils.getMat(
                yetiClass.getClassesConfig().getString("YETI_CLASS.CUBE_ITEM.MATERIAL"));

        if (hand.getType() != cubeMaterial) return;
        if (hand.hasItemMeta() && hand.getItemMeta().hasLore()) return;

        if (yetiClass.checkYeti(player, true)) return;

        if (yetiClass.getCubeCooldown().hasCooldown(player)) {
            player.sendMessage(yetiClass.getLanguageConfig().getString("PVP_CLASSES.YETI_CLASS.CUBE_COOLDOWN")
                    .replace("%seconds%", yetiClass.getCubeCooldown().getRemaining(player))
            );
            return;
        }

        if (yetiClass.isInEventClaim(player)) {
            player.sendMessage(yetiClass.getLanguageConfig().getString("PVP_CLASSES.YETI_CLASS.CANNOT_USE_EVENT"));
            return;
        }

        yetiClass.getCubeCooldown().applyCooldown(player, yetiClass.getCubeCooldownSeconds());
        yetiClass.getManager().takeItemInHand(player, 1);
        yetiClass.getInstance().getTimerManager().getCombatTimer().applyTimer(player);

        Set<Location> blocks = placeCube(player);

        List<PotionEffect> effects = yetiClass.getClassesConfig().getStringList("YETI_CLASS.CUBE_ITEM.EFFECTS")
                .stream()
                .map(Serializer::getEffect)
                .collect(Collectors.toList());

        for (PotionEffect effect : effects) {
            if (effect != null) player.addPotionEffect(effect, true);
        }

        int longestDuration = effects.stream()
                .filter(e -> e != null)
                .mapToInt(PotionEffect::getDuration)
                .max()
                .orElse(20 * yetiClass.getClassesConfig().getInt("YETI_CLASS.CUBE_ITEM.DURATION_SECONDS"));

        final Set<Location> finalBlocks = blocks;

        new BukkitRunnable() {
            @Override
            public void run() {
                if (!yetiClass.getPlayers().contains(player.getUniqueId())) {
                    removeBlocks(finalBlocks);
                    return;
                }

                Set<Location> stored = yetiClass.getCubes().get(player.getUniqueId());
                if (stored != null && stored == finalBlocks) {
                    yetiClass.getCubes().remove(player.getUniqueId());
                }

                removeBlocks(finalBlocks);
            }

            private void removeBlocks(Set<Location> locations) {
                for (Location location : locations) {
                    Block block = location.getBlock();
                    if (block.getType() == Material.ICE) {
                        block.setType(Material.AIR);
                    }
                }
            }
        }.runTaskLater(yetiClass.getInstance(), longestDuration);
    }

    private Set<Location> placeCube(Player player) {
        Set<Location> blocks = new HashSet<>();
        Location origin = player.getLocation().clone().add(0, -1, 0);

        int centerX = origin.getBlockX();
        int centerY = origin.getBlockY();
        int centerZ = origin.getBlockZ();

        int size = yetiClass.getClassesConfig().getInt("YETI_CLASS.CUBE_ITEM.SIZE");
        double minDist = yetiClass.getClassesConfig().getDouble("YETI_CLASS.CUBE_ITEM.MIN_DISTANCE");
        double maxDist = yetiClass.getClassesConfig().getDouble("YETI_CLASS.CUBE_ITEM.MAX_DISTANCE");
        Material iceMaterial = me.keano.azurite.utils.ItemUtils.getMat(
                yetiClass.getClassesConfig().getString("YETI_CLASS.CUBE_ITEM.BLOCK_TYPE"));

        for (int x = centerX - size; x <= centerX + size; x++) {
            for (int z = centerZ - size; z <= centerZ + size; z++) {
                for (int y = centerY - size; y < centerY + size; y++) {
                    double distance = ((centerX - x) * (centerX - x)
                            + (centerZ - z) * (centerZ - z)
                            + (centerY - y) * (centerY - y));

                    if (distance < maxDist && distance >= minDist) {
                        Location location = new Location(origin.getWorld(), x, y + 1, z);
                        Block block = location.getBlock();

                        if (block == null || block.getType() != Material.AIR) continue;
                        if (isIllegal(location)) continue;

                        block.setType(iceMaterial);
                        blocks.add(location);
                    }
                }
            }
        }

        yetiClass.addCube(player, blocks);
        return blocks;
    }

    private boolean isIllegal(Location location) {
        Team team = yetiClass.getInstance().getTeamManager().getClaimManager().getTeam(location);

        if (team instanceof SafezoneTeam) return true;
        if (team instanceof EventTeam) return true;

        return false;
    }
}
