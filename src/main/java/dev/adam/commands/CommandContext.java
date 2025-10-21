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

    public CommandContext(CommandSender sender, String[] args, String label) {
        this.sender = sender;
        this.args = args != null ? args.clone() : new String[0];
        this.label = label != null ? label : "unknown";
    }

    public CommandSender getSender() { return sender; }
    public String[] getArgs() { return args.clone(); }
    public String getLabel() { return label; }
    public int getArgCount() { return args.length; }

    public Player getPlayer() {
        return (sender instanceof Player) ? (Player) sender : null;
    }

    public boolean isPlayer() {
        return sender instanceof Player;
    }

    public String getArg(int index) {
        return index >= 0 && index < args.length ? args[index] : null;
    }

    public String getArg(int index, String defaultValue) {
        String arg = getArg(index);
        return arg != null ? arg : defaultValue;
    }

    public Optional<Integer> getIntArg(int index) {
        String arg = getArg(index);
        if (arg == null) return Optional.empty();
        try {
            return Optional.of(Integer.parseInt(arg));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    public Optional<Player> getPlayerArg(int index) {
        String name = getArg(index);
        return name != null ? Optional.ofNullable(Bukkit.getPlayer(name)) : Optional.empty();
    }

    public boolean hasPermission(String perm) {
        return sender.hasPermission(perm);
    }

    public void send(String msg) { sender.sendMessage(msg); }
    public void sendSuccess(String msg) { sender.sendMessage("§a" + msg); }
    public void sendError(String msg) { sender.sendMessage("§c" + msg); }
    public void sendInfo(String msg) { sender.sendMessage("§b" + msg); }

    public boolean ensurePlayer() {
        if (!isPlayer()) {
            sendError("Only players can use this command!");
            return false;
        }
        return true;
    }

    public boolean ensureArgs(int minArgs, String usage) {
        if (args.length < minArgs) {
            sendError("Usage: " + usage);
            return false;
        }
        return true;
    }
}