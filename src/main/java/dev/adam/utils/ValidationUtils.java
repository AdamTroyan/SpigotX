package dev.adam.utils;

import java.util.Collection;
import java.util.UUID;
import java.util.regex.Pattern;

public class ValidationUtils {
    
        private static final Pattern UUID_PATTERN = Pattern.compile(
        "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$"
    );
        
    public static void notNull(Object obj, String message) {
        if (obj == null) throw new IllegalArgumentException(message);
    }
    
    public static void notEmpty(String str, String message) {
        if (str == null || str.isEmpty()) throw new IllegalArgumentException(message);
    }
    
    public static void notBlank(String str, String message) {
        if (str == null || str.trim().isEmpty()) throw new IllegalArgumentException(message);
    }
    
    public static void notEmpty(Collection<?> collection, String message) {
        if (collection == null || collection.isEmpty()) throw new IllegalArgumentException(message);
    }
    
    public static void notEmpty(Object[] array, String message) {
        if (array == null || array.length == 0) throw new IllegalArgumentException(message);
    }
        
    public static boolean inRange(int value, int min, int max, String message) {
        if (value < min || value > max) {
            if (message != null) throw new IllegalArgumentException(message);
            return false;
        }
        return true;
    }
    
    public static boolean inRange(long value, long min, long max, String message) {
        if (value < min || value > max) {
            if (message != null) throw new IllegalArgumentException(message);
            return false;
        }
        return true;
    }
    
    public static boolean inRange(double value, double min, double max, String message) {
        if (value < min || value > max) {
            if (message != null) throw new IllegalArgumentException(message);
            return false;
        }
        return true;
    }
    
    // === POSITIVE/NEGATIVE CHECKS ===
    
    public static boolean isPositive(int value, String message) {
        if (value <= 0) {
            if (message != null) throw new IllegalArgumentException(message);
            return false;
        }
        return true;
    }
    
    public static boolean isPositive(double value, String message) {
        if (value <= 0) {
            if (message != null) throw new IllegalArgumentException(message);
            return false;
        }
        return true;
    }
    
    public static boolean isNonNegative(int value, String message) {
        if (value < 0) {
            if (message != null) throw new IllegalArgumentException(message);
            return false;
        }
        return true;
    }
    
    public static boolean isNonNegative(double value, String message) {
        if (value < 0) {
            if (message != null) throw new IllegalArgumentException(message);
            return false;
        }
        return true;
    }
    
    // === LENGTH VALIDATION ===
    
    public static boolean hasMinLength(String str, int minLength, String message) {
        if (str == null || str.length() < minLength) {
            if (message != null) throw new IllegalArgumentException(message);
            return false;
        }
        return true;
    }
    
    public static boolean hasMaxLength(String str, int maxLength, String message) {
        if (str != null && str.length() > maxLength) {
            if (message != null) throw new IllegalArgumentException(message);
            return false;
        }
        return true;
    }
    
    public static boolean hasLengthBetween(String str, int minLength, int maxLength, String message) {
        if (str == null || str.length() < minLength || str.length() > maxLength) {
            if (message != null) throw new IllegalArgumentException(message);
            return false;
        }
        return true;
    }
        
    public static boolean isValidUUID(String uuid, String message) {
        if (uuid == null || !UUID_PATTERN.matcher(uuid).matches()) {
            if (message != null) throw new IllegalArgumentException(message);
            return false;
        }
        return true;
    }
    
    public static boolean matchesPattern(String str, Pattern pattern, String message) {
        if (str == null || !pattern.matcher(str).matches()) {
            if (message != null) throw new IllegalArgumentException(message);
            return false;
        }
        return true;
    }
    
    public static boolean matchesPattern(String str, String regex, String message) {
        if (str == null || !str.matches(regex)) {
            if (message != null) throw new IllegalArgumentException(message);
            return false;
        }
        return true;
    }
        
    public static boolean hasMinSize(Collection<?> collection, int minSize, String message) {
        if (collection == null || collection.size() < minSize) {
            if (message != null) throw new IllegalArgumentException(message);
            return false;
        }
        return true;
    }
    
    public static boolean hasMaxSize(Collection<?> collection, int maxSize, String message) {
        if (collection != null && collection.size() > maxSize) {
            if (message != null) throw new IllegalArgumentException(message);
            return false;
        }
        return true;
    }
    
    public static boolean hasSizeBetween(Collection<?> collection, int minSize, int maxSize, String message) {
        if (collection == null || collection.size() < minSize || collection.size() > maxSize) {
            if (message != null) throw new IllegalArgumentException(message);
            return false;
        }
        return true;
    }
    
    // === COMPARISON VALIDATION ===
    
    public static boolean isEqual(Object obj1, Object obj2, String message) {
        boolean equal = (obj1 == null && obj2 == null) || (obj1 != null && obj1.equals(obj2));
        if (!equal) {
            if (message != null) throw new IllegalArgumentException(message);
            return false;
        }
        return true;
    }
    
    public static boolean isNotEqual(Object obj1, Object obj2, String message) {
        boolean notEqual = !((obj1 == null && obj2 == null) || (obj1 != null && obj1.equals(obj2)));
        if (!notEqual) {
            if (message != null) throw new IllegalArgumentException(message);
            return false;
        }
        return true;
    }
    
    public static boolean isValidCoordinate(int coord, String message) {
        return inRange(coord, -30000000, 30000000, message);
    }
    
    public static boolean isValidHeight(int y, String message) {
        return inRange(y, -64, 320, message);
    }
    
    public static boolean isValidItemAmount(int amount, String message) {
        return inRange(amount, 1, 64, message);
    }
    
    public static boolean isNumeric(String str) {
        if (str == null || str.isEmpty()) return false;
        return str.matches("\\d+");
    }
    
    public static boolean isAlphabetic(String str) {
        if (str == null || str.isEmpty()) return false;
        return str.matches("[a-zA-Z]+");
    }
    
    public static boolean isAlphanumeric(String str) {
        if (str == null || str.isEmpty()) return false;
        return str.matches("[a-zA-Z0-9]+");
    }
    
    public static boolean isSafeString(String str, String message) {
        if (str == null || !str.matches("[a-zA-Z0-9_\\-\\s]+")) {
            if (message != null) throw new IllegalArgumentException(message);
            return false;
        }
        return true;
    }
    
    public static boolean isValidUrl(String url, String message) {
        if (url == null) {
            if (message != null) throw new IllegalArgumentException(message);
            return false;
        }
        
        try {
            new java.net.URL(url);
            return true;
        } catch (java.net.MalformedURLException e) {
            if (message != null) throw new IllegalArgumentException(message);
            return false;
        }
    }
    
    public static boolean isInRange(int value, int min, int max) {
        return value >= min && value <= max;
    }
    
    public static boolean isInRange(double value, double min, double max) {
        return value >= min && value <= max;
    }
    
    public static boolean isValidLength(String str, int minLength, int maxLength) {
        return str != null && str.length() >= minLength && str.length() <= maxLength;
    }
    
    public static boolean isNullOrEmpty(String str) {
        return str == null || str.isEmpty();
    }
    
    public static boolean isNullOrBlank(String str) {
        return str == null || str.trim().isEmpty();
    }
    
    public static boolean isNullOrEmpty(Collection<?> collection) {
        return collection == null || collection.isEmpty();
    }
}