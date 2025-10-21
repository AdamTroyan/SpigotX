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

    public CommandBuilder description(String desc) {
        this.description = desc;
        return this;
    }

    public CommandBuilder permission(String perm) {
        this.permission = perm;
        return this;
    }

    public CommandBuilder async() {
        this.async = true;
        return this;
    }

    public CommandBuilder executor(BiConsumer<CommandSender, String[]> exec) {
        this.executor = exec;
        return this;
    }

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

    public CommandBuilder contextExecutor(Consumer<CommandContext> exec) {
        this.executor = (sender, args) -> {
            CommandContext ctx = new CommandContext(sender, args, name);
            exec.accept(ctx);
        };
        return this;
    }

    public void register() {
        if (async) {
            CommandManager.registerAsync(name, description, permission, executor);
        } else {
            CommandManager.register(name, description, permission, executor);
        }
    }

    // Static factory methods
    public static CommandBuilder create(String name) {
        return new CommandBuilder(name);
    }

    public static void quick(String name, BiConsumer<CommandSender, String[]> executor) {
        create(name).executor(executor).register();
    }

    public static void quickPlayer(String name, Consumer<Player> executor) {
        create(name).playerExecutor(executor).register();
    }

    public static void quickFull(String name, String description, String permission, 
                                BiConsumer<CommandSender, String[]> executor) {
        create(name)
            .description(description)
            .permission(permission)
            .executor(executor)
            .register();
    }
}