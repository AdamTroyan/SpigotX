package dev.adam.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import java.util.List;

public abstract class DynamicCommand extends Command {

    public DynamicCommand(String name, List<String> aliases) {
        super(name);
        if (aliases != null && !aliases.isEmpty()) {
            setAliases(aliases);
        }
    }

    @Override
    public abstract boolean execute(CommandSender sender, String label, String[] args);
}
