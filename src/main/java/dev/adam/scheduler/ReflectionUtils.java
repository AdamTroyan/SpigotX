package dev.adam.scheduler;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility class for Java reflection operations in Bukkit/Spigot plugins.
 * 
 * This class provides a comprehensive set of reflection utilities optimized for Minecraft plugin development,
 * including method invocation, field access, NMS (Net Minecraft Server) operations, and packet handling.
 * All operations are cached for improved performance and include proper error handling.
 * 
 * <p>Key features:</p>
 * <ul>
 *   <li>Cached reflection operations for better performance</li>
 *   <li>NMS and CraftBukkit class access with version compatibility</li>
 *   <li>Packet sending utilities for custom network operations</li>
 *   <li>Safe method and field access with error handling</li>
 *   <li>Version detection and compatibility checking</li>
 *   <li>Debugging utilities for class inspection</li>
 * </ul>
 * 
 * @author Adam
 * @version 1.0
 * @since 1.0
 */
public class ReflectionUtils {

    /** Cache for method lookups to improve performance */
    private static final Map<String, Method> methodCache = new HashMap<>();
    
    /** Cache for field lookups to improve performance */
    private static final Map<String, Field> fieldCache = new HashMap<>();
    
    /** Cache for class lookups to improve performance */
    private static final Map<String, Class<?>> classCache = new HashMap<>();

    /** The current Bukkit version string (e.g., "v1_20_R1") */
    private static String bukkitVersion;

    static {
        // Extract version from Bukkit package name
        String packageName = Bukkit.getServer().getClass().getPackage().getName();
        bukkitVersion = packageName.substring(packageName.lastIndexOf('.') + 1);
    }

    // === METHOD INVOCATION ===

    /**
     * Invokes a method with no parameters on the specified object.
     * 
     * @param obj the object to invoke the method on
     * @param methodName the name of the method to invoke
     * @return the result of the method invocation, or null if failed
     */
    public static Object callMethod(Object obj, String methodName) {
        return callMethod(obj, methodName, new Class[0], new Object[0]);
    }

    /**
     * Invokes a method with the specified arguments on the given object.
     * Parameter types are automatically inferred from the argument types.
     * 
     * @param obj the object to invoke the method on
     * @param methodName the name of the method to invoke
     * @param args the arguments to pass to the method
     * @return the result of the method invocation, or null if failed
     */
    public static Object callMethod(Object obj, String methodName, Object... args) {
        if (args == null || args.length == 0) {
            return callMethod(obj, methodName);
        }

        Class<?>[] types = new Class[args.length];
        for (int i = 0; i < args.length; i++) {
            types[i] = args[i].getClass();
        }

        return callMethod(obj, methodName, types, args);
    }

    /**
     * Invokes a method with explicitly specified parameter types and arguments.
     * 
     * @param obj the object to invoke the method on
     * @param methodName the name of the method to invoke
     * @param paramTypes the parameter types of the method
     * @param args the arguments to pass to the method
     * @return the result of the method invocation, or null if failed
     */
    public static Object callMethod(Object obj, String methodName, Class<?>[] paramTypes, Object[] args) {
        try {
            Method method = getMethod(obj.getClass(), methodName, paramTypes);
            if (method != null) {
                method.setAccessible(true);
                return method.invoke(obj, args);
            }
        } catch (Exception e) {
            System.err.println("Failed to call method " + methodName + ": " + e.getMessage());
        }
        return null;
    }

    /**
     * Invokes a static method on the specified class.
     * 
     * @param clazz the class containing the static method
     * @param methodName the name of the static method to invoke
     * @param args the arguments to pass to the method
     * @return the result of the method invocation, or null if failed
     */
    public static Object callStaticMethod(Class<?> clazz, String methodName, Object... args) {
        try {
            Class<?>[] types = new Class[args.length];
            for (int i = 0; i < args.length; i++) {
                types[i] = args[i].getClass();
            }

            Method method = getMethod(clazz, methodName, types);
            if (method != null) {
                method.setAccessible(true);
                return method.invoke(null, args);
            }
        } catch (Exception e) {
            System.err.println("Failed to call static method " + methodName + ": " + e.getMessage());
        }
        return null;
    }

    // === FIELD ACCESS ===

    /**
     * Gets the value of a field from the specified object.
     * 
     * @param obj the object to get the field value from
     * @param fieldName the name of the field
     * @return the field value, or null if failed
     */
    public static Object getField(Object obj, String fieldName) {
        try {
            Field field = getField(obj.getClass(), fieldName);
            if (field != null) {
                field.setAccessible(true);
                return field.get(obj);
            }
        } catch (Exception e) {
            System.err.println("Failed to get field " + fieldName + ": " + e.getMessage());
        }
        return null;
    }

