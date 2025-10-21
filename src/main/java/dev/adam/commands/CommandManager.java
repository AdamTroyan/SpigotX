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
 */
public class CommandManager {
    private static final Map<String, RegisteredCommand> commands = new HashMap<>();
    private static final Map<String, BiFunction<CommandSender, String[], List<String>>> tabCompleters = new HashMap<>();
    private static final Set<String> registeredRoots = new HashSet<>();

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

    // Register annotation-based commands
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

    // Register simple programmatic command
    public static void register(String name, String description, String permission, 
                               BiConsumer<CommandSender, String[]> executor) {
        registerSimple(name, description, permission, false, executor);
    }

    // Register async programmatic command
    public static void registerAsync(String name, String description, String permission,
                                   BiConsumer<CommandSender, String[]> executor) {
        registerSimple(name, description, permission, true, executor);
    }

    // Register context-based command
    public static void registerContext(String name, String description, String permission,
                                     BiConsumer<CommandContext, Void> executor) {
        register(name, description, permission, (sender, args) -> {
            CommandContext ctx = new CommandContext(sender, args, name);
            executor.accept(ctx, null);
        });
    }

    // Register player-only command
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

    private static void registerSimple(String name, String description, String permission,
                                     boolean async, BiConsumer<CommandSender, String[]> executor) {
        RegisteredCommand regCmd = new RegisteredCommand(
            null, null, name, permission, description, async, executor
        );
        
        commands.put(name, regCmd);
        setupBukkitCommand(name);
    }

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

    // Internal command representation
    private static class RegisteredCommand {
        private final Object instance;
        private final Method method;
        private final String name;
        private final String permission;
        private final String description;
        private final boolean async;
        private final BiConsumer<CommandSender, String[]> executor;

        public RegisteredCommand(Object instance, Method method, String name, String permission, 
                               String description, boolean async) {
            this(instance, method, name, permission, description, async, null);
        }

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

        public Object getInstance() { return instance; }
        public Method getMethod() { return method; }
        public String getName() { return name; }
        public String getPermission() { return permission; }
        public String getDescription() { return description; }
        public boolean isAsync() { return async; }
        public BiConsumer<CommandSender, String[]> getExecutor() { return executor; }

        public boolean hasPermission(CommandSender sender) {
            return permission.isEmpty() || sender.hasPermission(permission);
        }
    }
}