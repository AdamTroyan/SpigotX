package dev.adam.commands.annotations;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface SubCommand {
    String parent();
    String name();
    String description() default "";
    String permission() default "";
    String usage() default "";
    boolean async() default false;
}