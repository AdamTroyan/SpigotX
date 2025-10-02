package dev.adam.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandMap;

import java.lang.reflect.Field;

public class CommandUtil {

    public static CommandMap getCommandMap() {
        try {
            Field field = Bukkit.getServer().getClass().getDeclaredField("commandMap");
            field.setAccessible(true);
            return (CommandMap) field.get(Bukkit.getServer());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
