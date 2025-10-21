package dev.adam;

import org.bukkit.event.Event;
import org.bukkit.plugin.Plugin;

import dev.adam.commands.CommandManager;
import dev.adam.events.EventBuilder;
import dev.adam.events.context.EventContext;
import dev.adam.gui.GUIListener;

import java.util.function.Consumer;

/**
 * Main entry point and utility class for the SpigotX framework.
 * 
 * <p>SpigotX is a comprehensive framework for Bukkit/Spigot plugin development that provides
 * simplified APIs for common plugin functionality including command management, event handling,
 * and GUI systems.</p>
 * 
 * <p>This class must be initialized with a plugin instance before using any SpigotX features.
 * It handles the registration of core listeners and provides static methods for accessing
 * framework functionality.</p>
 * 
 * <p>Usage example:</p>
 * <pre>{@code
 * public class MyPlugin extends JavaPlugin {
 *     @Override
 *     public void onEnable() {
 *         SpigotX.init(this);
 *         SpigotX.registerCommand(new MyCommands());
 *         SpigotX.on(PlayerJoinEvent.class, ctx -> {
 *             ctx.getPlayer().sendMessage("Welcome!");
 *         });
 *     }
 * }
 * }</pre>
 * 
 * @author Adam
 * @version 1.0
 * @since 1.0
 */
public class SpigotX {
    /** The plugin instance that initialized SpigotX */
    private static Plugin plugin;
    
    /** Flag to track whether the GUI listener has been registered */
    private static boolean guiListenerRegistered = false;

    /**
     * Initializes the SpigotX framework with the provided plugin instance.
     * 
     * <p>This method must be called before using any other SpigotX functionality.
     * It registers essential listeners and sets up the framework for use.</p>
     * 
     * <p>The GUI listener is automatically registered to handle inventory interactions
     * for the SpigotX GUI system. This listener is only registered once, even if
     * this method is called multiple times.</p>
     * 
     * @param pl the plugin instance to initialize SpigotX with
     * @throws IllegalArgumentException if the plugin parameter is null
     */
    public static void init(Plugin pl) {
        plugin = pl;
        System.out.println("SpigotX initialized!");

        if (!guiListenerRegistered) {
            pl.getServer().getPluginManager().registerEvents(new GUIListener(), pl);
            guiListenerRegistered = true;
        }
    }

    /**
     * Gets the plugin instance that was used to initialize SpigotX.
     * 
     * <p>This method provides access to the plugin instance for SpigotX components
     * that need to interact with the Bukkit API or access plugin resources.</p>
     * 
     * @return the plugin instance used during initialization
     * @throws IllegalStateException if SpigotX has not been initialized yet
     * @see #init(Plugin)
     */
    public static Plugin getPlugin() {
        if (plugin == null) throw new IllegalStateException("SpigotX.init(plugin) must be called first!");
        return plugin;
    }

    /**
     * Registers a command class instance with the SpigotX command management system.
     * 
     * <p>This method automatically discovers and registers all command methods in the
     * provided instance that are annotated with SpigotX command annotations. The
     * CommandManager handles reflection-based command registration and execution.</p>
     * 
     * <p>The commands instance should contain methods annotated with command annotations
     * that define the command name, description, permissions, and other properties.</p>
     * 
     * @param commandsInstance the instance containing command methods to register
     * @throws IllegalStateException if SpigotX has not been initialized yet
     * @throws IllegalArgumentException if the commandsInstance parameter is null
     * @see CommandManager
     */
    public static void registerCommand(Object commandsInstance) {
        if (plugin == null)
            throw new IllegalStateException("SpigotX.init(plugin) must be called before register!");
        new CommandManager(plugin, commandsInstance);
    }

    /**
     * Registers an event listener for the specified event class using a functional approach.
     * 
     * <p>This method provides a simplified way to register event handlers using lambda
     * expressions or method references. It creates an EventBuilder internally and
     * registers the handler with the Bukkit event system.</p>
     * 
     * <p>The handler receives an EventContext which provides access to the event instance
     * and additional utility methods for event handling.</p>
     * 
     * <p>Usage examples:</p>
     * <pre>{@code
     * // Using lambda expression
     * SpigotX.on(PlayerJoinEvent.class, ctx -> {
     *     Player player = ctx.getEvent().getPlayer();
     *     player.sendMessage("Welcome to the server!");
     * });
     * 
     * // Using method reference
     * SpigotX.on(PlayerDeathEvent.class, this::handlePlayerDeath);
     * }</pre>
     * 
     * @param <T> the type of event to listen for
     * @param eventClass the class of the event to listen for
     * @param handler the consumer function to handle the event
     * @throws IllegalStateException if SpigotX has not been initialized yet
     * @throws IllegalArgumentException if eventClass or handler parameters are null
     * @see EventBuilder
     * @see EventContext
     */
    public static <T extends Event> void on(Class<T> eventClass, Consumer<EventContext<T>> handler) {
        new EventBuilder<>(eventClass)
                .handle(handler)
                .register();
    }
}