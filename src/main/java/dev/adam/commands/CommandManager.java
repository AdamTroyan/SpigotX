package dev.adam.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

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

                PluginCommand pluginCommand = plugin.getCommand(cmd.name());
                if (pluginCommand != null) {
                    pluginCommand.setExecutor(this::executeCommand);
                } else {
                    Bukkit.getLogger().warning("[SpigotX] Command " + cmd.name() + " not found in plugin.yml!");
                }
            }
        }
    }

    private boolean executeCommand(CommandSender sender, org.bukkit.command.Command command, String label, String[] args) {
        Method method = commands.get(label.toLowerCase());
        if (method == null) return false;

        try {
            if (method.getParameterCount() == 1 && method.getParameterTypes()[0] == Player.class) {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("This command can only be run by a player.");
                    return true;
                }
                method.invoke(executor, player);
            }
            else if (method.getParameterCount() == 2 && method.getParameterTypes()[1] == String[].class) {
                method.invoke(executor, sender, args);
            }

            else {
                method.invoke(executor, sender);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }
}
