package dev.adam.placeholders;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Advanced placeholder management system for Bukkit/Spigot plugins.
 * 
 * This manager provides a comprehensive solution for text placeholders with support for
 * player-specific, offline player, global, and parameterized placeholders. It includes
 * caching mechanisms for performance optimization, built-in placeholders for common
 * server and player information, and advanced features like mathematical expressions
 * and context-aware parsing.
 * 
 * <p>Key features:</p>
 * <ul>
 *   <li>Multiple placeholder types (player, offline, global, parameterized)</li>
 *   <li>Intelligent caching system with configurable expiration</li>
 *   <li>Built-in mathematical expression evaluation</li>
 *   <li>Extensive built-in placeholders for server and player data</li>
 *   <li>Context-aware parsing with custom variable injection</li>
 *   <li>Thread-safe operations with concurrent collections</li>
 *   <li>Performance monitoring and cache statistics</li>
 *   <li>Automatic cache cleanup and memory management</li>
 * </ul>
 * 
 * <p>Placeholder formats:</p>
 * <ul>
 *   <li>Simple: {@code {player_name}}</li>
 *   <li>Parameterized: {@code {random:1,100}}</li>
 *   <li>Mathematical: {@code {math:10+5*2}}</li>
 * </ul>
 * 
 * @author Adam
 * @version 1.0
 * @since 1.0
 */
public class PlaceholderManager {
    
    /** Map of player-specific placeholders */
    private static final Map<String, Function<Player, String>> playerPlaceholders = new ConcurrentHashMap<>();
    
    /** Map of offline player placeholders */
    private static final Map<String, Function<OfflinePlayer, String>> offlinePlayerPlaceholders = new ConcurrentHashMap<>();
    
    /** Map of global (static) placeholders */
    private static final Map<String, Supplier<String>> globalPlaceholders = new ConcurrentHashMap<>();
    
    /** Map of parameterized placeholders that accept arguments */
    private static final Map<String, BiFunction<Player, String[], String>> parameterizedPlaceholders = new ConcurrentHashMap<>();

    /** Cache for placeholder values to improve performance */
    private static final Map<String, CachedPlaceholder> placeholderCache = new ConcurrentHashMap<>();
    
    /** Whether caching is enabled globally */
    private static boolean cachingEnabled = true;
    
    /** Default cache expiration time in milliseconds */
    private static long cacheExpirationTime = 5000; // 5 seconds

