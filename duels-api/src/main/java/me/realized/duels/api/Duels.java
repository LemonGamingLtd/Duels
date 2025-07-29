package me.realized.duels.api;

import me.realized.duels.api.arena.ArenaManager;
import me.realized.duels.api.command.SubCommand;
import me.realized.duels.api.kit.KitManager;
import me.realized.duels.api.queue.DQueueManager;
import me.realized.duels.api.queue.sign.QueueSignManager;
import me.realized.duels.api.spectate.SpectateManager;
import me.realized.duels.api.user.UserManager;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;


public interface Duels extends Plugin {

    /**
     * Gets the UserManager singleton used by Duels.
     *
     * @return UserManager singleton
     */
    @NotNull
    UserManager getUserManager();


    /**
     * Gets the ArenaManager singleton used by Duels.
     *
     * @return ArenaManager singleton
     */
    @NotNull
    ArenaManager getArenaManager();


    /**
     * Gets the KitManager singleton used by Duels.
     *
     * @return KitManager singleton
     */
    @NotNull
    KitManager getKitManager();


    /**
     * Gets the SpectateManager singleton used by Duels.
     *
     * @return SpectateManager singleton
     * @since 3.4.1
     */
    @NotNull
    SpectateManager getSpectateManager();


    /**
     * Gets the DQueueManager singleton used by Duels.
     *
     * @return DQueueManager singleton
     */
    @NotNull
    DQueueManager getQueueManager();


    /**
     * Gets the QueueSignManager singleton used by Duels.
     *
     * @return QueueSignManager singleton
     */
    @NotNull
    QueueSignManager getQueueSignManager();


    /**
     * Registers a {@link SubCommand} to a Command registered by Duels.
     *
     * @param command    Name of the parent command to register the {@link SubCommand}.
     * @param subCommand {@link SubCommand} to register.
     * @return True if sub command was successfully registered. False otherwise.
     */
    boolean registerSubCommand(@NotNull final String command, @NotNull final SubCommand subCommand);


    /**
     * Registers a {@link Listener} that will be automatically unregistered on unload of Duels.
     *
     * @param listener {@link Listener} to register.
     * @since 3.1.2
     */
    void registerListener(@NotNull final Listener listener);


    /**
     * Reloads the plugin.
     *
     * @return True if reload was successful. False otherwise.
     */
    boolean reload();


    /**
     * Logs a message with {@link java.util.logging.Level#INFO}.
     *
     * @param message message to log.
     * @since 3.1.0
     */
    void info(@NotNull final String message);


    /**
     * Logs a message with {@link java.util.logging.Level#WARNING}.
     *
     * @param message message to log.
     * @since 3.1.0
     */
    void warn(@NotNull final String message);


    /**
     * Logs a message with {@link java.util.logging.Level#SEVERE}.
     *
     * @param message message to log.
     * @since 3.1.0
     */
    void error(@NotNull final String message);


    /**
     * Logs a message and the {@link Throwable} provided with {@link java.util.logging.Level#SEVERE}.
     *
     * @param message message to log.
     * @param thrown  {@link Throwable} to log.
     * @since 3.1.0
     */
    void error(@NotNull final String message, @NotNull Throwable thrown);


    /**
     * Current plugin version.
     *
     * @return version of the plugin.
     * @since 3.1.0
     */
    String getVersion();
}
