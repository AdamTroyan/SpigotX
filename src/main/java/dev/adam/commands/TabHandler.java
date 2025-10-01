package dev.adam.commands;

import org.bukkit.command.CommandSender;

public interface TabHandler {
    java.util.List<String> complete(CommandSender sender, String[] args);
}
