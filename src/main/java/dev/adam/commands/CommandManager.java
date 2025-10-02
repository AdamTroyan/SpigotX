package dev.adam.commands;

import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class CommandManager {

    private final JavaPlugin plugin;
    private final Object executor;
    private final Map<String, Method> commands = new HashMap<>();
    private final CommandMap commandMap;

    public CommandManager(JavaPlugin plugin, Object executor) {
        this.plugin = plugin;
        this.executor = executor;
        this.commandMap = CommandUtil.getCommandMap();
        registerCommands();
    }

    private void registerCommands() {
        for (Method method : executor.getClass().getMethods()) {
            if (!method.isAnnotationPresent(Command.class)) continue;

            dev.adam.commands.Command annotation = method.getAnnotation(dev.adam.commands.Command.class);
            String mainName = annotation.name().toLowerCase();

            List<String> aliases = new ArrayList<>();
            if (annotation.aliases() != null && !annotation.aliases().isBlank()) {
                for (String a : annotation.aliases().split("\\|")) {
                    if (!a.isBlank()) aliases.add(a.toLowerCase().trim());
                }
            }

            commands.put(mainName, method);
            for (String a : aliases) commands.put(a, method);

            DynamicCommand dynamicCommand = new DynamicCommand(annotation.name(), aliases) {
                @Override
                public boolean execute(CommandSender sender, String label, String[] args) {
                    if (!annotation.permission().isEmpty() && !sender.hasPermission(annotation.permission())) {
                        sender.sendMessage(annotation.permissionMessage().isEmpty()
                                ? "You do not have permission to use this command."
                                : annotation.permissionMessage());
                        return true;
                    }

                    Runnable task = () -> {
                        try {
                            if (method.getParameterCount() == 2 && method.getParameterTypes()[1] == String[].class) {
                                method.invoke(executor, sender, args);
                            } else if (method.getParameterCount() == 1 && method.getParameterTypes()[0].isAssignableFrom(sender.getClass())) {
                                method.invoke(executor, sender);
                            } else {
                                method.invoke(executor);
                            }
                        } catch (Exception e) {
                            sender.sendMessage("An error occurred while executing the command.");
                            e.printStackTrace();
                        }
                    };

                    if (method.isAnnotationPresent(AsyncCommand.class)) {
                        CompletableFuture.runAsync(task);
                    } else {
                        task.run();
                    }

                    return true;
                }
            };

            if (commandMap != null) {
                commandMap.register(plugin.getName(), dynamicCommand);
            }
        }
    }
}
