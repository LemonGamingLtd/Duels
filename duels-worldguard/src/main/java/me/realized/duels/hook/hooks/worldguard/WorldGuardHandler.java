package me.realized.duels.hook.hooks.worldguard;

import java.util.Collection;
import org.bukkit.entity.Player;

public interface WorldGuardHandler {

    String findRegion(final Player player, final Collection<String> regions);

    /**
     * @return true if WorldGuard would cancel an item drop by this player where they stand (item-drop flag).
     */
    boolean blocksItemDrop(final Player player);
}
