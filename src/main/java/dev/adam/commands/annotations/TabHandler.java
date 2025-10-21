package dev.adam.commands.annotations;

import java.util.List;

import org.bukkit.command.CommandSender;

/**
 * Interface for handling tab completion in commands.
 * 
 * <p>Implementations of this interface provide tab completion functionality
 * for command arguments based on the current context.</p>
 * 
 * @author Adam
 * @since 1.0
 */
public interface TabHandler {
    /**
     * Provides tab completion suggestions for command arguments.
     * 
     * @param sender the command sender requesting tab completion
     * @param args the current command arguments
     * @return a list of completion suggestions
     */
    List<String> complete(CommandSender sender, String[] args);
}
