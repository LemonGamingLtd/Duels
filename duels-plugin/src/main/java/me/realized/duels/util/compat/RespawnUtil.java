package me.realized.duels.util.compat;

import java.util.function.Consumer;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerRespawnEvent.RespawnReason;

/** Bridges Folia's asynchronous respawn, which is not exposed by the Bukkit API. */
public final class RespawnUtil {

    private RespawnUtil() {}

    public static void respawn(final Player player, final Runnable complete) throws ReflectiveOperationException {
        if (isFolia()) {
            final Object handle = player.getClass().getMethod("getHandle").invoke(player);
            // Same path used by Folia when the client requests respawn. Callback owns the destination region.
            handle.getClass().getMethod("respawn", Consumer.class, RespawnReason.class)
                .invoke(handle, (Consumer<Object>) ignored -> complete.run(), RespawnReason.PLUGIN);
        } else {
            player.spigot().respawn();
            complete.run();
        }
    }

    private static boolean isFolia() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return true;
        } catch (ClassNotFoundException ignored) {
            return false;
        }
    }
}
