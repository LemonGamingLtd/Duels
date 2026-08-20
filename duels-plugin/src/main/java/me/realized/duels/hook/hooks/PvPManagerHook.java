package me.realized.duels.hook.hooks;

import me.chancesd.pvpmanager.PvPManager;
import me.chancesd.pvpmanager.event.PlayerTagEvent;
import me.chancesd.pvpmanager.manager.PlayerManager;
import me.chancesd.pvpmanager.player.CombatPlayer;
import me.chancesd.pvpmanager.player.UntagReason;
import me.realized.duels.DuelsPlugin;
import me.realized.duels.arena.ArenaManagerImpl;
import me.realized.duels.config.Config;
import me.realized.duels.util.hook.PluginHook;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class PvPManagerHook extends PluginHook<DuelsPlugin> {

    public static final String NAME = "PvPManager";

    private final Config config;
    private final ArenaManagerImpl arenaManager;

    public PvPManagerHook(final DuelsPlugin plugin) {
        super(plugin, NAME);
        this.config = plugin.getConfiguration();
        this.arenaManager = plugin.getArenaManager();

        try {
            Class.forName("me.chancesd.pvpmanager.event.PlayerTagEvent");
        } catch (ClassNotFoundException ex) {
            throw new RuntimeException("This version of " + getName() + " is not supported. Please try upgrading to the latest version.");
        }

        Bukkit.getPluginManager().registerEvents(new PvPManagerListener(), plugin);
    }

    public boolean isTagged(final Player player) {
        final PlayerManager playerHandler = ((PvPManager) getPlugin()).getPlayerManager();
        if (playerHandler == null) {
            return false;
        }

        final CombatPlayer pvPlayer = playerHandler.get(player);
        return pvPlayer != null && pvPlayer.isInCombat();
    }

    public void untag(final Player player) {
        final PlayerManager playerHandler = ((PvPManager) getPlugin()).getPlayerManager();
        if (playerHandler == null) {
            return;
        }
        final CombatPlayer pvPlayer = playerHandler.get(player);
        if (pvPlayer == null) {
            return;
        }
        pvPlayer.untag(UntagReason.PLUGIN_API);
    }

    public class PvPManagerListener implements Listener {

        @EventHandler(ignoreCancelled = true)
        public void on(final PlayerTagEvent event) {
            if (!config.isPmPreventTag()) {
                return;
            }

            final Player player = event.getPlayer();
            final Player enemy = event.getEnemy();
            if (arenaManager.isInMatch(player) || (enemy != null && arenaManager.isInMatch(enemy))) {
                event.setCancelled(true);
            }
        }
    }
}
