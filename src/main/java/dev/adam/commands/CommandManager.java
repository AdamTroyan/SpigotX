package dev.adam.commands;

import dev.adam.SpigotX;
import dev.adam.commands.annotations.Command;
import dev.adam.commands.annotations.SubCommand;
import dev.adam.commands.annotations.TabComplete;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Method;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

/**
 * Simple and powerful command management system.
 * 
 * <p>This class provides a comprehensive command management system for Bukkit plugins,
 * supporting both annotation-based and programmatic command registration. It handles
 * command execution, permission checking, tab completion, and sub-command routing.</p>
 * 
 * <p>Features include:</p>
 * <ul>
 *   <li>Annotation-based command registration using {@link Command} and {@link SubCommand}</li>
 *   <li>Programmatic command registration with fluent API</li>
 *   <li>Automatic tab completion support</li>
 *   <li>Permission-based command access control</li>
 *   <li>Asynchronous command execution support</li>
 *   <li>Sub-command routing and nested command structures</li>
 * </ul>
 * 
 * @author Adam
 * @version 1.0
 * @since 1.0
 */
public class CommandManager {
    /** Map of registered commands keyed by their full command path */
    private static final Map<String, RegisteredCommand> commands = new HashMap<>();
    
    /** Map of custom tab completers keyed by command name */
    private static final Map<String, BiFunction<CommandSender, String[], List<String>>> tabCompleters = new HashMap<>();
    
    /** Set of root commands that have been registered with Bukkit */
    private static final Set<String> registeredRoots = new HashSet<>();

    /**
     * Creates a new CommandManager and registers commands from the instance.
     * 
     * <p>This constructor scans the provided command instance for methods annotated with
     * {@link Command}, {@link SubCommand}, or {@link TabComplete} and automatically
     * registers them with the command system.</p>
     * 
     * @param plugin the plugin instance that owns these commands
     * @param commandInstance the object containing command methods
     * @throws IllegalArgumentException if plugin or commandInstance is null
     */
    public CommandManager(org.bukkit.plugin.Plugin plugin, Object commandInstance) {
        if (plugin == null) {
            throw new IllegalArgumentException("Plugin cannot be null");
        }
        if (commandInstance == null) {
            throw new IllegalArgumentException("Command instance cannot be null");
        }

        Method[] methods = commandInstance.getClass().getDeclaredMethods();
        
        for (Method method : methods) {
            if (method.isAnnotationPresent(Command.class)) {
                registerCommand(commandInstance, method);
            } else if (method.isAnnotationPresent(SubCommand.class)) {
                registerSubCommand(commandInstance, method);
            } else if (method.isAnnotationPresent(TabComplete.class)) {
                registerTabCompleter(commandInstance, method);
            }
        }
    }

    /**
     * Registers commands from an annotated class instance.
     * 
     * <p>This method scans the provided instance for methods annotated with command
     * annotations and registers them with the command system. This is useful when
     * you want to register commands without creating a CommandManager instance.</p>
     * 
     * @param commandInstance the object containing command methods
     * @throws IllegalArgumentException if commandInstance is null
     */
    public static void register(Object commandInstance) {
        Method[] methods = commandInstance.getClass().getDeclaredMethods();
        
        // Register commands and subcommands
        for (Method method : methods) {
            if (method.isAnnotationPresent(Command.class)) {
                registerCommand(commandInstance, method);
            } else if (method.isAnnotationPresent(SubCommand.class)) {
                registerSubCommand(commandInstance, method);
            } else if (method.isAnnotationPresent(TabComplete.class)) {
                registerTabCompleter(commandInstance, method);
            }
        }
    }

    /**
     * Registers a command with full configuration.
     * 
     * <p>This method allows programmatic registration of commands with complete
     * configuration including name, description, permission, and executor.</p>
     * 
     * @param name the command name
     * @param description the command description
     * @param permission the required permission (empty string for no permission)
     * @param executor the command executor function
     */
    public static void register(String name, String description, String permission, 
                               BiConsumer<CommandSender, String[]> executor) {
        registerSimple(name, description, permission, false, executor);
    }

    /**
     * Registers an asynchronous command.
     * 
     * <p>Commands registered with this method will be executed on a separate thread,
     * preventing them from blocking the main server thread during execution.</p>
     * 
     * @param name the command name
     * @param description the command description
     * @param permission the required permission (empty string for no permission)
     * @param executor the command executor function
     */
    public static void registerAsync(String name, String description, String permission,
                                   BiConsumer<CommandSender, String[]> executor) {
        registerSimple(name, description, permission, true, executor);
    }

