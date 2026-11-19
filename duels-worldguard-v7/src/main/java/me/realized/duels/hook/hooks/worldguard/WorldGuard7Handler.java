package me.realized.duels.hook.hooks.worldguard;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.internal.platform.WorldGuardPlatform;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import java.util.Collection;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class WorldGuard7Handler implements WorldGuardHandler {

    @Override
    public String findRegion(final Player player, final Collection<String> regions) {
        final Location location = player.getLocation();
        final BlockVector3 vector = BlockVector3.at(location.getBlockX(), location.getBlockY(), location.getBlockZ());

        for (final ProtectedRegion region : WorldGuard.getInstance().getPlatform().getRegionContainer().get(BukkitAdapter.adapt(player.getWorld()))
            .getApplicableRegions(vector)) {
            if (regions.contains(region.getId())) {
                return region.getId();
            }
        }

        return null;
    }

    @Override
    public boolean blocksItemDrop(final Player player) {
        // Mirrors RegionProtectionListener#onSpawnEntity for a player-dropped item.
        final WorldGuardPlatform platform = WorldGuard.getInstance().getPlatform();
        final LocalPlayer localPlayer = WorldGuardPlugin.inst().wrapPlayer(player);

        if (!platform.getGlobalStateManager().get(localPlayer.getWorld()).useRegions) {
            return false;
        }

        if (platform.getSessionManager().hasBypass(localPlayer, localPlayer.getWorld())) {
            return false;
        }

        return !platform.getRegionContainer().createQuery().testBuild(localPlayer.getLocation(), localPlayer, Flags.ITEM_DROP);
    }
}
