package me.realized.duels.player;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.base.Charsets;
import lombok.Getter;
import me.realized.duels.DuelsPlugin;
import me.realized.duels.config.Config;
import me.realized.duels.data.LocationData;
import me.realized.duels.data.PlayerData;
import me.realized.duels.hook.hooks.EssentialsHook;
import me.realized.duels.teleport.Teleport;
import me.realized.duels.util.Loadable;
import me.realized.duels.util.Log;
import me.realized.duels.util.io.FileUtil;
import me.realized.duels.util.json.JsonUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import me.realized.duels.util.compat.RespawnUtil;
import org.bukkit.event.player.PlayerJoinEvent;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages:
 * (1) player info cache for restoration after matches.
 * (2) lobby location for teleportation after matches.
 */
public class PlayerInfoManager implements Loadable {

    private static final String CACHE_FILE_NAME = "player-cache.json";

    private static final String LOBBY_FILE_NAME = "lobby.json";
    private static final String ERROR_LOBBY_LOAD = "Could not load lobby location!";
    private static final String ERROR_LOBBY_SAVE = "Could not save lobby location!";
    private static final String ERROR_LOBBY_DEFAULT = "Lobby location was not set, using %s's spawn location as default. Use the command /duels setlobby in-game to set the lobby location.";

    private final DuelsPlugin plugin;
    private final Config config;
    private final File cacheFile;
    private final File lobbyFile;

    private final Map<UUID, PlayerInfo> cache = new ConcurrentHashMap<>();
    private final Map<UUID, Player> respawning = new ConcurrentHashMap<>();

    private Teleport teleport;
    private EssentialsHook essentials;

    @Getter
    private Location lobby;

    public PlayerInfoManager(final DuelsPlugin plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfiguration();
        this.cacheFile = new File(plugin.getDataFolder(), CACHE_FILE_NAME);
        this.lobbyFile = new File(plugin.getDataFolder(), LOBBY_FILE_NAME);
        plugin.getScheduler().runTaskLater(() -> Bukkit.getPluginManager().registerEvents(new PlayerInfoListener(), plugin), 1L);
    }

    @Override
    public void handleLoad() throws IOException {
        this.teleport = plugin.getTeleport();
        this.essentials = plugin.getHookManager().getHook(EssentialsHook.class);

        if (FileUtil.checkNonEmpty(cacheFile, false)) {
            try (final Reader reader = new InputStreamReader(new FileInputStream(cacheFile), Charsets.UTF_8)) {
                final Map<UUID, PlayerData> data = JsonUtil.getObjectMapper().readValue(reader, new TypeReference<HashMap<UUID, PlayerData>>() {
                });

                if (data != null) {
                    for (final Map.Entry<UUID, PlayerData> entry : data.entrySet()) {
                        cache.put(entry.getKey(), entry.getValue().toPlayerInfo());
                    }
                }
            }

            cacheFile.delete();
        }

        if (FileUtil.checkNonEmpty(lobbyFile, false)) {
            try (final Reader reader = new InputStreamReader(new FileInputStream(lobbyFile), Charsets.UTF_8)) {
                this.lobby = JsonUtil.getObjectMapper().readValue(reader, LocationData.class).toLocation();
            } catch (IOException ex) {
                Log.error(this, ERROR_LOBBY_LOAD, ex);
            }
        }

        // If lobby is not found or invalid, use the default world's spawn location for lobby.
        if (lobby == null || lobby.getWorld() == null) {
            final World world = Bukkit.getWorlds().get(0);
            this.lobby = world.getSpawnLocation();
            Log.warn(this, String.format(ERROR_LOBBY_DEFAULT, world.getName()));
        }
    }

    @Override
    public void handleUnload() throws IOException {
        // Persist pending recoveries; respawning during plugin shutdown is unsafe on Folia.
        if (cache.isEmpty()) {
            return;
        }

        final Map<UUID, PlayerData> data = new HashMap<>();

        for (final Map.Entry<UUID, PlayerInfo> entry : cache.entrySet()) {
            data.put(entry.getKey(), PlayerData.fromPlayerInfo(entry.getValue()));
        }

        try (final Writer writer = new OutputStreamWriter(new FileOutputStream(cacheFile), Charsets.UTF_8)) {
            JsonUtil.getObjectWriter().writeValue(writer, data);
            writer.flush();
        }

        cache.clear();
    }

