package dev.adam.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Optional;

/**
 * Context wrapper for command execution with utility methods.
 */
public class CommandContext {
    private final CommandSender sender;
    private final String[] args;
    private final String label;

    /**
     * Creates a new CommandContext.
     * 
     * @param sender the command sender
     * @param args the command arguments
     * @param label the command label used
     */
    public CommandContext(CommandSender sender, String[] args, String label) {
        this.sender = sender;
        this.args = args != null ? args.clone() : new String[0];
        this.label = label != null ? label : "unknown";
    }

    /**
     * Gets the command sender.
     * 
     * @return the command sender
     */
    public CommandSender getSender() { return sender; }

    /**
     * Gets a copy of the command arguments.
     * 
     * @return a copy of the arguments array
     */
    public String[] getArgs() { return args.clone(); }

    /**
     * Gets the command label that was used.
     * 
     * @return the command label
     */
    public String getLabel() { return label; }

    /**
     * Gets the number of arguments.
     * 
     * @return the argument count
     */
    public int getArgCount() { return args.length; }

    /**
     * Gets the sender as a Player if they are one.
     * 
     * @return the player, or null if sender is not a player
     */
    public Player getPlayer() {
        return (sender instanceof Player) ? (Player) sender : null;
    }

    /**
     * Checks if the sender is a player.
     * 
     * @return true if sender is a player
     */
    public boolean isPlayer() {
        return sender instanceof Player;
    }

    /**
     * Gets an argument by index.
     * 
     * @param index the argument index
     * @return the argument, or null if index is out of bounds
     */
    public String getArg(int index) {
        return index >= 0 && index < args.length ? args[index] : null;
    }

    /**
     * Gets an argument by index with a default value.
     * 
     * @param index the argument index
     * @param defaultValue the default value if index is out of bounds
     * @return the argument or default value
     */
    public String getArg(int index, String defaultValue) {
        String arg = getArg(index);
        return arg != null ? arg : defaultValue;
    }

    /**
     * Gets an argument as an integer.
     * 
     * @param index the argument index
     * @return Optional containing the integer, or empty if not a valid integer
     */
    public Optional<Integer> getIntArg(int index) {
        String arg = getArg(index);
        if (arg == null) return Optional.empty();
        try {
            return Optional.of(Integer.parseInt(arg));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    /**
     * Gets an argument as a Player.
     * 
     * @param index the argument index
     * @return Optional containing the player, or empty if not found
     */
    public Optional<Player> getPlayerArg(int index) {
        String name = getArg(index);
        return name != null ? Optional.ofNullable(Bukkit.getPlayer(name)) : Optional.empty();
    }

    /**
     * Checks if the sender has a permission.
     * 
     * @param perm the permission to check
     * @return true if sender has permission
     */
    public boolean hasPermission(String perm) {
        return sender.hasPermission(perm);
    }

    /**
     * Sends a message to the command sender.
     * 
     * @param msg the message to send
     */
    public void send(String msg) { sender.sendMessage(msg); }

    /**
     * Sends a success message (green) to the command sender.
     * 
     * @param msg the message to send
     */
    public void sendSuccess(String msg) { sender.sendMessage("§a" + msg); }

    /**
     * Sends an error message (red) to the command sender.
     * 
     * @param msg the message to send
     */
    public void sendError(String msg) { sender.sendMessage("§c" + msg); }

    /**
     * Sends an info message (blue) to the command sender.
     * 
     * @param msg the message to send
     */
    public void sendInfo(String msg) { sender.sendMessage("§b" + msg); }

    /**
     * Ensures the sender is a player, sending an error if not.
     * 
     * @return true if sender is a player
     */
    public boolean ensurePlayer() {
        if (!isPlayer()) {
            sendError("Only players can use this command!");
            return false;
        }
        return true;
    }

    /**
     * Ensures minimum argument count, sending usage if not met.
     * 
     * @param minArgs the minimum required arguments
     * @param usage the usage message to send if not met
     * @return true if enough arguments are provided
     */
    public boolean ensureArgs(int minArgs, String usage) {
        if (args.length < minArgs) {
            sendError("Usage: " + usage);
            return false;
        }
        return true;
    }
}