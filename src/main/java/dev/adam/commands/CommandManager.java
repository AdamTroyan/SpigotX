package dev.adam.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Constructor;
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

            Command cmdAnnotation = method.getAnnotation(Command.class);
            String mainName = cmdAnnotation.name().toLowerCase();

            List<String> aliasList = new ArrayList<>();
            if (!cmdAnnotation.aliases().isEmpty()) {
                for (String alias : cmdAnnotation.aliases().split("\\|")) {
                    alias = alias.trim().toLowerCase();
                    if (!alias.isEmpty()) aliasList.add(alias);
                }
            }

            commands.put(mainName, method);
            for (String alias : aliasList) commands.put(alias, method);

            PluginCommand pluginCommand;
            try {
                Constructor<PluginCommand> constructor =
                        PluginCommand.class.getDeclaredConstructor(String.class, JavaPlugin.class);
                constructor.setAccessible(true);
                pluginCommand = constructor.newInstance(cmdAnnotation.name(), plugin);
            } catch (Exception e) {
                Bukkit.getLogger().warning("[CommandManager] Failed to create command " + cmdAnnotation.name());
                e.printStackTrace();
                continue;
            }

            pluginCommand.setExecutor(this::executeCommand);
            pluginCommand.setAliases(aliasList);
            pluginCommand.setDescription(cmdAnnotation.description());
            pluginCommand.setUsage(cmdAnnotation.usage());

            if (!cmdAnnotation.permission().isEmpty()) {
                pluginCommand.setPermission(cmdAnnotation.permission());
                pluginCommand.setPermissionMessage(cmdAnnotation.permissionMessage());
            }

            pluginCommand.setTabCompleter((sender, command, label, args) -> {
                if (method.isAnnotationPresent(TabComplete.class)) {
                    try {
                        Object handler = method.getAnnotation(TabComplete.class)
                                .handler().getDeclaredConstructor().newInstance();
                        Object result = handler.getClass()
                                .getMethod("complete", CommandSender.class, String[].class)
                                .invoke(handler, sender, args);
                        if (result instanceof List<?> list) return list.stream().map(Object::toString).toList();
                    } catch (Exception ignored) {}
                }
                return Collections.emptyList();
            });

            if (commandMap != null) {
                commandMap.register(plugin.getName(), pluginCommand);
            }
        }
    }

    private boolean executeCommand(CommandSender sender, org.bukkit.command.Command command, String label, String[] args) {
        Method method = commands.get(label.toLowerCase());
        if (method == null) return false;

        Command cmd = method.getAnnotation(Command.class);

        if (!cmd.permission().isEmpty() && !sender.hasPermission(cmd.permission())) {
            sender.sendMessage(cmd.permissionMessage().isEmpty()
                    ? "You do not have permission to use this command."
                    : cmd.permissionMessage());
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

        if (async) CompletableFuture.runAsync(task);
        else task.run();

        return true;
    }
}