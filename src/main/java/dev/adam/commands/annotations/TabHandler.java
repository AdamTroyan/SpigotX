package dev.adam.commands.annotations;

import java.util.List;

import org.bukkit.command.CommandSender;

public interface TabHandler {
    List<String> complete(CommandSender sender, String[] args);
}
