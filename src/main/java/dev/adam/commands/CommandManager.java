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
import java.util.Arrays;
import java.util.function.BiConsumer;

public class CommandManager {
    private final Plugin plugin;

    public CommandManager(Plugin plugin, Object commandClassInstance) {
        this.plugin = plugin;
        Arrays.stream(commandClassInstance.getClass().getDeclaredMethods())
            .filter(m -> m.isAnnotationPresent(Command.class))
            .forEach(method -> registerAnnotatedCommand(commandClassInstance, method));
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