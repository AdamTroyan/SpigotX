package dev.adam.commands;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CommandContext {
    private final CommandSender sender;
    private final String[] args;

    public CommandContext(CommandSender sender, String[] args) {
        this.sender = sender;
        this.args = args;
    }

    public CommandSender getSender() {
        return sender;
    }

    public String[] getArgs() {
        return args;
    }

    public Player getPlayer() {
        return (sender instanceof Player) ? (Player) sender : null;
    }

    public void send(String msg) {
        sender.sendMessage(msg);
    }

    public boolean hasPermission(String perm) {
        return sender.hasPermission(perm);
    }
}