    /**
     * Registers a command with CommandContext executor.
     * 
     * <p>This method registers a command that uses {@link CommandContext} for
     * enhanced command handling with utility methods and contextual information.</p>
     * 
     * @param name the command name
     * @param description the command description
     * @param permission the required permission (empty string for no permission)
     * @param executor the context executor function
     */
    public static void registerContext(String name, String description, String permission,
                                     BiConsumer<CommandContext, Void> executor) {
        register(name, description, permission, (sender, args) -> {
            CommandContext ctx = new CommandContext(sender, args, name);
            executor.accept(ctx, null);
        });
    }

    /**
     * Registers a player-only command.
     * 
     * <p>Commands registered with this method can only be executed by players.
     * If a non-player (console, command block) tries to execute the command,
     * they will receive an error message.</p>
     * 
     * @param name the command name
     * @param description the command description
     * @param permission the required permission (empty string for no permission)
     * @param executor the player executor function
     */
    public static void registerPlayer(String name, String description, String permission,
                                    BiConsumer<Player, String[]> executor) {
        register(name, description, permission, (sender, args) -> {
            if (sender instanceof Player) {
                executor.accept((Player) sender, args);
            } else {
                sender.sendMessage("§cOnly players can use this command!");
            }
        });
    }

    /**
     * Registers a command method annotated with {@link Command}.
     * 
     * @param instance the object instance containing the method
     * @param method the method to register as a command
     */
    private static void registerCommand(Object instance, Method method) {
        Command cmd = method.getAnnotation(Command.class);
        String name = cmd.name().toLowerCase();
        
        validateMethod(method, name);
        
        RegisteredCommand regCmd = new RegisteredCommand(
            instance, method, name, cmd.permission(), cmd.description(), cmd.async()
        );
        
        commands.put(name, regCmd);
        setupBukkitCommand(name);
    }

    /**
     * Registers a sub-command method annotated with {@link SubCommand}.
     * 
     * @param instance the object instance containing the method
     * @param method the method to register as a sub-command
     */
    private static void registerSubCommand(Object instance, Method method) {
        SubCommand subCmd = method.getAnnotation(SubCommand.class);
        String parent = subCmd.parent().toLowerCase();
        String name = subCmd.name().toLowerCase();
        String fullName = parent + " " + name;
        
        validateMethod(method, fullName);
        
        RegisteredCommand regCmd = new RegisteredCommand(
            instance, method, fullName, subCmd.permission(), subCmd.description(), subCmd.async()
        );
        
        commands.put(fullName, regCmd);
        setupBukkitCommand(parent);
    }

    /**
     * Registers a tab completion method annotated with {@link TabComplete}.
     * 
     * @param instance the object instance containing the method
     * @param method the method to register as a tab completer
     */
    private static void registerTabCompleter(Object instance, Method method) {
        TabComplete tabComplete = method.getAnnotation(TabComplete.class);
        String command = tabComplete.command().toLowerCase();
        
        tabCompleters.put(command, (sender, args) -> {
            try {
                method.setAccessible(true);
                @SuppressWarnings("unchecked")
                List<String> result = (List<String>) method.invoke(instance, sender, args);
                return result != null ? result : Collections.emptyList();
            } catch (Exception e) {
                return Collections.emptyList();
            }
        });
    }

    /**
     * Registers a simple programmatic command.
     * 
     * @param name the command name
     * @param description the command description
     * @param permission the required permission
     * @param async whether the command should run asynchronously
     * @param executor the command executor
     */
    private static void registerSimple(String name, String description, String permission,
                                     boolean async, BiConsumer<CommandSender, String[]> executor) {
        RegisteredCommand regCmd = new RegisteredCommand(
            null, null, name, permission, description, async, executor
        );
        
        commands.put(name, regCmd);
        setupBukkitCommand(name);
    }

    /**
     * Validates that a command method has the correct signature.
     * 
     * @param method the method to validate
     * @param commandName the command name for error messages
     * @throws IllegalArgumentException if the method signature is invalid
     */
    private static void validateMethod(Method method, String commandName) {
        if (method.getParameterCount() != 2) {
            throw new IllegalArgumentException("Command method '" + commandName + "' must have 2 parameters");
        }

        Class<?>[] paramTypes = method.getParameterTypes();
        boolean validFirst = paramTypes[0] == Player.class || paramTypes[0] == CommandSender.class || paramTypes[0] == CommandContext.class;
        boolean validSecond = paramTypes[1] == String[].class;

        if (!validFirst || !validSecond) {
            throw new IllegalArgumentException("Command method '" + commandName + "' has invalid signature");
        }
    }

