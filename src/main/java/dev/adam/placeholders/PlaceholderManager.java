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

public class PlaceholderManager {
    private static final Map<String, Function<Player, String>> playerPlaceholders = new ConcurrentHashMap<>();
    private static final Map<String, Function<OfflinePlayer, String>> offlinePlayerPlaceholders = new ConcurrentHashMap<>();
    private static final Map<String, Supplier<String>> globalPlaceholders = new ConcurrentHashMap<>();
    private static final Map<String, BiFunction<Player, String[], String>> parameterizedPlaceholders = new ConcurrentHashMap<>();
    
    private static final Map<String, CachedPlaceholder> placeholderCache = new ConcurrentHashMap<>();
    private static boolean cachingEnabled = true;
    private static long cacheExpirationTime = 5000;
    
    private static final DecimalFormat MONEY_FORMAT = new DecimalFormat("#,##0.00");
    private static final DecimalFormat NUMBER_FORMAT = new DecimalFormat("#,##0");
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{([^}]+)\\}");
    private static final Pattern PARAMETER_PATTERN = Pattern.compile("\\{([^:}]+):([^}]+)\\}");
    private static final Pattern MATH_PATTERN = Pattern.compile("\\{math:([^}]+)\\}");

    public static void register(String key, Function<Player, String> func) {
        playerPlaceholders.put(key.toLowerCase(), func);
    }
    
    public static void registerOffline(String key, Function<OfflinePlayer, String> func) {
        offlinePlayerPlaceholders.put(key.toLowerCase(), func);
    }
    
    public static void registerGlobal(String key, Supplier<String> supplier) {
        globalPlaceholders.put(key.toLowerCase(), supplier);
    }
    
    public static void registerParameterized(String key, BiFunction<Player, String[], String> func) {
        parameterizedPlaceholders.put(key.toLowerCase(), func);
    }
    
    public static void registerCached(String key, Function<Player, String> func, long cacheTime) {
        register(key, player -> {
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

    public static String parse(Player player, String text) {
        if (text == null || text.isEmpty()) return text;
        
        text = parseMathExpressions(text);
        
        text = parseParameterizedPlaceholders(player, text);
        
        text = parseRegularPlaceholders(player, text);
        
        text = parseGlobalPlaceholders(text);
        
        return text;
    }

    public static String parseOffline(OfflinePlayer player, String text) {
        if (text == null || text.isEmpty()) return text;
        
        for (var entry : offlinePlayerPlaceholders.entrySet()) {
            text = text.replace("{" + entry.getKey() + "}", entry.getValue().apply(player));
        }
        
        text = parseGlobalPlaceholders(text);
        
        return text;
    }
    
    public static List<String> parseList(Player player, List<String> lines) {
        if (lines == null) return new ArrayList<>();
        
        List<String> parsed = new ArrayList<>();
        for (String line : lines) {
            parsed.add(parse(player, line));
        }
        return parsed;
    }
    
    public static String parseWithContext(Player player, String text, Map<String, String> context) {
        if (context != null) {
            for (var entry : context.entrySet()) {
                text = text.replace("{" + entry.getKey() + "}", entry.getValue());
            }
        }
        
        return parse(player, text);
    }
    
    private static String parseRegularPlaceholders(Player player, String text) {
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(text);
        StringBuffer result = new StringBuffer();
        
        while (matcher.find()) {
            String placeholder = matcher.group(1).toLowerCase();
            String replacement = getPlaceholderValue(player, placeholder);
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);
        
        return result.toString();
    }
    
    private static String parseParameterizedPlaceholders(Player player, String text) {
        Matcher matcher = PARAMETER_PATTERN.matcher(text);
        StringBuffer result = new StringBuffer();
        
        while (matcher.find()) {
            String placeholder = matcher.group(1).toLowerCase();
            String parameters = matcher.group(2);
            String[] params = parameters.split(",");
            
            BiFunction<Player, String[], String> func = parameterizedPlaceholders.get(placeholder);
            String replacement = func != null ? func.apply(player, params) : "{" + matcher.group(1) + ":" + parameters + "}";
            
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);
        
        return result.toString();
    }
    
    private static String parseGlobalPlaceholders(String text) {
        for (var entry : globalPlaceholders.entrySet()) {
            text = text.replace("{" + entry.getKey() + "}", entry.getValue().get());
        }
        return text;
    }
    
    private static String parseMathExpressions(String text) {
        Matcher matcher = MATH_PATTERN.matcher(text);
        StringBuffer result = new StringBuffer();
        
        while (matcher.find()) {
            String expression = matcher.group(1);
            try {
                double value = evaluateExpression(expression);
                matcher.appendReplacement(result, String.valueOf(value));
            } catch (Exception e) {
                matcher.appendReplacement(result, "Error");
            }
        }
        matcher.appendTail(result);
        
        return result.toString();
    }
    
    private static String getPlaceholderValue(Player player, String placeholder) {
        Function<Player, String> func = playerPlaceholders.get(placeholder);
        return func != null ? func.apply(player) : "{" + placeholder + "}";
    }

    public static void registerBuiltInPlaceholders() {
        register("player_name", player -> player != null ? player.getName() : "Unknown");
        register("player_displayname", player -> player != null ? player.getDisplayName() : "Unknown");
        register("player_uuid", player -> player != null ? player.getUniqueId().toString() : "Unknown");
        register("player_world", player -> player != null ? player.getWorld().getName() : "Unknown");
        register("player_gamemode", player -> player != null ? player.getGameMode().name() : "Unknown");
        register("player_level", player -> player != null ? String.valueOf(player.getLevel()) : "0");
        register("player_exp", player -> player != null ? String.valueOf(Math.round(player.getExp() * 100)) + "%" : "0%");
        register("player_health", player -> player != null ? MONEY_FORMAT.format(player.getHealth()) : "0");
        register("player_max_health", player -> player != null ? MONEY_FORMAT.format(player.getMaxHealth()) : "0");
        register("player_food", player -> player != null ? String.valueOf(player.getFoodLevel()) : "0");
        register("player_ip", player -> player != null ? player.getAddress().getAddress().getHostAddress() : "Unknown");
        register("player_ping", player -> player != null ? String.valueOf(player.getPing()) : "0");
        
        register("player_x", player -> player != null ? String.valueOf(player.getLocation().getBlockX()) : "0");
        register("player_y", player -> player != null ? String.valueOf(player.getLocation().getBlockY()) : "0");
        register("player_z", player -> player != null ? String.valueOf(player.getLocation().getBlockZ()) : "0");
        register("player_yaw", player -> player != null ? MONEY_FORMAT.format(player.getLocation().getYaw()) : "0");
        register("player_pitch", player -> player != null ? MONEY_FORMAT.format(player.getLocation().getPitch()) : "0");
        
        registerGlobal("server_name", () -> Bukkit.getServer().getName());
        registerGlobal("server_version", () -> Bukkit.getVersion());
        registerGlobal("server_bukkit_version", () -> Bukkit.getBukkitVersion());
        registerGlobal("server_online", () -> String.valueOf(Bukkit.getOnlinePlayers().size()));
        registerGlobal("server_max_players", () -> String.valueOf(Bukkit.getMaxPlayers()));
        registerGlobal("server_motd", () -> Bukkit.getMotd());
        registerGlobal("server_uptime", () -> getUptime());
        
        registerGlobal("date", () -> DATE_FORMAT.format(new Date()));
        registerGlobal("time", () -> new SimpleDateFormat("HH:mm:ss").format(new Date()));
        registerGlobal("timestamp", () -> String.valueOf(System.currentTimeMillis()));
        registerGlobal("year", () -> String.valueOf(Calendar.getInstance().get(Calendar.YEAR)));
        registerGlobal("month", () -> String.valueOf(Calendar.getInstance().get(Calendar.MONTH) + 1));
        registerGlobal("day", () -> String.valueOf(Calendar.getInstance().get(Calendar.DAY_OF_MONTH)));
        
        registerParameterized("random", (player, params) -> {
            if (params.length >= 2) {
                try {
                    int min = Integer.parseInt(params[0].trim());
                    int max = Integer.parseInt(params[1].trim());
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
                    double number = Double.parseDouble(params[0].trim());
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
                    double money = Double.parseDouble(params[0].trim());
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
                    int start = Integer.parseInt(params[1].trim());
                    int end = Integer.parseInt(params[2].trim());
                    return text.substring(Math.max(0, start), Math.min(text.length(), end));
                } catch (Exception e) {
                    return params[0];
                }
            }
            return "";
        });
    }
    
    private static String getUptime() {
        long uptime = System.currentTimeMillis() - getServerStartTime();
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
    
    private static long serverStartTime = System.currentTimeMillis();
    
    private static long getServerStartTime() {
        return serverStartTime;
    }
    
    private static double evaluateExpression(String expression) {
        expression = expression.replace(" ", "");
        
        if (expression.contains("+")) {
            String[] parts = expression.split("\\+");
            return Double.parseDouble(parts[0]) + Double.parseDouble(parts[1]);
        } else if (expression.contains("-")) {
            String[] parts = expression.split("-");
            return Double.parseDouble(parts[0]) - Double.parseDouble(parts[1]);
        } else if (expression.contains("*")) {
            String[] parts = expression.split("\\*");
            return Double.parseDouble(parts[0]) * Double.parseDouble(parts[1]);
        } else if (expression.contains("/")) {
            String[] parts = expression.split("/");
            return Double.parseDouble(parts[0]) / Double.parseDouble(parts[1]);
        }
        
        return Double.parseDouble(expression);
    }
        
    private static class CachedPlaceholder {
        private final String value;
        private final long expirationTime;
        
        public CachedPlaceholder(String value, long cacheTime) {
            this.value = value;
            this.expirationTime = System.currentTimeMillis() + cacheTime;
        }
        
        public String getValue() {
            return value;
        }
        
        public boolean isExpired() {
            return System.currentTimeMillis() > expirationTime;
        }
    }
    
    public static void clearCache() {
        placeholderCache.clear();
    }
    
    public static void clearExpiredCache() {
        placeholderCache.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }
    
    public static void setCachingEnabled(boolean enabled) {
        cachingEnabled = enabled;
        if (!enabled) {
            clearCache();
        }
    }
    
    public static void setCacheExpirationTime(long time) {
        cacheExpirationTime = time;
    }
        
    public static void listPlaceholders() {
        System.out.println("=== Registered Placeholders ===");
        System.out.println("Player placeholders: " + playerPlaceholders.keySet());
        System.out.println("Offline player placeholders: " + offlinePlayerPlaceholders.keySet());
        System.out.println("Global placeholders: " + globalPlaceholders.keySet());
        System.out.println("Parameterized placeholders: " + parameterizedPlaceholders.keySet());
    }
    
    public static boolean isPlaceholderRegistered(String placeholder) {
        String key = placeholder.toLowerCase();
        return playerPlaceholders.containsKey(key) || 
               offlinePlayerPlaceholders.containsKey(key) || 
               globalPlaceholders.containsKey(key) || 
               parameterizedPlaceholders.containsKey(key);
    }
    
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
        
    public static void unregister(String key) {
        key = key.toLowerCase();
        playerPlaceholders.remove(key);
        offlinePlayerPlaceholders.remove(key);
        globalPlaceholders.remove(key);
        parameterizedPlaceholders.remove(key);
    }
    
    public static void unregisterAll() {
        playerPlaceholders.clear();
        offlinePlayerPlaceholders.clear();
        globalPlaceholders.clear();
        parameterizedPlaceholders.clear();
        clearCache();
    }

    public static void initialize() {
        registerBuiltInPlaceholders();
        
        if (Bukkit.getPluginManager().getPlugins().length > 0) {
            Plugin plugin = Bukkit.getPluginManager().getPlugins()[0];
            Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, 
                PlaceholderManager::clearExpiredCache, 20L * 60L, 20L * 60L);
        }
    }
}