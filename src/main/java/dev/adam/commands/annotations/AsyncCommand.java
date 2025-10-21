package dev.adam.commands.annotations;

import java.lang.annotation.*;

/**
 * Marks a command method to be executed asynchronously.
 *
 * <p>Commands annotated with this will be executed on a separate thread,
 * allowing for non-blocking operations that won't freeze the main server thread.</p>
 * 
 * @author Adam
 * @since 1.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface AsyncCommand {
}