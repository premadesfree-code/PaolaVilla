package me.keano.azurite.modules.pvpclass.type.yeti;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;

/**
 * Custom damage event used when a player freezes to death inside a Yeti's ice cube.
 */
@Getter
@Setter
public class YetiFreezeEvent extends EntityDamageEvent {

    private Player frozenBy;

    public YetiFreezeEvent(Entity damagee, Player frozenBy, DamageCause cause, double damage) {
        super(damagee, cause, damage);
        this.frozenBy = frozenBy;
    }
}
