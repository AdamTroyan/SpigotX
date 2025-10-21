package dev.adam.commands.annotations;

import java.lang.annotation.*;

/**
 * Marks a method as a tab completion handler for a specific command.
 *
 * <p>Methods annotated with this will provide tab completion suggestions
 * for the specified command when players press the Tab key.</p>
 * 
 * @author Adam
 * @since 1.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface TabComplete {
    /**
    * The command name this tab completer is for.
    * 
    * @return the command name
    */
    String command();
}