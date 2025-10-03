package dev.adam.commands;

import dev.adam.SpigotX;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.function.BiConsumer;

public class CommandManager {
    public static void register(String name, String desc, String perm, BiConsumer<CommandSender, String[]> exec) {
        JavaPlugin plugin = (JavaPlugin) SpigotX.getPlugin();
        PluginCommand cmd = plugin.getCommand(name);
        if (cmd == null) {
            dev.adam.logging.Logger.warn("Command /" + name + " not found in plugin.yml!");
            return;
        }
        cmd.setDescription(desc);
        if (!perm.isEmpty()) cmd.setPermission(perm);
        cmd.setExecutor((sender, command, label, args) -> {
            try {
                exec.accept(sender, args);
            } catch (Exception e) {
                dev.adam.logging.Logger.error("Error in command /" + name + ": " + e.getMessage());
                sender.sendMessage("§cAn error occurred.");
            }
            return true;
        });
    }
}