    /**
     * Sets up a Bukkit command with executor and tab completer.
     * 
     * @param rootCommand the root command name to setup
     */
    private static void setupBukkitCommand(String rootCommand) {
        if (!registeredRoots.add(rootCommand)) return;

        JavaPlugin plugin = (JavaPlugin) SpigotX.getPlugin();
        PluginCommand pluginCommand = plugin.getCommand(rootCommand);
        
        if (pluginCommand == null) {
            plugin.getLogger().warning("Command /" + rootCommand + " not found in plugin.yml!");
            return;
        }

        pluginCommand.setExecutor((sender, command, label, args) -> {
            return executeCommand(sender, label, args);
        });

        pluginCommand.setTabCompleter((sender, command, label, args) -> {
            return getTabCompletions(sender, label, args);
        });
    }

    /**
     * Executes a command based on the sender, label, and arguments.
     * 
     * @param sender the command sender
     * @param label the command label that was used
     * @param args the command arguments
     * @return true if the command was handled, false otherwise
     */
    private static boolean executeCommand(CommandSender sender, String label, String[] args) {
        // Try to find matching command
        List<String> parts = new ArrayList<>();
        parts.add(label.toLowerCase());
        for (String arg : args) {
            parts.add(arg.toLowerCase());
        }

        for (int i = parts.size(); i > 0; i--) {
            String commandPath = String.join(" ", parts.subList(0, i));
            RegisteredCommand regCmd = commands.get(commandPath);
            
            if (regCmd != null) {
                if (!regCmd.hasPermission(sender)) {
                    sender.sendMessage("§cYou don't have permission to use this command.");
                    return true;
                }

                String[] remainingArgs = parts.subList(i, parts.size()).toArray(new String[0]);
                executeRegisteredCommand(regCmd, sender, remainingArgs, commandPath);
                return true;
            }
        }

        // Show available commands
        showAvailableCommands(sender, label);
        return true;
    }

    /**
     * Executes a registered command with proper parameter handling.
     * 
     * @param regCmd the registered command to execute
     * @param sender the command sender
     * @param args the command arguments
     * @param commandPath the full command path that was matched
     */
    private static void executeRegisteredCommand(RegisteredCommand regCmd, CommandSender sender, 
                                               String[] args, String commandPath) {
        Runnable execution = () -> {
            try {
                if (regCmd.getExecutor() != null) {
                    // Simple executor
                    regCmd.getExecutor().accept(sender, args);
                } else {
                    // Method executor
                    Method method = regCmd.getMethod();
                    Object instance = regCmd.getInstance();
                    
                    Class<?> paramType = method.getParameterTypes()[0];
                    
                    if (paramType == Player.class) {
                        if (!(sender instanceof Player)) {
                            sender.sendMessage("§cThis command can only be used by players.");
                            return;
                        }
                        method.invoke(instance, (Player) sender, args);
                    } else if (paramType == CommandSender.class) {
                        method.invoke(instance, sender, args);
                    } else if (paramType == CommandContext.class) {
                        CommandContext ctx = new CommandContext(sender, args, commandPath);
                        method.invoke(instance, ctx, args);
                    }
                }
            } catch (Exception e) {
                sender.sendMessage("§cAn error occurred while executing the command.");
                e.printStackTrace();
            }
        };

        if (regCmd.isAsync()) {
            Bukkit.getScheduler().runTaskAsynchronously(SpigotX.getPlugin(), execution);
        } else {
            execution.run();
        }
    }

    /**
     * Shows available commands to the sender when no specific command is found.
     * 
     * @param sender the command sender to show commands to
     * @param label the base command label
     */
    private static void showAvailableCommands(CommandSender sender, String label) {
        List<String> available = new ArrayList<>();
        
        for (RegisteredCommand cmd : commands.values()) {
            if (cmd.getName().startsWith(label.toLowerCase()) && cmd.hasPermission(sender)) {
                String desc = cmd.getDescription().isEmpty() ? "No description" : cmd.getDescription();
                available.add("§7/" + cmd.getName() + " §8- §f" + desc);
            }
        }

        if (available.isEmpty()) {
            sender.sendMessage("§cNo commands available.");
        } else {
            sender.sendMessage("§6Available commands:");
            available.forEach(sender::sendMessage);
        }
    }

