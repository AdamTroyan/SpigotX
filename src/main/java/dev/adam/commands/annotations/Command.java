package dev.adam.commands.annotations;

import java.lang.annotation.*;

/**
 * Marks a method as a command handler in the SpigotX command system.
 * 
 * <p>Methods annotated with this will be automatically registered as commands
 * when the containing class is registered with the CommandManager.</p>
 * 
 * @author Adam
 * @since 1.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Command {
    /**
     * The name of the command.
     * 
     * @return the command name
     */
    String name();
    
    /**
     * A description of what the command does.
     * 
     * @return the command description, empty string by default
     */
    String description() default "";
    
    /**
     * The permission required to execute this command.
     * 
     * @return the required permission, empty string for no permission by default
     */
    String permission() default "";
    
    /**
     * Usage information for the command.
     * 
     * @return the usage string, empty string by default
     */
    String usage() default "";
    
    /**
     * Whether this command should be executed asynchronously.
     * 
     * @return true if the command should run async, false by default
     */
    boolean async() default false;
}