package dev.adam.commands;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

public class TabCompleterBuilder {
    private BiFunction<CommandSender, String[], List<String>> completer;
    private final Map<Integer, Function<CommandSender, List<String>>> argCompletions = new HashMap<>();
    private Function<CommandSender, List<String>> defaultCompletion;

    public TabCompleterBuilder completer(BiFunction<CommandSender, String[], List<String>> func) {
        this.completer = func;
        return this;
    }

    public TabCompleterBuilder arg(int index, Function<CommandSender, List<String>> completion) {
        argCompletions.put(index, completion);
        return this;
    }

    public TabCompleterBuilder arg(int index, List<String> values) {
        return arg(index, sender -> values);
    }

    public TabCompleterBuilder arg(int index, String... values) {
        return arg(index, Arrays.asList(values));
    }

    public TabCompleterBuilder players(int index) {
        return arg(index, sender -> Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .collect(Collectors.toList()));
    }

    public TabCompleterBuilder playersWithPermission(int index, String permission) {
        return arg(index, sender -> Bukkit.getOnlinePlayers().stream()
                .filter(p -> p.hasPermission(permission))
                .map(Player::getName)
                .collect(Collectors.toList()));
    }

    public TabCompleterBuilder worlds(int index) {
        return arg(index, sender -> Bukkit.getWorlds().stream()
                .map(w -> w.getName())
                .collect(Collectors.toList()));
    }

    public TabCompleterBuilder materials(int index) {
        return arg(index, sender -> Arrays.stream(Material.values())
                .map(m -> m.name().toLowerCase())
                .collect(Collectors.toList()));
    }

    public TabCompleterBuilder plugins(int index) {
        return arg(index, sender -> Arrays.stream(Bukkit.getPluginManager().getPlugins())
                .map(Plugin::getName)
                .collect(Collectors.toList()));
    }

    public TabCompleterBuilder numbers(int index, int min, int max) {
        List<String> numbers = new ArrayList<>();
        for (int i = min; i <= max; i++) {
            numbers.add(String.valueOf(i));
        }
        return arg(index, numbers);
    }

    public TabCompleterBuilder booleans(int index) {
        return arg(index, "true", "false");
    }

    public TabCompleterBuilder gamemodes(int index) {
        return arg(index, "survival", "creative", "adventure", "spectator");
    }

    public TabCompleterBuilder difficulties(int index) {
        return arg(index, "peaceful", "easy", "normal", "hard");
    }

    public TabCompleterBuilder conditional(int index, Function<CommandSender, Boolean> condition, Function<CommandSender, List<String>> completion) {
        return arg(index, sender -> condition.apply(sender) ? completion.apply(sender) : Collections.emptyList());
    }

    public TabCompleterBuilder ifPermission(int index, String permission, Function<CommandSender, List<String>> completion) {
        return conditional(index, sender -> sender.hasPermission(permission), completion);
    }

    public TabCompleterBuilder ifPlayer(int index, Function<Player, List<String>> completion) {
        return arg(index, sender -> {
            if (sender instanceof Player) {
                return completion.apply((Player) sender);
            }
            return Collections.emptyList();
        });
    }

    public TabCompleterBuilder defaultTo(Function<CommandSender, List<String>> completion) {
        this.defaultCompletion = completion;
        return this;
    }

    public TabCompleterBuilder defaultTo(List<String> values) {
        return defaultTo(sender -> values);
    }

    public TabCompleterBuilder defaultTo(String... values) {
        return defaultTo(Arrays.asList(values));
    }

    public TabCompleterBuilder filtered(BiFunction<CommandSender, String[], Boolean> filter) {
        BiFunction<CommandSender, String[], List<String>> originalCompleter = this.completer;
        this.completer = (sender, args) -> {
            if (filter.apply(sender, args)) {
                return originalCompleter != null ? originalCompleter.apply(sender, args) : complete(sender, args);
            }
            return Collections.emptyList();
        };
        return this;
    }

    public TabCompleterBuilder cached(long cacheTimeMs) {
        Map<String, CachedCompletion> cache = new HashMap<>();
        BiFunction<CommandSender, String[], List<String>> originalCompleter = this.completer;

        this.completer = (sender, args) -> {
            String key = sender.getName() + ":" + Arrays.toString(args);
            CachedCompletion cached = cache.get(key);

            if (cached != null && !cached.isExpired()) {
                return cached.getCompletions();
            }

            List<String> result = originalCompleter != null ? originalCompleter.apply(sender, args) : complete(sender, args);
            cache.put(key, new CachedCompletion(result, cacheTimeMs));
            return result;
        };

        return this;
    }

    public List<String> complete(CommandSender sender, String[] args) {
        if (completer != null) {
            return filterCompletions(completer.apply(sender, args), args);
        }

        int argIndex = args.length - 1;
        if (argIndex >= 0 && argCompletions.containsKey(argIndex)) {
            return filterCompletions(argCompletions.get(argIndex).apply(sender), args);
        }

        if (defaultCompletion != null) {
            return filterCompletions(defaultCompletion.apply(sender), args);
        }

        return Collections.emptyList();
    }

    private List<String> filterCompletions(List<String> completions, String[] args) {
        if (completions == null || completions.isEmpty() || args.length == 0) {
            return completions != null ? completions : Collections.emptyList();
        }

        String lastArg = args[args.length - 1].toLowerCase();
        return completions.stream()
                .filter(completion -> completion.toLowerCase().startsWith(lastArg))
                .sorted()
                .collect(Collectors.toList());
    }

    private static class CachedCompletion {
        private final List<String> completions;
        private final long expirationTime;

        public CachedCompletion(List<String> completions, long cacheTimeMs) {
            this.completions = completions;
            this.expirationTime = System.currentTimeMillis() + cacheTimeMs;
        }

        public List<String> getCompletions() {
            return completions;
        }

        public boolean isExpired() {
            return System.currentTimeMillis() > expirationTime;
        }
    }

    public static TabCompleterBuilder create() {
        return new TabCompleterBuilder();
    }
}