    /**
     * Sets the value of a field in the specified object.
     * 
     * @param obj the object to set the field value in
     * @param fieldName the name of the field
     * @param value the value to set
     * @return true if successful, false if failed
     */
    public static boolean setField(Object obj, String fieldName, Object value) {
        try {
            Field field = getField(obj.getClass(), fieldName);
            if (field != null) {
                field.setAccessible(true);
                field.set(obj, value);
                return true;
            }
        } catch (Exception e) {
            System.err.println("Failed to set field " + fieldName + ": " + e.getMessage());
        }
        return false;
    }

    /**
     * Gets the value of a static field from the specified class.
     * 
     * @param clazz the class containing the static field
     * @param fieldName the name of the static field
     * @return the field value, or null if failed
     */
    public static Object getStaticField(Class<?> clazz, String fieldName) {
        try {
            Field field = getField(clazz, fieldName);
            if (field != null) {
                field.setAccessible(true);
                return field.get(null);
            }
        } catch (Exception e) {
            System.err.println("Failed to get static field " + fieldName + ": " + e.getMessage());
        }
        return null;
    }

    /**
     * Sets the value of a static field in the specified class.
     * 
     * @param clazz the class containing the static field
     * @param fieldName the name of the static field
     * @param value the value to set
     * @return true if successful, false if failed
     */
    public static boolean setStaticField(Class<?> clazz, String fieldName, Object value) {
        try {
            Field field = getField(clazz, fieldName);
            if (field != null) {
                field.setAccessible(true);
                field.set(null, value);
                return true;
            }
        } catch (Exception e) {
            System.err.println("Failed to set static field " + fieldName + ": " + e.getMessage());
        }
        return false;
    }

    // === CLASS LOADING ===

    /**
     * Loads and returns a class by its fully qualified name.
     * Results are cached for improved performance.
     * 
     * @param className the fully qualified class name
     * @return the Class object, or null if not found
     */
    public static Class<?> getClass(String className) {
        if (classCache.containsKey(className)) {
            return classCache.get(className);
        }

        try {
            Class<?> clazz = Class.forName(className);
            classCache.put(className, clazz);
            return clazz;
        } catch (ClassNotFoundException e) {
            System.err.println("Class not found: " + className);
            classCache.put(className, null);
            return null;
        }
    }

    /**
     * Creates a new instance of the specified class with the given arguments.
     * 
     * @param clazz the class to instantiate
     * @param args the constructor arguments
     * @return the new instance, or null if failed
     */
    public static Object createInstance(Class<?> clazz, Object... args) {
        try {
            if (args.length == 0) {
                return clazz.getDeclaredConstructor().newInstance();
            }

            Class<?>[] types = new Class[args.length];
            for (int i = 0; i < args.length; i++) {
                types[i] = args[i].getClass();
            }

            Constructor<?> constructor = clazz.getConstructor(types);
            return constructor.newInstance(args);
        } catch (Exception e) {
            System.err.println("Failed to create instance of " + clazz.getName() + ": " + e.getMessage());
        }
        return null;
    }

    // === NMS AND CRAFTBUKKIT ACCESS ===

    /**
     * Gets an NMS (Net Minecraft Server) class by name.
     * Automatically handles version-specific package names.
     * 
     * @param className the NMS class name (without package)
     * @return the NMS Class object, or null if not found
     */
    public static Class<?> getNMSClass(String className) {
        return getClass("net.minecraft.server." + bukkitVersion + "." + className);
    }

    /**
     * Gets a CraftBukkit class by name.
     * Automatically handles version-specific package names.
     * 
     * @param className the CraftBukkit class name (without package)
     * @return the CraftBukkit Class object, or null if not found
     */
    public static Class<?> getCraftBukkitClass(String className) {
        return getClass("org.bukkit.craftbukkit." + bukkitVersion + "." + className);
    }

    /**
     * Gets the NMS player object from a Bukkit Player.
     * 
     * @param player the Bukkit player
     * @return the NMS player object, or null if failed
     */
    public static Object getNMSPlayer(Player player) {
        return callMethod(player, "getHandle");
    }

    /**
     * Gets the player connection object from a Bukkit Player.
     * Used for sending custom packets.
     * 
     * @param player the Bukkit player
     * @return the player connection object, or null if failed
     */
    public static Object getPlayerConnection(Player player) {
        Object nmsPlayer = getNMSPlayer(player);
        return getField(nmsPlayer, "playerConnection");
    }

    /**
     * Sends a custom packet to the specified player.
     * 
     * @param player the player to send the packet to
     * @param packet the NMS packet object to send
     * @return true if successful, false if failed
     */
    public static boolean sendPacket(Player player, Object packet) {
        try {
            Object playerConnection = getPlayerConnection(player);
            callMethod(playerConnection, "sendPacket", packet);
            return true;
        } catch (Exception e) {
            System.err.println("Failed to send packet: " + e.getMessage());
            return false;
        }
    }

