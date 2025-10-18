package dev.adam.utils;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class ReflectionUtils {
    
    private static final Map<String, Method> methodCache = new HashMap<>();
    private static final Map<String, Field> fieldCache = new HashMap<>();
    private static final Map<String, Class<?>> classCache = new HashMap<>();
    
    private static String bukkitVersion;
    private static String nmsVersion;
    
    static {
        String packageName = Bukkit.getServer().getClass().getPackage().getName();
        bukkitVersion = packageName.substring(packageName.lastIndexOf('.') + 1);
        nmsVersion = bukkitVersion.replace("v", "").replace("R", "").replace("_", ".");
    }
    
    
    public static Object callMethod(Object obj, String methodName) {
        return callMethod(obj, methodName, new Class[0], new Object[0]);
    }
    
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
    
    public static Object callStaticMethod(Class<?> clazz, String methodName, Object... args) {
        return callMethod(null, clazz, methodName, args);
    }
    
    private static Object callMethod(Object obj, Class<?> clazz, String methodName, Object... args) {
        try {
            Class<?>[] types = new Class[args.length];
            for (int i = 0; i < args.length; i++) {
                types[i] = args[i].getClass();
            }
            
            Method method = getMethod(clazz, methodName, types);
            if (method != null) {
                method.setAccessible(true);
                return method.invoke(obj, args);
            }
        } catch (Exception e) {
            System.err.println("Failed to call static method " + methodName + ": " + e.getMessage());
        }
        return null;
    }
    
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
    
    public static Object createInstance(Class<?> clazz, Object... args) {
        try {
            if (args.length == 0) {
                return clazz.newInstance();
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
    
    public static Class<?> getNMSClass(String className) {
        return getClass("net.minecraft.server." + bukkitVersion + "." + className);
    }
    
    public static Class<?> getCraftBukkitClass(String className) {
        return getClass("org.bukkit.craftbukkit." + bukkitVersion + "." + className);
    }
    
    public static Object getNMSPlayer(Player player) {
        return callMethod(player, "getHandle");
    }
    
    public static Object getPlayerConnection(Player player) {
        Object nmsPlayer = getNMSPlayer(player);
        return getField(nmsPlayer, "playerConnection");
    }
    
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
    
    public static String getBukkitVersion() {
        return bukkitVersion;
    }
    
    public static String getNMSVersion() {
        return nmsVersion;
    }
    
    public static boolean isVersionAtLeast(String version) {
        return nmsVersion.compareTo(version) >= 0;
    }
    
    public static void printMethods(Class<?> clazz) {
        System.out.println("Methods in " + clazz.getName() + ":");
        for (Method method : clazz.getDeclaredMethods()) {
            System.out.println("  " + method.getName() + "(" + Arrays.toString(method.getParameterTypes()) + ")");
        }
    }

    public static void printFields(Class<?> clazz) {
        System.out.println("Fields in " + clazz.getName() + ":");
        for (Field field : clazz.getDeclaredFields()) {
            System.out.println("  " + field.getType().getSimpleName() + " " + field.getName());
        }
    }

    public static boolean classExists(String className) {
        return getClass(className) != null;
    }

    public static boolean methodExists(Class<?> clazz, String methodName, Class<?>... paramTypes) {
        return getMethod(clazz, methodName, paramTypes) != null;
    }
    
    public static boolean fieldExists(Class<?> clazz, String fieldName) {
        return getField(clazz, fieldName) != null;
    }
    
    public static void clearCache() {
        methodCache.clear();
        fieldCache.clear();
        classCache.clear();
    }
    
    public static void printCacheStats() {
        System.out.println("ReflectionUtils Cache Stats:");
        System.out.println("  Methods cached: " + methodCache.size());
        System.out.println("  Fields cached: " + fieldCache.size());
        System.out.println("  Classes cached: " + classCache.size());
    }
}