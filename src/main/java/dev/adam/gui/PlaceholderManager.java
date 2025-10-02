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
        if (key == null || provider == null) return;
        placeholders.put(key, provider);
    }

    public void unregister(String key) {
        if (key == null) return;
        placeholders.remove(key);
    }

    public void clearAll() { placeholders.clear(); }

    public String apply(Player p, String text) {
        if (text == null || p == null) return "";
        String out = text;

        for (Map.Entry<String, Function<Player, String>> e : placeholders.entrySet()) {
            String token = "{" + e.getKey() + "}";
            if (out.contains(token)) {
                try {
                    String val = e.getValue().apply(p);
                    if (val == null) val = "";
                    out = out.replace(token, val);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }

        return out;
    }
}