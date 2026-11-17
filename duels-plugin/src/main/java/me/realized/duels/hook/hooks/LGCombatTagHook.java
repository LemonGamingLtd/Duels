package me.realized.duels.hook.hooks;

import ltd.lemongaming.combattag.api.CombatTagAPI;
import ltd.lemongaming.combattag.api.UntagReason;
import ltd.lemongaming.combattag.api.event.PlayerCombatTagEvent;
import me.realized.duels.DuelsPlugin;
import me.realized.duels.arena.ArenaManagerImpl;
import me.realized.duels.config.Config;
import me.realized.duels.util.hook.PluginHook;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class LGCombatTagHook extends PluginHook<DuelsPlugin> {

    public static final String NAME = "LGCombatTag";

    private final Config config;
    private final ArenaManagerImpl arenaManager;
    private final CombatTagAPI api;

    public LGCombatTagHook(final DuelsPlugin plugin) {
        super(plugin, NAME);
        this.config = plugin.getConfiguration();
        this.arenaManager = plugin.getArenaManager();
        this.api = CombatTagAPI.get();
        Bukkit.getPluginManager().registerEvents(new LGCombatTagListener(), plugin);
    }

    public boolean isTagged(final Player player) {
        return config.isLgctPreventDuel() && api.isInCombat(player);
    }

    public void untag(final Player player) {
        api.untag(player, UntagReason.PLUGIN);
    }

    public class LGCombatTagListener implements Listener {

        @EventHandler(ignoreCancelled = true)
        public void on(final PlayerCombatTagEvent event) {
            if (!config.isLgctPreventTag()) {
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
