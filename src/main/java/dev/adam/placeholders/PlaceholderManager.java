package dev.adam.placeholders;

import org.bukkit.entity.Player;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class PlaceholderManager {
    private static final Map<String, Function<Player, String>> placeholders = new HashMap<>();

    public static void register(String key, Function<Player, String> func) {
        placeholders.put(key, func);
    }

    public static String parse(Player player, String text) {
        for (var entry : placeholders.entrySet()) {
            text = text.replace("{" + entry.getKey() + "}", entry.getValue().apply(player));
        }
        return text;
    }
}