    /**
     * Sets a lobby location at given player's location.
     *
     * @param player Player to get location for lobby
     * @return true if setting lobby was successful, false otherwise
     */
    public boolean setLobby(final Player player) {
        final Location lobby = player.getLocation().clone();

        try (final Writer writer = new OutputStreamWriter(new FileOutputStream(lobbyFile), Charsets.UTF_8)) {
            JsonUtil.getObjectWriter().writeValue(writer, LocationData.fromLocation(lobby));
            writer.flush();
            this.lobby = lobby;
            return true;
        } catch (IOException ex) {
            Log.error(this, ERROR_LOBBY_SAVE, ex);
            return false;
        }
    }

    /**
     * Gets cached PlayerInfo instance for given player.
     *
     * @param player Player to get cached PlayerInfo instance
     * @return cached PlayerInfo instance or null if not found
     */
    public PlayerInfo get(final Player player) {
        return cache.get(player.getUniqueId());
    }

    /**
     * Creates a cached PlayerInfo instance for given player.
     *
     * @param player           Player to create a cached PlayerInfo instance
     * @param excludeInventory true to exclude inventory contents from being stored in PlayerInfo, false otherwise
     */
    public void create(final Player player, final boolean excludeInventory) {
        final PlayerInfo info = new PlayerInfo(player, excludeInventory);

        if (!config.isTeleportToLastLocation()) {
            info.setLocation(lobby.clone());
        }

        cache.put(player.getUniqueId(), info);
    }

    /**
     * Calls {@link #create(Player, boolean)} with excludeInventory defaulting to false.
     *
     * @see {@link #create(Player, boolean)}
     */
    public void create(final Player player) {
        create(player, false);
    }

    /**
     * Removes the given player from cache.
     *
     * @param player Player to remove from cache
     * @return Removed PlayerInfo instance or null if not found
     */
    public PlayerInfo remove(final Player player) {
        return cache.remove(player.getUniqueId());
    }

    /** Must run on the player's entity scheduler. Explicit recovery also supports lost caches. */
    public void recover(final Player player, final boolean explicit) {
        if (!player.isOnline() || plugin.getArenaManager().isInMatch(player)
            || (!explicit && get(player) == null) || respawning.containsKey(player.getUniqueId())) {
            return;
        }
        if (!player.isDead()) {
            restoreAfterRespawn(player, explicit);
            return;
        }
        if (respawning.putIfAbsent(player.getUniqueId(), player) != null) {
            return;
        }
        try {
            RespawnUtil.respawn(player, () -> {
                respawning.remove(player.getUniqueId(), player);
                restoreAfterRespawn(player, explicit);
            });
        } catch (ReflectiveOperationException | RuntimeException ex) {
            respawning.remove(player.getUniqueId(), player);
            plugin.getLogger().log(java.util.logging.Level.SEVERE,
                "Could not respawn " + player.getName() + "; saved duel data retained", ex);
        }
    }

    private void restoreAfterRespawn(final Player player, final boolean explicit) {
        if (!player.isOnline() || player.isDead() || plugin.getArenaManager().isInMatch(player)) {
            return;
        }
        final PlayerInfo info = get(player);
        if (info != null) {
            info.restore(player);
            cache.remove(player.getUniqueId(), info);
            teleport.tryTeleport(player, info.getLocation());
        } else if (explicit) {
            // An admin may recover an orphaned death after a restart without changing inventory.
            teleport.tryTeleport(player, lobby);
        }
    }

    private class PlayerInfoListener implements Listener {

        @EventHandler(priority = EventPriority.MONITOR)
        public void on(final PlayerJoinEvent event) {
            final Player player = event.getPlayer();
            plugin.getScheduler().runTaskLaterAtEntity(player, () -> recover(player, false), 1L);
        }

        @EventHandler(priority = EventPriority.MONITOR)
        public void on(final PlayerDeathEvent event) {
            final Player player = event.getEntity();
            if (get(player) != null) {
                // Finish death processing before requesting respawn. Disconnected players recover on join.
                plugin.getScheduler().runTaskLaterAtEntity(player, () -> recover(player, false), 2L);
            }
        }

        @EventHandler(priority = EventPriority.MONITOR)
        public void on(final PlayerRespawnEvent event) {
            final Player player = event.getPlayer();
            // The event fires before the player is alive and placed in the destination region.
            plugin.getScheduler().runTaskLaterAtEntity(player, () -> {
                if (!respawning.containsKey(player.getUniqueId())) {
                    restoreAfterRespawn(player, false);
                }
            }, 1L);
        }

        @EventHandler(priority = EventPriority.MONITOR)
        public void on(final PlayerQuitEvent event) {
            respawning.remove(event.getPlayer().getUniqueId(), event.getPlayer());
        }
    }
}
