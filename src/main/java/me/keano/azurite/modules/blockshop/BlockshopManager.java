package me.keano.azurite.modules.blockshop;

import me.keano.azurite.HCF;
import me.keano.azurite.modules.events.sotw.SOTWManager;
import me.keano.azurite.modules.framework.Manager;
import me.keano.azurite.modules.teams.Team;
import me.keano.azurite.modules.teams.type.PlayerTeam;
import me.keano.azurite.modules.teams.type.SafezoneTeam;
import me.keano.azurite.modules.teams.type.WarzoneTeam;
import me.keano.azurite.modules.teams.type.WildernessTeam;
import org.bukkit.entity.Player;

/**
 * Copyright (c) 2025. Keano
 * Use or redistribution of source or file is
 * only permitted if given explicit permission.
 */
public class BlockshopManager extends Manager {

    public BlockshopManager(HCF instance) {
        super(instance);
    }

    public boolean cannotUseShop(Player player) {
        SOTWManager sotwManager = getInstance().getSotwManager();
        Team at = getInstance().getTeamManager().getClaimManager().getTeam(player.getLocation());

        if (getConfig().getBoolean("BLOCKSHOP_CONFIG.ONLY_SOTW") && !sotwManager.isActive()) {
            player.sendMessage(getLanguageConfig().getString("BLOCKSHOP_COMMAND.DISABLED_SOTW"));
            return true;

        } else if (!getConfig().getBoolean("BLOCKSHOP_CONFIG.ALLOW_WARZONE") && at instanceof WarzoneTeam) {
            player.sendMessage(getLanguageConfig().getString("BLOCKSHOP_COMMAND.DISABLED_WARZONE"));
            return true;

        } else if (!getConfig().getBoolean("BLOCKSHOP_CONFIG.ALLOW_WILDERNESS") && at instanceof WildernessTeam) {
            player.sendMessage(getLanguageConfig().getString("BLOCKSHOP_COMMAND.DISABLED_WILDERNESS"));
            return true;

        } else if (!getConfig().getBoolean("BLOCKSHOP_CONFIG.ALLOW_COMBAT") && getInstance().getTimerManager().getCombatTimer().hasTimer(player)) {
            player.sendMessage(getLanguageConfig().getString("BLOCKSHOP_COMMAND.DISABLED_COMBAT"));
            return true;

        } else if (at instanceof PlayerTeam) {
            PlayerTeam pt = (PlayerTeam) at;

            if (getConfig().getBoolean("BLOCKSHOP_CONFIG.ALLOW_OWN_TEAM") && pt.getPlayers().contains(player.getUniqueId())) {
                return false;

            } else if (!getConfig().getBoolean("BLOCKSHOP_CONFIG.ALLOW_ENEMY_TEAM") && !pt.getPlayers().contains(player.getUniqueId())) {
                player.sendMessage(getLanguageConfig().getString("BLOCKSHOP_COMMAND.DISABLED_ENEMY"));
                return true;
            }

        } else if (!(at instanceof SafezoneTeam)) {
            player.sendMessage(getLanguageConfig().getString("BLOCKSHOP_COMMAND.DISABLED_HERE"));
            return true;
        }

        return false;
    }
}
