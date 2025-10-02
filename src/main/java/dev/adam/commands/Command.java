package dev.adam.commands;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Command {
    String name();
    String aliases() default "";
    String permission() default "";
    String permissionMessage() default "";
    String description() default "";
    String usage() default "";
    boolean tabComplete() default true;
}