    /**
     * Gets tab completion suggestions for a command.
     * 
     * @param sender the command sender requesting completions
     * @param label the command label
     * @param args the current command arguments
     * @return a list of tab completion suggestions
     */
    private static List<String> getTabCompletions(CommandSender sender, String label, String[] args) {
        // Check for custom tab completer
        BiFunction<CommandSender, String[], List<String>> completer = tabCompleters.get(label.toLowerCase());
        if (completer != null) {
            return completer.apply(sender, args);
        }

        // Default subcommand completion
        if (args.length <= 1) {
            String partial = args.length == 0 ? "" : args[0].toLowerCase();
            
            return commands.keySet().stream()
                    .filter(key -> key.startsWith(label.toLowerCase() + " "))
                    .map(key -> key.substring(label.length() + 1).split(" ")[0])
                    .distinct()
                    .filter(sub -> sub.startsWith(partial))
                    .filter(sub -> {
                        RegisteredCommand cmd = commands.get(label.toLowerCase() + " " + sub);
                        return cmd != null && cmd.hasPermission(sender);
                    })
                    .sorted()
                    .collect(java.util.stream.Collectors.toList());
        }

        return Collections.emptyList();
    }

    /**
     * Internal command representation that holds command metadata and execution details.
     * 
     * <p>This class encapsulates all information needed to execute a registered command,
     * including the target instance, method, permissions, and execution settings.</p>
     */
    private static class RegisteredCommand {
        /** The object instance containing the command method (null for programmatic commands) */
        private final Object instance;
        
        /** The method to invoke when executing this command (null for programmatic commands) */
        private final Method method;
        
        /** The full name/path of the command */
        private final String name;
        
        /** The permission required to execute this command */
        private final String permission;
        
        /** The description of what this command does */
        private final String description;
        
        /** Whether this command should be executed asynchronously */
        private final boolean async;
        
        /** The executor function for programmatic commands (null for annotation-based commands) */
        private final BiConsumer<CommandSender, String[]> executor;

        /**
         * Creates a new RegisteredCommand for annotation-based commands.
         * 
         * @param instance the object instance containing the command method
         * @param method the method to invoke
         * @param name the command name
         * @param permission the required permission
         * @param description the command description
         * @param async whether to execute asynchronously
         */
        public RegisteredCommand(Object instance, Method method, String name, String permission, 
                               String description, boolean async) {
            this(instance, method, name, permission, description, async, null);
        }

        /**
         * Creates a new RegisteredCommand with full configuration.
         * 
         * @param instance the object instance (null for programmatic commands)
         * @param method the method to invoke (null for programmatic commands)
         * @param name the command name
         * @param permission the required permission
         * @param description the command description
         * @param async whether to execute asynchronously
         * @param executor the executor function (null for annotation-based commands)
         */
        public RegisteredCommand(Object instance, Method method, String name, String permission,
                               String description, boolean async, BiConsumer<CommandSender, String[]> executor) {
            this.instance = instance;
            this.method = method;
            this.name = name;
            this.permission = permission;
            this.description = description;
            this.async = async;
            this.executor = executor;
            
            if (method != null) {
                method.setAccessible(true);
            }
        }

        /**
         * Gets the object instance containing the command method.
         * 
         * @return the instance, or null for programmatic commands
         */
        public Object getInstance() { return instance; }
        
        /**
         * Gets the method to invoke when executing this command.
         * 
         * @return the method, or null for programmatic commands
         */
        public Method getMethod() { return method; }
        
        /**
         * Gets the full name/path of the command.
         * 
         * @return the command name
         */
        public String getName() { return name; }
        
        /**
         * Gets the permission required to execute this command.
         * 
         * @return the permission string, or empty string if no permission required
         */
        public String getPermission() { return permission; }
        
        /**
         * Gets the description of what this command does.
         * 
         * @return the command description
         */
        public String getDescription() { return description; }
        
        /**
         * Checks if this command should be executed asynchronously.
         * 
         * @return true if async execution is enabled
         */
        public boolean isAsync() { return async; }
        
        /**
         * Gets the executor function for programmatic commands.
         * 
         * @return the executor function, or null for annotation-based commands
         */
        public BiConsumer<CommandSender, String[]> getExecutor() { return executor; }

        /**
         * Checks if the given sender has permission to execute this command.
         * 
         * @param sender the command sender to check
         * @return true if sender has permission or no permission is required
         */
        public boolean hasPermission(CommandSender sender) {
            return permission.isEmpty() || sender.hasPermission(permission);
        }
    }
}