package dev.adam.commands;

import dev.adam.SpigotX;
import dev.adam.commands.annotations.AsyncCommand;
import dev.adam.commands.annotations.Command;
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
            .forEach(method -> {
                registerAnnotatedCommand(commandClassInstance, method);

                Command cmd = method.getAnnotation(Command.class);
                String name = cmd.name().toLowerCase().trim();
                
                subCommandMap.put(name, method);
                subCommandInstanceMap.put(name, commandClassInstance);

                String root = name.split(" ")[0];

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

                            sender.sendMessage("Unknown command.");

                            return true;
                        });
                    }
                }
            });
    }

    public static void register(String name, String desc, String perm, BiConsumer<CommandSender, String[]> exec) {
        JavaPlugin plugin = (JavaPlugin) SpigotX.getPlugin();
        PluginCommand cmd = plugin.getCommand(name);

        if (cmd == null) {
            plugin.getLogger().warning("[SpigotX] Command /" + name + " not found in plugin.yml!");
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

    private void registerAnnotatedCommand(Object instance, Method method) {
        Command cmd = method.getAnnotation(Command.class);
        String name = cmd.name();
        String desc = cmd.description();
        String perm = cmd.permission();
        String usage = cmd.usage();

        boolean async = method.isAnnotationPresent(AsyncCommand.class);

        new CommandBuilder()
            .name(name)
            .description(desc)
            .permission(perm)
            .executor((sender, args) -> {
                Runnable run = () -> {
                    try {
                        if (method.getParameterCount() == 2) {
                            Class<?> paramType = method.getParameterTypes()[0];
                            if (paramType == Player.class) {
                                if (!(sender instanceof Player)) {
                                    sender.sendMessage("Only players can use this command.");
                                    return;
                                }
                                method.invoke(instance, sender, args);
                            } else if (paramType == CommandSender.class) {
                                method.invoke(instance, sender, args);
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
            })
            .register();
    }
}