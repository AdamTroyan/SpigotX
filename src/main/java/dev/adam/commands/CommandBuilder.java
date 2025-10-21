package dev.adam.commands;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Simple fluent builder for commands.
 */
public class CommandBuilder {
    private String name;
    private String description = "";
    private String permission = "";
    private boolean async = false;
    private BiConsumer<CommandSender, String[]> executor;

    private CommandBuilder(String name) {
        this.name = name;
    }

    /**
     * Sets the command description.
     * 
     * @param desc the command description
     * @return this builder instance for chaining
     */
    public CommandBuilder description(String desc) {
        this.description = desc;
        return this;
    }

    /**
     * Sets the required permission for this command.
     * 
     * @param perm the permission string
     * @return this builder instance for chaining
     */
    public CommandBuilder permission(String perm) {
        this.permission = perm;
        return this;
    }

    /**
     * Marks this command to run asynchronously.
     * 
     * @return this builder instance for chaining
     */
    public CommandBuilder async() {
        this.async = true;
        return this;
    }

    /**
     * Sets the command executor with sender and arguments.
     * 
     * @param exec the executor function
     * @return this builder instance for chaining
     */
    public CommandBuilder executor(BiConsumer<CommandSender, String[]> exec) {
        this.executor = exec;
        return this;
    }

    /**
     * Sets a player-only executor with just the player parameter.
     * 
     * @param exec the player executor function
     * @return this builder instance for chaining
     */
    public CommandBuilder playerExecutor(Consumer<Player> exec) {
        this.executor = (sender, args) -> {
            if (sender instanceof Player) {
                exec.accept((Player) sender);
            } else {
                sender.sendMessage("§cOnly players can use this command!");
            }
        };
        return this;
    }

    /**
     * Sets a player-only executor with player and arguments.
     * 
     * @param exec the player executor function
     * @return this builder instance for chaining
     */
    public CommandBuilder playerExecutor(BiConsumer<Player, String[]> exec) {
        this.executor = (sender, args) -> {
            if (sender instanceof Player) {
                exec.accept((Player) sender, args);
            } else {
                sender.sendMessage("§cOnly players can use this command!");
            }
        };
        return this;
    }

    /**
     * Sets the command executor using CommandContext.
     * 
     * @param exec the context executor function
     * @return this builder instance for chaining
     */
    public CommandBuilder contextExecutor(Consumer<CommandContext> exec) {
        this.executor = (sender, args) -> {
            CommandContext ctx = new CommandContext(sender, args, name);
            exec.accept(ctx);
        };
        return this;
    }

    /**
     * Registers the command with the server.
     */
    public void register() {
        if (async) {
            CommandManager.registerAsync(name, description, permission, executor);
        } else {
            CommandManager.register(name, description, permission, executor);
        }
    }

    // Static factory methods
    /**
     * Creates a new CommandBuilder instance.
     * 
     * @param name the command name
     * @return a new CommandBuilder instance
     */
    public static CommandBuilder create(String name) {
        return new CommandBuilder(name);
    }

    /**
     * Quickly registers a simple command.
     * 
     * @param name the command name
     * @param executor the command executor
     */
    public static void quick(String name, BiConsumer<CommandSender, String[]> executor) {
        create(name).executor(executor).register();
    }

    /**
     * Quickly registers a player-only command.
     * 
     * @param name the command name
     * @param executor the player executor
     */
    public static void quickPlayer(String name, Consumer<Player> executor) {
        create(name).playerExecutor(executor).register();
    }

    /**
     * Quickly registers a fully configured command.
     * 
     * @param name the command name
     * @param description the command description
     * @param permission the required permission
     * @param executor the command executor
     */
    public static void quickFull(String name, String description, String permission, 
                           BiConsumer<CommandSender, String[]> executor) {
        create(name)
            .description(description)
            .permission(permission)
            .executor(executor)
            .register();
    }
}