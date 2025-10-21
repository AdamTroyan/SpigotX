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
 * 
 * <p>This class provides a powerful and flexible way to create tab completion handlers
 * for Bukkit commands using a fluent builder pattern. It supports various types of
 * completions including static values, dynamic functions, conditional completions,
 * and built-in completions for common Minecraft elements.</p>
 * 
 * <p>Key features include:</p>
 * <ul>
 *   <li>Argument-specific completions with index-based targeting</li>
 *   <li>Built-in completions for players, worlds, materials, etc.</li>
 *   <li>Conditional completions based on permissions or custom conditions</li>
 *   <li>Global filtering and permission requirements</li>
 *   <li>Automatic completion filtering based on partial input</li>
 * </ul>
 * 
 * <p>Usage example:</p>
 * <pre>{@code
 * TabCompleterBuilder.create()
 *     .players(0)                           // First argument: player names
 *     .arg(1, "give", "take", "set")       // Second argument: actions
 *     .materials(2)                        // Third argument: materials
 *     .requirePermission("admin.items")    // Require permission
 *     .build();
 * }</pre>
 * 
 * @author Adam
 * @version 1.0
 * @since 1.0
 */
public class TabCompleterBuilder {
    /** Map of argument-specific completion functions indexed by argument position */
    private final Map<Integer, Function<CommandSender, List<String>>> argCompletions = new HashMap<>();
    
    /** Map of conditional completion functions that depend on current command context */
    private final Map<Integer, BiFunction<CommandSender, String[], List<String>>> conditionalCompletions = new HashMap<>();
    
    /** Default completion function used when no specific completion is found */
    private Function<CommandSender, List<String>> defaultCompletion;
    
    /** Global filter applied to all completions for permission or condition checking */
    private Predicate<CommandSender> globalFilter;

    // === BASIC ARG METHODS ===

    /**
     * Sets tab completion for a specific argument index using a dynamic function.
     *
     * <p>The completion function receives the command sender and should return a list
     * of possible completions. This allows for dynamic completions that can change
     * based on the sender's context, permissions, or other factors.</p>
     *
     * @param index      the argument index (0-based, where 0 is the first argument after the command)
     * @param completion function that generates completion suggestions based on the sender
     * @return this builder instance for method chaining
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
     * <p>This method is useful when you have a fixed set of completions that don't
     * change based on context. The list is copied to prevent external modifications.</p>
     *
     * @param index  the argument index (0-based)
     * @param values the static list of completion suggestions
     * @return this builder instance for method chaining
     */
    public TabCompleterBuilder arg(int index, List<String> values) {
        return arg(index, sender -> values != null ? new ArrayList<>(values) : Collections.emptyList());
    }

    /**
     * Sets tab completion for a specific argument index using static values.
     *
     * <p>Convenience method for setting completions using varargs. Internally
     * converts the array to a list for processing.</p>
     *
     * @param index  the argument index (0-based)
     * @param values the static array of completion suggestions
     * @return this builder instance for method chaining
     */
    public TabCompleterBuilder arg(int index, String... values) {
        return arg(index, values != null ? Arrays.asList(values) : Collections.emptyList());
    }

    // === PREDEFINED COMPLETIONS ===

    /**
     * Sets tab completion to show online player names for the specified argument.
     *
     * <p>This completion dynamically updates based on currently online players.
     * The player names are automatically sorted alphabetically for consistency.</p>
     *
     * @param index the argument index (0-based)
     * @return this builder instance for method chaining
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
     * <p>Only shows players who have the specified permission, useful for
     * administrative commands where you only want to target players with
     * certain privileges.</p>
     *
     * @param index      the argument index (0-based)
     * @param permission the required permission that players must have to appear in completions
     * @return this builder instance for method chaining
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
     * <p>Shows all currently loaded worlds on the server. Useful for commands
     * that need to reference specific worlds for teleportation, world management, etc.</p>
     *
     * @param index the argument index (0-based)
     * @return this builder instance for method chaining
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
     * <p>Shows all non-legacy Material enum values in lowercase format.
     * Legacy materials are filtered out to ensure compatibility with current
     * Minecraft versions.</p>
     *
     * @param index the argument index (0-based)
     * @return this builder instance for method chaining
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
     * <p>Shows all currently loaded plugins on the server. Useful for
     * plugin management commands or debugging tools.</p>
     *
     * @param index the argument index (0-based)
     * @return this builder instance for method chaining
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
     * <p>Generates a list of numbers from min to max (inclusive). The range
     * is automatically corrected if min is greater than max. Useful for
     * commands that accept numeric values within specific bounds.</p>
     *
     * @param index the argument index (0-based)
     * @param min   the minimum number (inclusive)
     * @param max   the maximum number (inclusive)
     * @return this builder instance for method chaining
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
     * <p>Shows "true" and "false" as completion options. Useful for commands
     * that accept boolean flags or settings.</p>
     *
     * @param index the argument index (0-based)
     * @return this builder instance for method chaining
     */
    public TabCompleterBuilder booleans(int index) {
        return arg(index, "true", "false");
    }

    /**
     * Sets tab completion to show game mode names for the specified argument.
     *
     * <p>Shows all available GameMode enum values in lowercase format.
     * Includes survival, creative, adventure, and spectator modes.</p>
     *
     * @param index the argument index (0-based)
     * @return this builder instance for method chaining
     */
    public TabCompleterBuilder gamemodes(int index) {
        return arg(index, Arrays.stream(GameMode.values())
                .map(gm -> gm.name().toLowerCase())
                .collect(Collectors.toList()));
    }

