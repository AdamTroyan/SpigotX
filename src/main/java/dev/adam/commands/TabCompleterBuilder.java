package dev.adam.commands;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Simple fluent builder for tab completers with comprehensive functionality.
 */
public class TabCompleterBuilder {
    private final Map<Integer, Function<CommandSender, List<String>>> argCompletions = new HashMap<>();
    private final Map<Integer, BiFunction<CommandSender, String[], List<String>>> conditionalCompletions = new HashMap<>();
    private Function<CommandSender, List<String>> defaultCompletion;
    private Predicate<CommandSender> globalFilter;

    // === BASIC ARG METHODS ===

    /**
     * Sets tab completion for a specific argument index using a dynamic function.
     *
     * @param index      the argument index (0-based)
     * @param completion function that generates completion suggestions
     * @return this builder instance for chaining
     */
    public TabCompleterBuilder arg(int index, Function<CommandSender, List<String>> completion) {
        if (index >= 0 && completion != null) {
            argCompletions.put(index, completion);
        }
        return this;
    }

    /**
     * Sets tab completion for a specific argument index using a static list.
     *
     * @param index  the argument index (0-based)
     * @param values the static list of completion suggestions
     * @return this builder instance for chaining
     */
    public TabCompleterBuilder arg(int index, List<String> values) {
        return arg(index, sender -> values != null ? new ArrayList<>(values) : Collections.emptyList());
    }

    /**
     * Sets tab completion for a specific argument index using static values.
     *
     * @param index  the argument index (0-based)
     * @param values the static array of completion suggestions
     * @return this builder instance for chaining
     */
    public TabCompleterBuilder arg(int index, String... values) {
        return arg(index, values != null ? Arrays.asList(values) : Collections.emptyList());
    }

    // === PREDEFINED COMPLETIONS ===

    /**
     * Sets tab completion to show online player names for the specified argument.
     *
     * @param index the argument index (0-based)
     * @return this builder instance for chaining
     */
    public TabCompleterBuilder players(int index) {
        return arg(index, sender -> Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .sorted()
                .collect(Collectors.toList()));
    }

    /**
     * Sets tab completion to show players with a specific permission.
     *
     * @param index      the argument index (0-based)
     * @param permission the required permission
     * @return this builder instance for chaining
     */
    public TabCompleterBuilder playersWithPermission(int index, String permission) {
        return arg(index, sender -> Bukkit.getOnlinePlayers().stream()
                .filter(p -> p.hasPermission(permission))
                .map(Player::getName)
                .sorted()
                .collect(Collectors.toList()));
    }

    /**
     * Sets tab completion to show world names for the specified argument.
     *
     * @param index the argument index (0-based)
     * @return this builder instance for chaining
     */
    public TabCompleterBuilder worlds(int index) {
        return arg(index, sender -> Bukkit.getWorlds().stream()
                .map(World::getName)
                .sorted()
                .collect(Collectors.toList()));
    }

    /**
     * Sets tab completion to show material names for the specified argument.
     *
     * @param index the argument index (0-based)
     * @return this builder instance for chaining
     */
    public TabCompleterBuilder materials(int index) {
        return arg(index, sender -> Arrays.stream(Material.values())
                .filter(m -> !m.isLegacy())
                .map(m -> m.name().toLowerCase())
                .sorted()
                .collect(Collectors.toList()));
    }

    /**
     * Sets tab completion to show plugin names for the specified argument.
     *
     * @param index the argument index (0-based)
     * @return this builder instance for chaining
     */
    public TabCompleterBuilder plugins(int index) {
        return arg(index, sender -> Arrays.stream(Bukkit.getPluginManager().getPlugins())
                .map(Plugin::getName)
                .sorted()
                .collect(Collectors.toList()));
    }

    /**
     * Sets tab completion to show numbers in a range for the specified argument.
     *
     * @param index the argument index (0-based)
     * @param min   the minimum number (inclusive)
     * @param max   the maximum number (inclusive)
     * @return this builder instance for chaining
     */
    public TabCompleterBuilder numbers(int index, int min, int max) {
        List<String> numbers = new ArrayList<>();
        for (int i = Math.min(min, max); i <= Math.max(min, max); i++) {
            numbers.add(String.valueOf(i));
        }
        return arg(index, numbers);
    }

    /**
     * Sets tab completion to show boolean values for the specified argument.
     *
     * @param index the argument index (0-based)
     * @return this builder instance for chaining
     */
    public TabCompleterBuilder booleans(int index) {
        return arg(index, "true", "false");
    }

