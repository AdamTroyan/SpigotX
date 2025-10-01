package dev.adam.gui;

import org.bukkit.entity.Player;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public final class PlaceholderManager {
    private static final PlaceholderManager INSTANCE = new PlaceholderManager();
    private final Map<String, Function<Player, String>> placeholders = new ConcurrentHashMap<>();

    private PlaceholderManager() {}

    public static PlaceholderManager get() { return INSTANCE; }

    public void register(String key, Function<Player, String> provider) {
        placeholders.put(key, provider);
    }

    public void unregister(String key) { placeholders.remove(key); }

    public String apply(Player p, String text) {
        if (text == null) return null;
        String out = text;
        for (Map.Entry<String, Function<Player, String>> e : placeholders.entrySet()) {
            String token = "{" + e.getKey() + "}";
            if (out.contains(token)) {
                String val = e.getValue().apply(p);
                if (val == null) val = "";
                out = out.replace(token, val);
            }
        }
        return out;
    }
}
