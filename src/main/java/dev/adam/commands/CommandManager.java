package dev.adam.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class CommandManager {

    private final Map<String, Method> commands = new HashMap<>();
    private final Object executor;
    private final JavaPlugin plugin;

    public CommandManager(JavaPlugin plugin, Object executor) {
        this.plugin = plugin;
        this.executor = executor;
        registerCommands();
    }

    private void registerCommands() {
        for (Method method : executor.getClass().getMethods()) {
            if (method.isAnnotationPresent(Command.class)) {
                Command cmd = method.getAnnotation(Command.class);

                commands.put(cmd.name().toLowerCase(), method);

                if (!cmd.aliases().isEmpty()) {
                    for (String alias : cmd.aliases().split("\\|")) {
                        commands.put(alias.toLowerCase(), method);
                    }
                }

                PluginCommand pluginCommand = plugin.getCommand(cmd.name());
                if (pluginCommand != null) {
                    pluginCommand.setExecutor(this::executeCommand);

                    if (!cmd.aliases().isEmpty()) {
                        pluginCommand.setAliases(java.util.Arrays.asList(cmd.aliases().split("\\|")));
                    }

                    pluginCommand.setTabCompleter((sender, command, alias, args) -> {
                        if (method.isAnnotationPresent(TabComplete.class)) {
                            try {
                                Object result = method.getAnnotation(TabComplete.class)
                                        .handler().getDeclaredConstructor().newInstance()
                                        .complete(sender, args);
                                if (result instanceof java.util.List<?> list) {
                                    return list.stream().map(Object::toString).toList();
                                }
                            } catch (Exception ignored) {}
                        }
                        return java.util.Collections.emptyList();
                    });
                } else {
                    Bukkit.getLogger().warning("[CommandManager] Command " + cmd.name() + " not found in plugin.yml!");
                }
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
