package dev.adam.commands.annotations;

import java.lang.annotation.*;

/**
 * Marks a method as a sub-command handler in the SpigotX command system.
 *
 * <p>Sub-commands are executed when a parent command is called with specific arguments.
 * They provide a way to create nested command structures.</p>
 *
 * @author Adam
 * @since 1.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface SubCommand {
    /**
     * The parent command name this sub-command belongs to.
     *
     * @return the parent command name
     */
    String parent();
    
    /**
     * The name of the sub-command.
     * 
     * @return the sub-command name
     */
    String name();
    
    /**
     * A description of what the sub-command does.
     * 
     * @return the sub-command description, empty string by default
     */
    String description() default "";
    
    /**
     * The permission required to execute this sub-command.
     * 
     * @return the required permission, empty string for no permission by default
     */
    String permission() default "";
    
    /**
     * Usage information for the sub-command.
     * 
     * @return the usage string, empty string by default
     */
    String usage() default "";
    
    /**
     * Whether this sub-command should be executed asynchronously.
     * 
     * @return true if the sub-command should run async, false by default
     */
    boolean async() default false;
}