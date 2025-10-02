package dev.adam.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class CommandManager {

    private final Map<String, Method> commands = new HashMap<>();
    private final Object executor;
    private final JavaPlugin plugin;
    private final CommandMap commandMap;

    public CommandManager(JavaPlugin plugin, Object executor) {
        this.plugin = plugin;
        this.executor = executor;
        this.commandMap = getCommandMap();
        registerCommands();
    }

    private CommandMap getCommandMap() {
        try {
            Field field = Bukkit.getServer().getClass().getDeclaredField("commandMap");
            field.setAccessible(true);
            return (CommandMap) field.get(Bukkit.getServer());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void registerCommands() {
        for (Method method : executor.getClass().getMethods()) {
            if (!method.isAnnotationPresent(Command.class)) continue;

            Command cmd = method.getAnnotation(Command.class);
            String mainName = cmd.name().toLowerCase();

            commands.put(mainName, method);

            List<String> aliasList = new ArrayList<>();
            if (!cmd.aliases().isEmpty()) {
                for (String alias : cmd.aliases().split("\\|")) {
                    alias = alias.trim().toLowerCase();
                    if (!alias.isEmpty()) {
                        commands.put(alias, method);
                        aliasList.add(alias);
                    }
                }
            }

            try {
                PluginCommand pluginCommand = PluginCommand.class
                        .getDeclaredConstructor(String.class, JavaPlugin.class)
                        .newInstance(cmd.name(), plugin);

                pluginCommand.setExecutor(this::executeCommand);
                pluginCommand.setDescription(cmd.description());
                pluginCommand.setUsage(cmd.usage());
                pluginCommand.setAliases(aliasList);

                if (!cmd.permission().isEmpty()) {
                    pluginCommand.setPermission(cmd.permission());
                    pluginCommand.setPermissionMessage(cmd.permissionMessage().isEmpty()
                            ? "You do not have permission to use this command."
                            : cmd.permissionMessage());
                }

                pluginCommand.setTabCompleter((sender, command, label, args) -> {
                    if (method.isAnnotationPresent(TabComplete.class)) {
                        try {
                            Object result = method.getAnnotation(TabComplete.class)
                                    .handler().getDeclaredConstructor().newInstance()
                                    .complete(sender, args);
                            if (result instanceof List<?> list) {
                                return list.stream().map(Object::toString).toList();
                            }
                        } catch (Exception ignored) {}
                    }
                    return Collections.emptyList();
                });

                if (commandMap != null) {
                    commandMap.register(plugin.getName(), pluginCommand);
                }

            } catch (Exception e) {
                Bukkit.getLogger().warning("[CommandManager] Failed to register command: " + cmd.name());
                e.printStackTrace();
            }
        }
    }

    private boolean executeCommand(CommandSender sender, org.bukkit.command.Command command, String label, String[] args) {
        Method method = commands.get(label.toLowerCase());
        if (method == null) return false;

        Command cmd = method.getAnnotation(Command.class);

        if (!cmd.permission().isEmpty() && !sender.hasPermission(cmd.permission())) {
            String msg = cmd.permissionMessage().isEmpty()
                    ? "You do not have permission to use this command."
                    : cmd.permissionMessage();
            sender.sendMessage(msg);
            return true;
        }

        boolean async = method.isAnnotationPresent(AsyncCommand.class);

        Runnable task = () -> {
            try {
                if (method.getParameterCount() == 1 && method.getParameterTypes()[0] == Player.class) {
                    if (!(sender instanceof Player player)) {
                        sender.sendMessage("This command can only be run by a player.");
                        return;
                    }
                    method.invoke(executor, player);
                } else if (method.getParameterCount() == 2 && method.getParameterTypes()[1] == String[].class) {
                    method.invoke(executor, sender, args);
                } else {
                    method.invoke(executor, sender);
                }
            } catch (Exception e) {
                sender.sendMessage("An error occurred while executing this command.");
                e.printStackTrace();
            }
        };

        if (async) {
            CompletableFuture.runAsync(task);
        } else {
            task.run();
        }

        return true;
    }
}
