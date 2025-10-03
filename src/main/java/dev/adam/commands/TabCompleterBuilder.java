package dev.adam.commands;

import org.bukkit.command.CommandSender;
import java.util.List;
import java.util.function.BiFunction;
import java.util.Collections;

public class TabCompleterBuilder {
    private BiFunction<CommandSender, String[], List<String>> completer;

    public TabCompleterBuilder completer(BiFunction<CommandSender, String[], List<String>> func) {
        this.completer = func;
        
        return this;
    }

    public List<String> complete(CommandSender sender, String[] args) {
        return completer != null ? completer.apply(sender, args) : Collections.emptyList();
    }
}