    /**
     * Sets tab completion to show difficulty names for the specified argument.
     *
     * <p>Shows the four standard Minecraft difficulty levels: peaceful, easy,
     * normal, and hard. Useful for world management commands.</p>
     *
     * @param index the argument index (0-based)
     * @return this builder instance for method chaining
     */
    public TabCompleterBuilder difficulties(int index) {
        return arg(index, "peaceful", "easy", "normal", "hard");
    }

    // === CONDITIONAL COMPLETIONS ===

    /**
     * Sets conditional tab completion based on a custom condition.
     *
     * <p>The condition function receives the command sender and current arguments,
     * allowing for complex logic to determine whether completions should be shown.
     * If the condition returns true, the completion function is executed.</p>
     *
     * @param index      the argument index (0-based)
     * @param condition  the condition function to evaluate (receives sender and args)
     * @param completion the completion function to execute if condition is true
     * @return this builder instance for method chaining
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
     * Sets tab completion that only shows if sender has permission.
     *
     * <p>The completions will only be shown to command senders who have the
     * specified permission. This is useful for hiding administrative options
     * from regular users.</p>
     *
     * @param index      the argument index (0-based)
     * @param permission the required permission
     * @param completion the completion function to execute if permission is granted
     * @return this builder instance for method chaining
     */
    public TabCompleterBuilder ifPermission(int index, String permission, Function<CommandSender, List<String>> completion) {
        return arg(index, sender -> sender.hasPermission(permission) ? completion.apply(sender) : Collections.emptyList());
    }

    /**
     * Sets tab completion that only shows if sender has permission (static values).
     *
     * <p>Convenience method for showing static completions only to users with
     * the specified permission. Useful for simple permission-gated options.</p>
     *
     * @param index      the argument index (0-based)
     * @param permission the required permission
     * @param values     the static completion values to show if permission is granted
     * @return this builder instance for method chaining
     */
    public TabCompleterBuilder ifPermission(int index, String permission, String... values) {
        return ifPermission(index, permission, sender -> Arrays.asList(values));
    }

    /**
     * Sets tab completion that only works for players.
     *
     * <p>The completion function will only be called if the command sender is
     * a Player instance. Console and other non-player senders will receive
     * no completions for this argument.</p>
     *
     * @param index      the argument index (0-based)
     * @param completion the player-specific completion function
     * @return this builder instance for method chaining
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
     * Sets default completion when no specific completion is found.
     *
     * <p>This completion function will be used for any argument position that
     * doesn't have a specific completion defined. Useful for providing fallback
     * completions or general suggestions.</p>
     *
     * @param completion the default completion function
     * @return this builder instance for method chaining
     */
    public TabCompleterBuilder defaultTo(Function<CommandSender, List<String>> completion) {
        this.defaultCompletion = completion;
        return this;
    }

    /**
     * Sets default completion to static values.
     *
     * <p>Convenience method for setting static default completions. These values
     * will be shown for any argument position that doesn't have specific completions.</p>
     *
     * @param values the default completion values
     * @return this builder instance for method chaining
     */
    public TabCompleterBuilder defaultTo(List<String> values) {
        return defaultTo(sender -> values != null ? new ArrayList<>(values) : Collections.emptyList());
    }

    /**
     * Sets default completion to static values.
     *
     * <p>Varargs convenience method for setting static default completions.</p>
     *
     * @param values the default completion values
     * @return this builder instance for method chaining
     */
    public TabCompleterBuilder defaultTo(String... values) {
        return defaultTo(values != null ? Arrays.asList(values) : Collections.emptyList());
    }

    // === FILTERING ===

    /**
     * Requires permission for any tab completion to work.
     *
     * <p>Sets a global filter that requires the specified permission for any
     * completions to be shown. If the sender doesn't have this permission,
     * all completions will be hidden.</p>
     *
     * @param permission the required permission for all completions
     * @return this builder instance for method chaining
     */
    public TabCompleterBuilder requirePermission(String permission) {
        this.globalFilter = sender -> sender.hasPermission(permission);
        return this;
    }

    /**
     * Requires a custom condition for tab completion to work.
     *
     * <p>Sets a global filter using a custom predicate. The predicate is tested
     * against the command sender, and completions are only shown if it returns true.</p>
     *
     * @param filter the condition predicate that must be satisfied
     * @return this builder instance for method chaining
     */
    public TabCompleterBuilder requireCondition(Predicate<CommandSender> filter) {
        this.globalFilter = filter;
        return this;
    }

    // === BUILDING ===

    /**
     * Builds the tab completer function.
     *
     * <p>Creates and returns a BiFunction that can be used as a tab completer
     * for Bukkit commands. The function applies global filters and delegates
     * to the internal completion logic.</p>
     *
     * @return the built tab completion function that can be registered with Bukkit
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
     * Internal completion logic that handles argument resolution and filtering.
     *
     * <p>This method contains the core logic for determining which completions
     * to show based on the current argument index and available completion
     * functions. It tries conditional completions first, then regular completions,
     * and finally falls back to default completions.</p>
     *
     * @param sender the command sender requesting completions
     * @param args   the current command arguments
     * @return a list of filtered completion suggestions
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
     * Filters completions based on partial input from the user.
     *
     * <p>This method implements the standard tab completion behavior where
     * only completions that start with the user's partial input are shown.
     * The filtering is case-insensitive and results are sorted and deduplicated.</p>
     *
     * @param completions the raw list of completions to filter
     * @param args        the current command arguments (used to get partial input)
     * @return a filtered, sorted, and deduplicated list of completions
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
     * Creates a new TabCompleterBuilder instance.
     *
     * <p>Factory method for creating a new builder. This is the recommended
     * way to start building tab completers using the fluent API.</p>
     *
     * @return a new TabCompleterBuilder instance ready for configuration
     */
    public static TabCompleterBuilder create() {
        return new TabCompleterBuilder();
    }
}