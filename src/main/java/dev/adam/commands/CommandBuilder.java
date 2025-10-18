package dev.adam.commands;

import org.bukkit.command.CommandSender;

import java.util.function.BiConsumer;

public class CommandBuilder {
    private String name;
    private String description = "";
    private String permission = "";
    private BiConsumer<CommandSender, String[]> executor;

    public CommandBuilder name(String name) {
        this.name = name;

        return this;
    }

    public CommandBuilder description(String desc) {
        this.description = desc;

        return this;
    }

    public CommandBuilder permission(String perm) {
        this.permission = perm;

        return this;
    }

    public CommandBuilder executor(BiConsumer<CommandSender, String[]> exec) {
        this.executor = exec;

        return this;
    }

    public void register() {
        CommandManager.register(name, description, permission, executor);
    }
}