    // === UTILITY METHODS ===

    /**
     * Gets the current Bukkit version string.
     * 
     * @return the Bukkit version (e.g., "v1_20_R1")
     */
    public static String getBukkitVersion() {
        return bukkitVersion;
    }

    /**
     * Checks if a class exists by its fully qualified name.
     * 
     * @param className the class name to check
     * @return true if the class exists, false otherwise
     */
    public static boolean classExists(String className) {
        return getClass(className) != null;
    }

    /**
     * Checks if a method exists in the specified class.
     * 
     * @param clazz the class to check
     * @param methodName the method name
     * @param paramTypes the parameter types
     * @return true if the method exists, false otherwise
     */
    public static boolean methodExists(Class<?> clazz, String methodName, Class<?>... paramTypes) {
        return getMethod(clazz, methodName, paramTypes) != null;
    }

    /**
     * Checks if a field exists in the specified class.
     * 
     * @param clazz the class to check
     * @param fieldName the field name
     * @return true if the field exists, false otherwise
     */
    public static boolean fieldExists(Class<?> clazz, String fieldName) {
        return getField(clazz, fieldName) != null;
    }

    /**
     * Clears all reflection caches.
     * Useful for memory management in long-running applications.
     */
    public static void clearCache() {
        methodCache.clear();
        fieldCache.clear();
        classCache.clear();
    }

    // === DEBUGGING UTILITIES ===

    /**
     * Prints all methods in the specified class to the console.
     * Useful for debugging and exploring class structures.
     * 
     * @param clazz the class to inspect
     */
    public static void printMethods(Class<?> clazz) {
        System.out.println("Methods in " + clazz.getName() + ":");
        for (Method method : clazz.getDeclaredMethods()) {
            System.out.println("  " + method.getName() + "(" + Arrays.toString(method.getParameterTypes()) + ")");
        }
    }

    /**
     * Prints all fields in the specified class to the console.
     * Useful for debugging and exploring class structures.
     * 
     * @param clazz the class to inspect
     */
    public static void printFields(Class<?> clazz) {
        System.out.println("Fields in " + clazz.getName() + ":");
        for (Field field : clazz.getDeclaredFields()) {
            System.out.println("  " + field.getType().getSimpleName() + " " + field.getName());
        }
    }

    /**
     * Prints cache statistics to the console.
     * Useful for monitoring cache usage and performance.
     */
    public static void printCacheStats() {
        System.out.println("ReflectionUtils Cache Stats:");
        System.out.println("  Methods cached: " + methodCache.size());
        System.out.println("  Fields cached: " + fieldCache.size());
        System.out.println("  Classes cached: " + classCache.size());
    }

    // === PRIVATE HELPER METHODS ===

    /**
     * Gets a method from the specified class with caching support.
     * Searches through the class hierarchy if not found in the immediate class.
     * 
     * @param clazz the class to search
     * @param methodName the method name
     * @param paramTypes the parameter types
     * @return the Method object, or null if not found
     */
    private static Method getMethod(Class<?> clazz, String methodName, Class<?>[] paramTypes) {
        String key = clazz.getName() + "." + methodName + "." + Arrays.toString(paramTypes);

        if (methodCache.containsKey(key)) {
            return methodCache.get(key);
        }

        try {
            Method method = clazz.getDeclaredMethod(methodName, paramTypes);
            methodCache.put(key, method);
            return method;
        } catch (NoSuchMethodException e) {
            if (clazz.getSuperclass() != null) {
                Method method = getMethod(clazz.getSuperclass(), methodName, paramTypes);
                methodCache.put(key, method);
                return method;
            }
        }

        methodCache.put(key, null);
        return null;
    }

    /**
     * Gets a field from the specified class with caching support.
     * Searches through the class hierarchy if not found in the immediate class.
     * 
     * @param clazz the class to search
     * @param fieldName the field name
     * @return the Field object, or null if not found
     */
    private static Field getField(Class<?> clazz, String fieldName) {
        String key = clazz.getName() + "." + fieldName;

        if (fieldCache.containsKey(key)) {
            return fieldCache.get(key);
        }

        try {
            Field field = clazz.getDeclaredField(fieldName);
            fieldCache.put(key, field);
            return field;
        } catch (NoSuchFieldException e) {
            if (clazz.getSuperclass() != null) {
                Field field = getField(clazz.getSuperclass(), fieldName);
                fieldCache.put(key, field);
                return field;
            }
        }

        fieldCache.put(key, null);
        return null;
    }
}