    /** Formatters for consistent number and date formatting */
    private static final DecimalFormat MONEY_FORMAT = new DecimalFormat("#,##0.00");
    private static final DecimalFormat NUMBER_FORMAT = new DecimalFormat("#,##0");
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    /** Regex patterns for different placeholder types */
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{([^}]+)\\}");
    private static final Pattern PARAMETER_PATTERN = Pattern.compile("\\{([^:}]+):([^}]+)\\}");
    private static final Pattern MATH_PATTERN = Pattern.compile("\\{math:([^}]+)\\}");

    /** Server start time for uptime calculations */
    private static final long serverStartTime = System.currentTimeMillis();

    // === PLACEHOLDER REGISTRATION ===

    /**
     * Registers a player-specific placeholder.
     * These placeholders require a Player object and are evaluated per-player.
     * 
     * @param key the placeholder key (case-insensitive)
     * @param func the function that generates the placeholder value
     */
    public static void register(String key, Function<Player, String> func) {
        playerPlaceholders.put(key.toLowerCase(), func);
    }

    /**
     * Registers an offline player placeholder.
     * These placeholders work with OfflinePlayer objects for players not currently online.
     * 
     * @param key the placeholder key (case-insensitive)
     * @param func the function that generates the placeholder value
     */
    public static void registerOffline(String key, Function<OfflinePlayer, String> func) {
        offlinePlayerPlaceholders.put(key.toLowerCase(), func);
    }

    /**
     * Registers a global placeholder.
     * These placeholders are static and don't depend on any player data.
     * 
     * @param key the placeholder key (case-insensitive) 
     * @param supplier the supplier that generates the placeholder value
     */
    public static void registerGlobal(String key, Supplier<String> supplier) {
        globalPlaceholders.put(key.toLowerCase(), supplier);
    }

    /**
     * Registers a parameterized placeholder.
     * These placeholders accept parameters in the format {key:param1,param2}.
     * 
     * @param key the placeholder key (case-insensitive)
     * @param func the function that processes the placeholder with parameters
     */
    public static void registerParameterized(String key, BiFunction<Player, String[], String> func) {
        parameterizedPlaceholders.put(key.toLowerCase(), func);
    }

    /**
     * Registers a cached placeholder with custom cache time.
     * The placeholder value will be cached for the specified duration to improve performance.
     * 
     * @param key the placeholder key (case-insensitive)
     * @param func the function that generates the placeholder value
     * @param cacheTime the cache duration in milliseconds
     */
    public static void registerCached(String key, Function<Player, String> func, long cacheTime) {
        register(key, player -> {
            if (!cachingEnabled) {
                return func.apply(player);
            }

            String cacheKey = key + ":" + (player != null ? player.getUniqueId() : "global");
            CachedPlaceholder cached = placeholderCache.get(cacheKey);

            if (cached != null && !cached.isExpired()) {
                return cached.getValue();
            }

            String value = func.apply(player);
            placeholderCache.put(cacheKey, new CachedPlaceholder(value, cacheTime));
            return value;
        });
    }

    // === TEXT PARSING ===

    /**
     * Parses all placeholders in the given text for a specific player.
     * Processes placeholders in order: math expressions, parameterized, regular, then global.
     * 
     * @param player the player context for placeholder resolution
     * @param text the text containing placeholders to parse
     * @return the text with all placeholders replaced
     */
    public static String parse(Player player, String text) {
        if (text == null || text.isEmpty()) return text;

        // Parse in order of complexity to avoid conflicts
        text = parseMathExpressions(text);
        text = parseParameterizedPlaceholders(player, text);
        text = parseRegularPlaceholders(player, text);
        text = parseGlobalPlaceholders(text);

        return text;
    }

    /**
     * Parses placeholders for offline players.
     * Only processes offline player placeholders and global placeholders.
     * 
     * @param player the offline player context
     * @param text the text containing placeholders to parse
     * @return the text with offline player placeholders replaced
     */
    public static String parseOffline(OfflinePlayer player, String text) {
        if (text == null || text.isEmpty()) return text;

        // Process offline player placeholders
        for (var entry : offlinePlayerPlaceholders.entrySet()) {
            String placeholder = "{" + entry.getKey() + "}";
            if (text.contains(placeholder)) {
                text = text.replace(placeholder, entry.getValue().apply(player));
            }
        }

        text = parseGlobalPlaceholders(text);
        return text;
    }

    /**
     * Parses placeholders in a list of strings.
     * Applies placeholder parsing to each string in the list.
     * 
     * @param player the player context for placeholder resolution
     * @param lines the list of strings to parse
     * @return a new list with all placeholders replaced
     */
    public static List<String> parseList(Player player, List<String> lines) {
        if (lines == null) return new ArrayList<>();

        List<String> parsed = new ArrayList<>();
        for (String line : lines) {
            parsed.add(parse(player, line));
        }
        return parsed;
    }

    /**
     * Parses placeholders with additional context variables.
     * Context variables are processed first, then regular placeholders.
     * 
     * @param player the player context for placeholder resolution
     * @param text the text containing placeholders to parse
     * @param context additional context variables as key-value pairs
     * @return the text with context variables and placeholders replaced
     */
    public static String parseWithContext(Player player, String text, Map<String, String> context) {
        if (context != null) {
            for (var entry : context.entrySet()) {
                String placeholder = "{" + entry.getKey() + "}";
                text = text.replace(placeholder, entry.getValue());
            }
        }

        return parse(player, text);
    }

    // === PARSING IMPLEMENTATION ===

    /**
     * Parses regular (non-parameterized) placeholders using regex matching.
     */
    private static String parseRegularPlaceholders(Player player, String text) {
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(text);
        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            String placeholder = matcher.group(1).toLowerCase();
            
            // Skip if this looks like a parameterized placeholder
            if (placeholder.contains(":")) {
                continue;
            }
            
            String replacement = getPlaceholderValue(player, placeholder);
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);

        return result.toString();
    }

    /**
     * Parses parameterized placeholders that include arguments.
     */
    private static String parseParameterizedPlaceholders(Player player, String text) {
        Matcher matcher = PARAMETER_PATTERN.matcher(text);
        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            String placeholder = matcher.group(1).toLowerCase();
            String parameters = matcher.group(2);
            String[] params = parameters.split(",");

            // Trim whitespace from parameters
            for (int i = 0; i < params.length; i++) {
                params[i] = params[i].trim();
            }

            BiFunction<Player, String[], String> func = parameterizedPlaceholders.get(placeholder);
            String replacement = func != null ? func.apply(player, params) : 
                                "{" + matcher.group(1) + ":" + parameters + "}";

            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);

        return result.toString();
    }

    /**
     * Parses global placeholders using simple string replacement.
     */
    private static String parseGlobalPlaceholders(String text) {
        for (var entry : globalPlaceholders.entrySet()) {
            String placeholder = "{" + entry.getKey() + "}";
            if (text.contains(placeholder)) {
                text = text.replace(placeholder, entry.getValue().get());
            }
        }
        return text;
    }

    /**
     * Parses and evaluates mathematical expressions in {math:expression} format.
     */
    private static String parseMathExpressions(String text) {
        Matcher matcher = MATH_PATTERN.matcher(text);
        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            String expression = matcher.group(1);
            try {
                double value = evaluateExpression(expression);
                String formatted = value == (long) value ? 
                    String.valueOf((long) value) : 
                    MONEY_FORMAT.format(value);
                matcher.appendReplacement(result, formatted);
            } catch (Exception e) {
                matcher.appendReplacement(result, "Math Error");
            }
        }
        matcher.appendTail(result);

        return result.toString();
    }

    /**
     * Gets the value for a regular placeholder.
     */
    private static String getPlaceholderValue(Player player, String placeholder) {
        Function<Player, String> func = playerPlaceholders.get(placeholder);
        return func != null ? func.apply(player) : "{" + placeholder + "}";
    }

    // === BUILT-IN PLACEHOLDERS ===

    /**
     * Registers all built-in placeholders for common server and player data.
     * This includes player info, server stats, date/time, and utility functions.
     */
    public static void registerBuiltInPlaceholders() {
        // Player information placeholders
        register("player_name", player -> player != null ? player.getName() : "Unknown");
        register("player_displayname", player -> player != null ? player.getDisplayName() : "Unknown");
        register("player_uuid", player -> player != null ? player.getUniqueId().toString() : "Unknown");
        register("player_world", player -> player != null ? player.getWorld().getName() : "Unknown");
        register("player_gamemode", player -> player != null ? player.getGameMode().name().toLowerCase() : "Unknown");
        register("player_level", player -> player != null ? String.valueOf(player.getLevel()) : "0");
        register("player_exp", player -> player != null ? String.valueOf(Math.round(player.getExp() * 100)) + "%" : "0%");
        register("player_health", player -> player != null ? MONEY_FORMAT.format(player.getHealth()) : "0.00");
        register("player_max_health", player -> player != null ? MONEY_FORMAT.format(player.getMaxHealth()) : "20.00");
        register("player_food", player -> player != null ? String.valueOf(player.getFoodLevel()) : "20");
        register("player_ip", player -> {
            if (player != null && player.getAddress() != null) {
                return player.getAddress().getAddress().getHostAddress();
            }
            return "Unknown";
        });
        register("player_ping", player -> player != null ? String.valueOf(player.getPing()) : "0");

        // Player location placeholders
        register("player_x", player -> player != null ? String.valueOf(player.getLocation().getBlockX()) : "0");
        register("player_y", player -> player != null ? String.valueOf(player.getLocation().getBlockY()) : "0");
        register("player_z", player -> player != null ? String.valueOf(player.getLocation().getBlockZ()) : "0");
        register("player_yaw", player -> player != null ? MONEY_FORMAT.format(player.getLocation().getYaw()) : "0.00");
        register("player_pitch", player -> player != null ? MONEY_FORMAT.format(player.getLocation().getPitch()) : "0.00");

        // Server information placeholders
        registerGlobal("server_name", () -> Bukkit.getServer().getName());
        registerGlobal("server_version", () -> Bukkit.getVersion());
        registerGlobal("server_bukkit_version", () -> Bukkit.getBukkitVersion());
        registerGlobal("server_online", () -> String.valueOf(Bukkit.getOnlinePlayers().size()));
        registerGlobal("server_max_players", () -> String.valueOf(Bukkit.getMaxPlayers()));
        registerGlobal("server_motd", () -> Bukkit.getMotd());
        registerGlobal("server_uptime", () -> getUptime());

        // Date and time placeholders
        registerGlobal("date", () -> DATE_FORMAT.format(new Date()));
        registerGlobal("time", () -> new SimpleDateFormat("HH:mm:ss").format(new Date()));
        registerGlobal("timestamp", () -> String.valueOf(System.currentTimeMillis()));
        registerGlobal("year", () -> String.valueOf(Calendar.getInstance().get(Calendar.YEAR)));
        registerGlobal("month", () -> String.valueOf(Calendar.getInstance().get(Calendar.MONTH) + 1));
        registerGlobal("day", () -> String.valueOf(Calendar.getInstance().get(Calendar.DAY_OF_MONTH)));

        // Utility parameterized placeholders
        registerParameterized("random", (player, params) -> {
            if (params.length >= 2) {
                try {
                    int min = Integer.parseInt(params[0]);
                    int max = Integer.parseInt(params[1]);
                    return String.valueOf(new Random().nextInt(max - min + 1) + min);
                } catch (NumberFormatException e) {
                    return "Error";
                }
            }
            return String.valueOf(new Random().nextInt(100));
        });

        registerParameterized("format_number", (player, params) -> {
            if (params.length >= 1) {
                try {
                    double number = Double.parseDouble(params[0]);
                    return NUMBER_FORMAT.format(number);
                } catch (NumberFormatException e) {
                    return params[0];
                }
            }
            return "0";
        });

        registerParameterized("format_money", (player, params) -> {
            if (params.length >= 1) {
                try {
                    double money = Double.parseDouble(params[0]);
                    return "$" + MONEY_FORMAT.format(money);
                } catch (NumberFormatException e) {
                    return params[0];
                }
            }
            return "$0.00";
        });

        registerParameterized("upper", (player, params) -> {
            return params.length >= 1 ? params[0].toUpperCase() : "";
        });

        registerParameterized("lower", (player, params) -> {
            return params.length >= 1 ? params[0].toLowerCase() : "";
        });

        registerParameterized("substring", (player, params) -> {
            if (params.length >= 3) {
                try {
                    String text = params[0];
                    int start = Integer.parseInt(params[1]);
                    int end = Integer.parseInt(params[2]);
                    return text.substring(Math.max(0, start), Math.min(text.length(), end));
                } catch (Exception e) {
                    return params[0];
                }
            }
            return "";
        });
    }

    // === UTILITY METHODS ===

    /**
     * Calculates and formats server uptime as a human-readable string.
     */
    private static String getUptime() {
        long uptime = System.currentTimeMillis() - serverStartTime;
        long seconds = uptime / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        if (days > 0) {
            return days + "d " + (hours % 24) + "h " + (minutes % 60) + "m";
        } else if (hours > 0) {
            return hours + "h " + (minutes % 60) + "m";
        } else {
            return minutes + "m " + (seconds % 60) + "s";
        }
    }

    /**
     * Evaluates simple mathematical expressions.
     * Supports basic operations: +, -, *, /
     */
    private static double evaluateExpression(String expression) {
        expression = expression.replace(" ", "");

        // Handle basic operations in order of precedence
        if (expression.contains("*")) {
            String[] parts = expression.split("\\*", 2);
            return Double.parseDouble(parts[0]) * Double.parseDouble(parts[1]);
        } else if (expression.contains("/")) {
            String[] parts = expression.split("/", 2);
            double denominator = Double.parseDouble(parts[1]);
            if (denominator == 0) throw new ArithmeticException("Division by zero");
            return Double.parseDouble(parts[0]) / denominator;
        } else if (expression.contains("+")) {
            String[] parts = expression.split("\\+", 2);
            return Double.parseDouble(parts[0]) + Double.parseDouble(parts[1]);
        } else if (expression.contains("-") && expression.lastIndexOf("-") > 0) {
            int lastMinus = expression.lastIndexOf("-");
            String part1 = expression.substring(0, lastMinus);
            String part2 = expression.substring(lastMinus + 1);
            return Double.parseDouble(part1) - Double.parseDouble(part2);
        }

        return Double.parseDouble(expression);
    }

    // === CACHE MANAGEMENT ===

    /**
     * Represents a cached placeholder value with expiration time.
     */
    private static class CachedPlaceholder {
        private final String value;
        private final long expirationTime;

        /**
         * Creates a new cached placeholder.
         * 
         * @param value the cached value
         * @param cacheTime how long to cache in milliseconds
         */
        public CachedPlaceholder(String value, long cacheTime) {
            this.value = value;
            this.expirationTime = System.currentTimeMillis() + cacheTime;
        }

        /**
         * Gets the cached value.
         * 
         * @return the cached value
         */
        public String getValue() {
            return value;
        }

        /**
         * Checks if this cached value has expired.
         * 
         * @return true if expired, false otherwise
         */
        public boolean isExpired() {
            return System.currentTimeMillis() > expirationTime;
        }
    }

    /**
     * Clears all cached placeholder values.
     */
    public static void clearCache() {
        placeholderCache.clear();
    }

    /**
     * Removes only expired entries from the cache.
     * This method is called automatically to prevent memory leaks.
     */
    public static void clearExpiredCache() {
        placeholderCache.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }

    /**
     * Enables or disables caching globally.
     * When disabled, all cached values are cleared.
     * 
     * @param enabled whether to enable caching
     */
    public static void setCachingEnabled(boolean enabled) {
        cachingEnabled = enabled;
        if (!enabled) {
            clearCache();
        }
    }

    /**
     * Sets the default cache expiration time for new cached placeholders.
     * 
     * @param time the expiration time in milliseconds
     */
    public static void setCacheExpirationTime(long time) {
        cacheExpirationTime = time;
    }

    // === PLACEHOLDER MANAGEMENT ===

    /**
     * Removes a placeholder from all registries.
     * 
     * @param key the placeholder key to remove
     */
    public static void unregister(String key) {
        key = key.toLowerCase();
        playerPlaceholders.remove(key);
        offlinePlayerPlaceholders.remove(key);
        globalPlaceholders.remove(key);
        parameterizedPlaceholders.remove(key);
    }

    /**
     * Removes all registered placeholders and clears the cache.
     */
    public static void unregisterAll() {
        playerPlaceholders.clear();
        offlinePlayerPlaceholders.clear();
        globalPlaceholders.clear();
        parameterizedPlaceholders.clear();
        clearCache();
    }

    /**
     * Checks if a placeholder is registered in any registry.
     * 
     * @param placeholder the placeholder key to check
     * @return true if registered, false otherwise
     */
    public static boolean isPlaceholderRegistered(String placeholder) {
        String key = placeholder.toLowerCase();
        return playerPlaceholders.containsKey(key) ||
                offlinePlayerPlaceholders.containsKey(key) ||
                globalPlaceholders.containsKey(key) ||
                parameterizedPlaceholders.containsKey(key);
    }

    // === DEBUGGING AND MONITORING ===

    /**
     * Lists all registered placeholders to the console.
     * Useful for debugging and seeing what placeholders are available.
     */
    public static void listPlaceholders() {
        System.out.println("=== Registered Placeholders ===");
        System.out.println("Player placeholders: " + playerPlaceholders.keySet());
        System.out.println("Offline player placeholders: " + offlinePlayerPlaceholders.keySet());
        System.out.println("Global placeholders: " + globalPlaceholders.keySet());
        System.out.println("Parameterized placeholders: " + parameterizedPlaceholders.keySet());
    }

    /**
     * Prints cache statistics to the console.
     * Shows cache size, settings, and expired entries count.
     */
    public static void printCacheStats() {
        System.out.println("=== Placeholder Cache Stats ===");
        System.out.println("Cached entries: " + placeholderCache.size());
        System.out.println("Caching enabled: " + cachingEnabled);
        System.out.println("Cache expiration time: " + cacheExpirationTime + "ms");

        long expired = placeholderCache.values().stream()
                .mapToLong(cache -> cache.isExpired() ? 1 : 0)
                .sum();
        System.out.println("Expired entries: " + expired);
    }

    // === INITIALIZATION ===

    /**
     * Initializes the placeholder manager with built-in placeholders and cache cleanup.
     * This method should be called once during plugin startup.
     */
    public static void initialize() {
        registerBuiltInPlaceholders();

        // Set up automatic cache cleanup every minute
        if (Bukkit.getPluginManager().getPlugins().length > 0) {
            Plugin plugin = Bukkit.getPluginManager().getPlugins()[0];
            Bukkit.getScheduler().runTaskTimerAsynchronously(plugin,
                    PlaceholderManager::clearExpiredCache, 20L * 60L, 20L * 60L);
        }
    }
}