    /**
     * Sets tab completion to show game mode names for the specified argument.
     *
     * @param index the argument index (0-based)
     * @return this builder instance for chaining
     */
    public TabCompleterBuilder gamemodes(int index) {
        return arg(index, Arrays.stream(GameMode.values())
                .map(gm -> gm.name().toLowerCase())
                .collect(Collectors.toList()));
    }

    /**
     * Sets tab completion to show difficulty names for the specified argument.
     *
     * @param index the argument index (0-based)
     * @return this builder instance for chaining
     */
    public TabCompleterBuilder difficulties(int index) {
        return arg(index, "peaceful", "easy", "normal", "hard");
    }

    // === CONDITIONAL COMPLETIONS ===

    /**
     * Sets conditional completions based on current arguments.
     */
    public TabCompleterBuilder conditional(int index, BiFunction<CommandSender, String[], Boolean> condition,
                                           Function<CommandSender, List<String>> completion) {
        if (index >= 0 && condition != null && completion != null) {
            conditionalCompletions.put(index, (sender, args) ->
                    condition.apply(sender, args) ? completion.apply(sender) : Collections.emptyList());
        }
        return this;
    }

    /**
     * Sets completions that only appear if sender has permission.
     */
    public TabCompleterBuilder ifPermission(int index, String permission, Function<CommandSender, List<String>> completion) {
        return arg(index, sender -> sender.hasPermission(permission) ? completion.apply(sender) : Collections.emptyList());
    }

    /**
     * Sets completions that only appear if sender has permission (static values).
     */
    public TabCompleterBuilder ifPermission(int index, String permission, String... values) {
        return ifPermission(index, permission, sender -> Arrays.asList(values));
    }

    /**
     * Sets completions that only appear if sender is a player.
     */
    public TabCompleterBuilder ifPlayer(int index, Function<Player, List<String>> completion) {
        return arg(index, sender -> {
            if (sender instanceof Player) {
                return completion.apply((Player) sender);
            }
            return Collections.emptyList();
        });
    }

    // === DEFAULT COMPLETIONS ===

    /**
     * Sets default completions using a function.
     */
    public TabCompleterBuilder defaultTo(Function<CommandSender, List<String>> completion) {
        this.defaultCompletion = completion;
        return this;
    }

    /**
     * Sets static default completions.
     */
    public TabCompleterBuilder defaultTo(List<String> values) {
        return defaultTo(sender -> values != null ? new ArrayList<>(values) : Collections.emptyList());
    }

    /**
     * Sets static default completions.
     */
    public TabCompleterBuilder defaultTo(String... values) {
        return defaultTo(values != null ? Arrays.asList(values) : Collections.emptyList());
    }

    // === FILTERING ===

    /**
     * Adds a global filter for all completions.
     */
    public TabCompleterBuilder requirePermission(String permission) {
        this.globalFilter = sender -> sender.hasPermission(permission);
        return this;
    }

    /**
     * Adds a custom global filter.
     */
    public TabCompleterBuilder requireCondition(Predicate<CommandSender> filter) {
        this.globalFilter = filter;
        return this;
    }

    // === BUILDING ===

    /**
     * Builds the tab completer function.
     */
    public BiFunction<CommandSender, String[], List<String>> build() {
        return (sender, args) -> {
            if (globalFilter != null && !globalFilter.test(sender)) {
                return Collections.emptyList();
            }
            return complete(sender, args);
        };
    }

    /**
     * Internal completion logic.
     */
    private List<String> complete(CommandSender sender, String[] args) {
        try {
            int argIndex = args.length - 1;

            // Check conditional completions first
            if (argIndex >= 0 && conditionalCompletions.containsKey(argIndex)) {
                List<String> conditionalResult = conditionalCompletions.get(argIndex).apply(sender, args);
                if (!conditionalResult.isEmpty()) {
                    return filterCompletions(conditionalResult, args);
                }
            }

            // Check regular argument completions
            if (argIndex >= 0 && argCompletions.containsKey(argIndex)) {
                List<String> completions = argCompletions.get(argIndex).apply(sender);
                return filterCompletions(completions, args);
            }

            // Fall back to default
            if (defaultCompletion != null) {
                return filterCompletions(defaultCompletion.apply(sender), args);
            }

            return Collections.emptyList();
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    /**
     * Filters completions based on partial input.
     */
    private List<String> filterCompletions(List<String> completions, String[] args) {
        if (completions == null || completions.isEmpty() || args.length == 0) {
            return completions != null ? completions : Collections.emptyList();
        }

        String lastArg = args[args.length - 1].toLowerCase();
        return completions.stream()
                .filter(Objects::nonNull)
                .filter(completion -> completion.toLowerCase().startsWith(lastArg))
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    /**
     * Creates a new TabCompleterBuilder.
     */
    public static TabCompleterBuilder create() {
        return new TabCompleterBuilder();
    }
}