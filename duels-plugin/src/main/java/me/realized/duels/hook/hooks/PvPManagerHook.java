package me.realized.duels.hook.hooks;

import me.NoChance.PvPManager.Events.PlayerTagEvent;
import me.NoChance.PvPManager.Managers.PlayerHandler;
import me.NoChance.PvPManager.PvPManager;
import me.NoChance.PvPManager.PvPlayer;
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
            Class.forName("me.NoChance.PvPManager.Events.PlayerTagEvent");
        } catch (ClassNotFoundException ex) {
            throw new RuntimeException("This version of " + getName() + " is not supported. Please try upgrading to the latest version.");
        }

        Bukkit.getPluginManager().registerEvents(new PvPManagerListener(), plugin);
    }

    public boolean isTagged(final Player player) {
        final PlayerHandler playerHandler = ((PvPManager) getPlugin()).getPlayerHandler();
        if (playerHandler == null) {
            return false;
        }

        final PvPlayer pvPlayer = playerHandler.get(player);
        return pvPlayer != null && pvPlayer.isInCombat();
    }

    public void untag(final Player player) {
        final PlayerHandler playerHandler = ((PvPManager) getPlugin()).getPlayerHandler();
        if (playerHandler == null) {
            return;
        }
        final PvPlayer pvPlayer = playerHandler.get(player);
        if (pvPlayer == null) {
            return;
        }
        playerHandler.untag(pvPlayer);
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
