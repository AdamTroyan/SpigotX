package dev.adam.gui;

@FunctionalInterface
public interface ClickHandler {
    void accept(ClickContext ctx);
}
