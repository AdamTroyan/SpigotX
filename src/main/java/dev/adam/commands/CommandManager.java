package dev.adam.commands;

import dev.adam.SpigotX;
import dev.adam.commands.annotations.AsyncCommand;
import dev.adam.commands.annotations.Command;
import dev.adam.commands.annotations.SubCommand;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Method;
import java.util.*;
import java.util.function.BiConsumer;

public class CommandManager {
    private final Plugin plugin;
    private final Map<String, Method> subCommandMap = new HashMap<>();
    private final Map<String, Object> subCommandInstanceMap = new HashMap<>();
    private final Set<String> registeredRoots = new HashSet<>();

    public CommandManager(Plugin plugin, Object commandClassInstance) {
        this.plugin = plugin;

        Arrays.stream(commandClassInstance.getClass().getDeclaredMethods())
                .filter(m -> m.isAnnotationPresent(Command.class))
                .forEach(method -> registerCommand(commandClassInstance, method));

        Arrays.stream(commandClassInstance.getClass().getDeclaredMethods())
                .filter(m -> m.isAnnotationPresent(SubCommand.class))
                .forEach(method -> registerSubCommand(commandClassInstance, method));
    }

    private void registerCommand(Object instance, Method method) {
        Command cmd = method.getAnnotation(Command.class);
        String name = cmd.name().toLowerCase().trim();
        registerMethodCommand(instance, method, name, cmd.permission(), cmd.description());
    }

    private void registerSubCommand(Object instance, Method method) {
        SubCommand subCmd = method.getAnnotation(SubCommand.class);
        String parent = subCmd.parent().toLowerCase().trim();
        String name = subCmd.name().toLowerCase().trim();
        String fullName = parent + " " + name;
        registerMethodCommand(instance, method, fullName, subCmd.permission(), subCmd.description());
    }

    private void registerMethodCommand(Object instance, Method method, String fullName, String permission, String description) {
        if (subCommandMap.containsKey(fullName)) {
            throw new IllegalArgumentException("Duplicate command name: " + fullName);
        }

        if (method.getParameterCount() != 2 ||
                !(method.getParameterTypes()[0] == Player.class || method.getParameterTypes()[0] == CommandSender.class) ||
                !method.getParameterTypes()[1].isArray() ||
                !method.getParameterTypes()[1].getComponentType().equals(String.class)) {
            throw new IllegalArgumentException("Command method " + method.getName() + " must have signature (Player/CommandSender, String[])");
        }

        subCommandMap.put(fullName, method);
        subCommandInstanceMap.put(fullName, instance);

        String root = fullName.split(" ")[0];

        if (registeredRoots.add(root)) {
            PluginCommand pluginCommand = ((JavaPlugin) plugin).getCommand(root);
            if (pluginCommand != null) {
                pluginCommand.setExecutor((sender, command, label, args) -> {
                    List<String> parts = new ArrayList<>();
                    parts.add(label.toLowerCase());
                    for (String arg : args) parts.add(arg.toLowerCase());

                    for (int i = parts.size(); i > 0; i--) {
                        String tryCmd = String.join(" ", parts.subList(0, i));
                        if (subCommandMap.containsKey(tryCmd)) {
                            Method m = subCommandMap.get(tryCmd);
                            Object inst = subCommandInstanceMap.get(tryCmd);
                            String[] remainingArgs = parts.subList(i, parts.size()).toArray(new String[0]);
                            boolean async = m.isAnnotationPresent(AsyncCommand.class);

                            Runnable run = () -> {
                                try {
                                    if (m.getParameterCount() == 2) {
                                        Class<?> paramType = m.getParameterTypes()[0];
                                        if (paramType == Player.class) {
                                            if (!(sender instanceof Player)) {
                                                sender.sendMessage("Only players can use this command.");
                                                return;
                                            }
                                            m.invoke(inst, sender, remainingArgs);
                                        } else if (paramType == CommandSender.class) {
                                            m.invoke(inst, sender, remainingArgs);
                                        } else {
                                            sender.sendMessage("Invalid command method signature.");
                                        }
                                    } else {
                                        sender.sendMessage("Invalid command method signature.");
                                    }
                                } catch (Exception e) {
                                    sender.sendMessage("Command error: " + e.getMessage());
                                    e.printStackTrace();
                                }
                            };
                            if (async) {
                                Bukkit.getScheduler().runTaskAsynchronously(plugin, run);
                            } else {
                                run.run();
                            }
                            return true;
                        }
                    }

                    sender.sendMessage("Available commands:");
                    subCommandMap.forEach((n, m) -> {
                        if (n.startsWith(label.toLowerCase())) {
                            String perm = getPermissionForCommand(m);
                            if (perm.isEmpty() || sender.hasPermission(perm)) {
                                String desc = getDescriptionForCommand(m);
                                sender.sendMessage("/" + n + " - " + desc);
                            }
                        }
                    });
                    return true;
                });

                pluginCommand.setTabCompleter((sender, command, label, args) -> {
                    List<String> completions = new ArrayList<>();
                    String base = label.toLowerCase();
                    if (args.length == 1) {
                        for (String key : subCommandMap.keySet()) {
                            if (key.startsWith(base + " ")) {
                                String sub = key.substring(base.length() + 1).split(" ")[0];
                                if (!completions.contains(sub)) {
                                    Method m = subCommandMap.get(key);
                                    String perm = getPermissionForCommand(m);
                                    if (perm.isEmpty() || sender.hasPermission(perm)) {
                                        completions.add(sub);
                                    }
                                }
                            }
                        }
                    }
                    return completions;
                });
            }
        }
    }

    private String getPermissionForCommand(Method method) {
        if (method.isAnnotationPresent(Command.class)) {
            return method.getAnnotation(Command.class).permission();
        }
        if (method.isAnnotationPresent(SubCommand.class)) {
            return method.getAnnotation(SubCommand.class).permission();
        }
        return "";
    }

    private String getDescriptionForCommand(Method method) {
        if (method.isAnnotationPresent(Command.class)) {
            return method.getAnnotation(Command.class).description();
        }
        if (method.isAnnotationPresent(SubCommand.class)) {
            return method.getAnnotation(SubCommand.class).description();
        }
        return "";
    }

    public static void register(String name, String desc, String perm, BiConsumer<CommandSender, String[]> exec) {
        JavaPlugin plugin = (JavaPlugin) SpigotX.getPlugin();
        PluginCommand cmd = plugin.getCommand(name);

        if (cmd == null) {
            plugin.getLogger().warning("Command /" + name + " not found in plugin.yml!");
            return;
        }

        cmd.setDescription(desc);

        if (!perm.isEmpty()) cmd.setPermission(perm);

        cmd.setExecutor((sender, command, label, args) -> {
            try {
                exec.accept(sender, args);
            } catch (Exception e) {
                sender.sendMessage("An error occurred.");
                e.printStackTrace();
            }
            return true;
        });
    }
}