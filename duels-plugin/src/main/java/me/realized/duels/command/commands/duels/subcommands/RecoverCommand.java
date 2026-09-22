package me.realized.duels.command.commands.duels.subcommands;

import me.realized.duels.DuelsPlugin;
import me.realized.duels.command.BaseCommand;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class RecoverCommand extends BaseCommand {

    public RecoverCommand(final DuelsPlugin plugin) {
        super(plugin, "recover", "recover [player]", "Respawns and recovers a stranded duelist.", 2, false);
    }

    @Override
    protected void execute(final CommandSender sender, final String label, final String[] args) {
        final Player player = Bukkit.getPlayerExact(args[1]);
        if (player == null) {
            sender.sendMessage("That player must be online on this server.");
            return;
        }
        plugin.getScheduler().runTaskAtEntity(player, () -> {
            if (arenaManager.isInMatch(player)) {
                sender.sendMessage("Cannot recover a player in an active match.");
                return;
            }
            playerManager.recover(player, true);
        });
        sender.sendMessage("Recovery requested for " + player.getName() + ". Check that they respawn and leave the arena.");
    }
}
