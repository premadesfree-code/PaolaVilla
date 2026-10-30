package me.keano.azurite.modules.pvpclass.type.yeti;

import me.keano.azurite.utils.Serializer;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * The Yeti snowball consumable. When a Yeti throws a snowball and hits
 * another Yeti class player, configurable effects are applied to the target.
 */
public class YetiSnowballItem implements Listener {

    private final YetiClass yetiClass;
    private final Set<UUID> trackedSnowballs;

    public YetiSnowballItem(YetiClass yetiClass) {
        this.yetiClass = yetiClass;
        this.trackedSnowballs = new HashSet<>();
    }

    @EventHandler
    public void onLaunch(ProjectileLaunchEvent event) {
        if (!(event.getEntity() instanceof Snowball)) return;
        if (!(event.getEntity().getShooter() instanceof Player)) return;

        Player shooter = (Player) event.getEntity().getShooter();
        if (!yetiClass.getPlayers().contains(shooter.getUniqueId())) return;

        Material snowballMaterial = me.keano.azurite.utils.ItemUtils.getMat(
                yetiClass.getClassesConfig().getString("YETI_CLASS.SNOWBALL_ITEM.MATERIAL"));

        ItemStack hand = yetiClass.getManager().getItemInHand(shooter);
        if (hand == null || hand.getType() != snowballMaterial) return;
        if (hand.hasItemMeta() && hand.getItemMeta().hasLore()) return;

        if (yetiClass.checkYeti(shooter, true)) {
            event.setCancelled(true);
            return;
        }

        if (yetiClass.getSnowballCooldown().hasCooldown(shooter)) {
            shooter.sendMessage(yetiClass.getLanguageConfig().getString("PVP_CLASSES.YETI_CLASS.SNOWBALL_COOLDOWN")
                    .replace("%seconds%", yetiClass.getSnowballCooldown().getRemaining(shooter))
            );
            event.setCancelled(true);
            return;
        }

        yetiClass.getSnowballCooldown().applyCooldown(shooter, yetiClass.getSnowballCooldownSeconds());
        yetiClass.getManager().takeItemInHand(shooter, 1);
        yetiClass.getInstance().getTimerManager().getCombatTimer().applyTimer(shooter);

        trackedSnowballs.add(event.getEntity().getUniqueId());
    }

    @EventHandler
    public void onHit(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Snowball)) return;
        if (!(event.getEntity() instanceof Player)) return;

        Snowball snowball = (Snowball) event.getDamager();
        if (!trackedSnowballs.remove(snowball.getUniqueId())) return;

        Player target = (Player) event.getEntity();
        if (!yetiClass.getPlayers().contains(target.getUniqueId())) return;

        List<PotionEffect> effects = yetiClass.getClassesConfig().getStringList("YETI_CLASS.SNOWBALL_ITEM.EFFECTS")
                .stream()
                .map(Serializer::getEffect)
                .collect(Collectors.toList());

        for (PotionEffect effect : effects) {
            if (effect != null) target.addPotionEffect(effect, true);
        }
    }
}
