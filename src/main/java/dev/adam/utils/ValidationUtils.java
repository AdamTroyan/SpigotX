package dev.adam.utils;

import java.util.Collection;
import java.util.regex.Pattern;

/**
 * Utility class for common validation operations in Minecraft plugins.
 * 
 * This class provides a comprehensive set of validation methods for common use cases
 * in Bukkit/Spigot plugin development, including null checks, range validation,
 * string validation, and Minecraft-specific validations.
 * 
 * @author Adam
 * @version 1.0
 * @since 1.0
 */
public class ValidationUtils {

    /** Pattern for validating UUID strings */
    private static final Pattern UUID_PATTERN = Pattern.compile(
            "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$"
    );

    // === NULL AND EMPTY CHECKS ===

    /**
     * Validates that an object is not null.
     * 
     * @param obj the object to validate
     * @param message the exception message if validation fails
     * @throws IllegalArgumentException if object is null
     */
    public static void notNull(Object obj, String message) {
        if (obj == null) throw new IllegalArgumentException(message);
    }

    /**
     * Validates that a string is not null or empty.
     * 
     * @param str the string to validate
     * @param message the exception message if validation fails
     * @throws IllegalArgumentException if string is null or empty
     */
    public static void notEmpty(String str, String message) {
        if (str == null || str.isEmpty()) throw new IllegalArgumentException(message);
    }

    /**
     * Validates that a string is not null, empty, or whitespace-only.
     * 
     * @param str the string to validate
     * @param message the exception message if validation fails
     * @throws IllegalArgumentException if string is null, empty, or blank
     */
    public static void notBlank(String str, String message) {
        if (str == null || str.trim().isEmpty()) throw new IllegalArgumentException(message);
    }

    /**
     * Validates that a collection is not null or empty.
     * 
     * @param collection the collection to validate
     * @param message the exception message if validation fails
     * @throws IllegalArgumentException if collection is null or empty
     */
    public static void notEmpty(Collection<?> collection, String message) {
        if (collection == null || collection.isEmpty()) throw new IllegalArgumentException(message);
    }

    /**
     * Validates that an array is not null or empty.
     * 
     * @param array the array to validate
     * @param message the exception message if validation fails
     * @throws IllegalArgumentException if array is null or empty
     */
    public static void notEmpty(Object[] array, String message) {
        if (array == null || array.length == 0) throw new IllegalArgumentException(message);
    }

    // === RANGE VALIDATION ===

    /**
     * Validates that an integer value is within the specified range (inclusive).
     * 
     * @param value the value to validate
     * @param min the minimum allowed value (inclusive)
     * @param max the maximum allowed value (inclusive)
     * @param message the exception message if validation fails (null to return false instead of throwing)
     * @return true if valid, false if invalid and message is null
     * @throws IllegalArgumentException if value is out of range and message is not null
     */
    public static boolean inRange(int value, int min, int max, String message) {
        if (value < min || value > max) {
            if (message != null) throw new IllegalArgumentException(message);
            return false;
        }
        return true;
    }

    /**
     * Validates that a double value is within the specified range (inclusive).
     * 
     * @param value the value to validate
     * @param min the minimum allowed value (inclusive)
     * @param max the maximum allowed value (inclusive)
     * @param message the exception message if validation fails (null to return false instead of throwing)
     * @return true if valid, false if invalid and message is null
     * @throws IllegalArgumentException if value is out of range and message is not null
     */
    public static boolean inRange(double value, double min, double max, String message) {
        if (value < min || value > max) {
            if (message != null) throw new IllegalArgumentException(message);
            return false;
        }
        return true;
    }

    /**
     * Validates that an integer value is positive (greater than 0).
     * 
     * @param value the value to validate
     * @param message the exception message if validation fails (null to return false instead of throwing)
     * @return true if valid, false if invalid and message is null
     * @throws IllegalArgumentException if value is not positive and message is not null
     */
    public static boolean isPositive(int value, String message) {
        if (value <= 0) {
            if (message != null) throw new IllegalArgumentException(message);
            return false;
        }
        return true;
    }

    /**
     * Validates that a double value is positive (greater than 0).
     * 
     * @param value the value to validate
     * @param message the exception message if validation fails (null to return false instead of throwing)
     * @return true if valid, false if invalid and message is null
     * @throws IllegalArgumentException if value is not positive and message is not null
     */
    public static boolean isPositive(double value, String message) {
        if (value <= 0) {
            if (message != null) throw new IllegalArgumentException(message);
            return false;
        }
        return true;
    }

    /**
     * Validates that an integer value is non-negative (greater than or equal to 0).
     * 
     * @param value the value to validate
     * @param message the exception message if validation fails (null to return false instead of throwing)
     * @return true if valid, false if invalid and message is null
     * @throws IllegalArgumentException if value is negative and message is not null
     */
    public static boolean isNonNegative(int value, String message) {
        if (value < 0) {
            if (message != null) throw new IllegalArgumentException(message);
            return false;
        }
        return true;
    }

    /**
     * Validates that a double value is non-negative (greater than or equal to 0).
     * 
     * @param value the value to validate
     * @param message the exception message if validation fails (null to return false instead of throwing)
     * @return true if valid, false if invalid and message is null
     * @throws IllegalArgumentException if value is negative and message is not null
     */
    public static boolean isNonNegative(double value, String message) {
        if (value < 0) {
            if (message != null) throw new IllegalArgumentException(message);
            return false;
        }
        return true;
    }

    // === STRING LENGTH VALIDATION ===

    /**
     * Validates that a string meets the minimum length requirement.
     * 
     * @param str the string to validate
     * @param minLength the minimum required length
     * @param message the exception message if validation fails (null to return false instead of throwing)
     * @return true if valid, false if invalid and message is null
     * @throws IllegalArgumentException if string is too short and message is not null
     */
    public static boolean hasMinLength(String str, int minLength, String message) {
        if (str == null || str.length() < minLength) {
            if (message != null) throw new IllegalArgumentException(message);
            return false;
        }
        return true;
    }

    /**
     * Validates that a string does not exceed the maximum length.
     * 
     * @param str the string to validate
     * @param maxLength the maximum allowed length
     * @param message the exception message if validation fails (null to return false instead of throwing)
     * @return true if valid, false if invalid and message is null
     * @throws IllegalArgumentException if string is too long and message is not null
     */
    public static boolean hasMaxLength(String str, int maxLength, String message) {
        if (str != null && str.length() > maxLength) {
            if (message != null) throw new IllegalArgumentException(message);
            return false;
        }
        return true;
    }

    /**
     * Validates that a string length is within the specified range.
     * 
     * @param str the string to validate
     * @param minLength the minimum required length
     * @param maxLength the maximum allowed length
     * @param message the exception message if validation fails (null to return false instead of throwing)
     * @return true if valid, false if invalid and message is null
     * @throws IllegalArgumentException if string length is out of range and message is not null
     */
    public static boolean hasLengthBetween(String str, int minLength, int maxLength, String message) {
        if (str == null || str.length() < minLength || str.length() > maxLength) {
            if (message != null) throw new IllegalArgumentException(message);
            return false;
        }
        return true;
    }

    // === PATTERN VALIDATION ===

    /**
     * Validates that a string is a valid UUID format.
     * 
     * @param uuid the UUID string to validate
     * @param message the exception message if validation fails (null to return false instead of throwing)
     * @return true if valid UUID format, false if invalid and message is null
     * @throws IllegalArgumentException if UUID format is invalid and message is not null
     */
    public static boolean isValidUUID(String uuid, String message) {
        if (uuid == null || !UUID_PATTERN.matcher(uuid).matches()) {
            if (message != null) throw new IllegalArgumentException(message);
            return false;
        }
        return true;
    }

    /**
     * Validates that a string matches the given regex pattern.
     * 
     * @param str the string to validate
     * @param pattern the compiled pattern to match against
     * @param message the exception message if validation fails (null to return false instead of throwing)
     * @return true if matches pattern, false if doesn't match and message is null
     * @throws IllegalArgumentException if string doesn't match pattern and message is not null
     */
    public static boolean matchesPattern(String str, Pattern pattern, String message) {
        if (str == null || !pattern.matcher(str).matches()) {
            if (message != null) throw new IllegalArgumentException(message);
            return false;
        }
        return true;
    }

    /**
     * Validates that a string matches the given regex pattern.
     * 
     * @param str the string to validate
     * @param regex the regex pattern to match against
     * @param message the exception message if validation fails (null to return false instead of throwing)
     * @return true if matches pattern, false if doesn't match and message is null
     * @throws IllegalArgumentException if string doesn't match pattern and message is not null
     */
    public static boolean matchesPattern(String str, String regex, String message) {
        if (str == null || !str.matches(regex)) {
            if (message != null) throw new IllegalArgumentException(message);
            return false;
        }
        return true;
    }

    // === COLLECTION SIZE VALIDATION ===

    /**
     * Validates that a collection meets the minimum size requirement.
     * 
     * @param collection the collection to validate
     * @param minSize the minimum required size
     * @param message the exception message if validation fails (null to return false instead of throwing)
     * @return true if valid, false if invalid and message is null
     * @throws IllegalArgumentException if collection is too small and message is not null
     */
    public static boolean hasMinSize(Collection<?> collection, int minSize, String message) {
        if (collection == null || collection.size() < minSize) {
            if (message != null) throw new IllegalArgumentException(message);
            return false;
        }
        return true;
    }

    /**
     * Validates that a collection does not exceed the maximum size.
     * 
     * @param collection the collection to validate
     * @param maxSize the maximum allowed size
     * @param message the exception message if validation fails (null to return false instead of throwing)
     * @return true if valid, false if invalid and message is null
     * @throws IllegalArgumentException if collection is too large and message is not null
     */
    public static boolean hasMaxSize(Collection<?> collection, int maxSize, String message) {
        if (collection != null && collection.size() > maxSize) {
            if (message != null) throw new IllegalArgumentException(message);
            return false;
        }
        return true;
    }

    /**
     * Validates that a collection size is within the specified range.
     * 
     * @param collection the collection to validate
     * @param minSize the minimum required size
     * @param maxSize the maximum allowed size
     * @param message the exception message if validation fails (null to return false instead of throwing)
     * @return true if valid, false if invalid and message is null
     * @throws IllegalArgumentException if collection size is out of range and message is not null
     */
    public static boolean hasSizeBetween(Collection<?> collection, int minSize, int maxSize, String message) {
        if (collection == null || collection.size() < minSize || collection.size() > maxSize) {
            if (message != null) throw new IllegalArgumentException(message);
            return false;
        }
        return true;
    }

    // === MINECRAFT-SPECIFIC VALIDATIONS ===

    /**
     * Validates that a coordinate is within Minecraft's world border limits.
     * 
     * @param coord the coordinate to validate
     * @param message the exception message if validation fails (null to return false instead of throwing)
     * @return true if valid coordinate, false if invalid and message is null
     * @throws IllegalArgumentException if coordinate is out of bounds and message is not null
     */
    public static boolean isValidCoordinate(int coord, String message) {
        return inRange(coord, -30000000, 30000000, message);
    }

    /**
     * Validates that a Y coordinate is within Minecraft's height limits.
     * 
     * @param y the Y coordinate to validate
     * @param message the exception message if validation fails (null to return false instead of throwing)
     * @return true if valid height, false if invalid and message is null
     * @throws IllegalArgumentException if height is out of bounds and message is not null
     */
    public static boolean isValidHeight(int y, String message) {
        return inRange(y, -64, 320, message);
    }

    /**
     * Validates that an item stack amount is within valid limits.
     * 
     * @param amount the item amount to validate
     * @param message the exception message if validation fails (null to return false instead of throwing)
     * @return true if valid amount, false if invalid and message is null
     * @throws IllegalArgumentException if amount is out of bounds and message is not null
     */
    public static boolean isValidItemAmount(int amount, String message) {
        return inRange(amount, 1, 64, message);
    }

    // === CONVENIENCE METHODS (NON-THROWING) ===

    /**
     * Checks if a string contains only numeric characters.
     * 
     * @param str the string to check
     * @return true if string is numeric, false otherwise
     */
    public static boolean isNumeric(String str) {
        if (str == null || str.isEmpty()) return false;
        return str.matches("\\d+");
    }

    /**
     * Checks if a string contains only alphabetic characters.
     * 
     * @param str the string to check
     * @return true if string is alphabetic, false otherwise
     */
    public static boolean isAlphabetic(String str) {
        if (str == null || str.isEmpty()) return false;
        return str.matches("[a-zA-Z]+");
    }

    /**
     * Checks if a string contains only alphanumeric characters.
     * 
     * @param str the string to check
     * @return true if string is alphanumeric, false otherwise
     */
    public static boolean isAlphanumeric(String str) {
        if (str == null || str.isEmpty()) return false;
        return str.matches("[a-zA-Z0-9]+");
    }

    /**
     * Validates that a string contains only safe characters (letters, numbers, underscore, hyphen, space).
     * 
     * @param str the string to validate
     * @param message the exception message if validation fails (null to return false instead of throwing)
     * @return true if safe, false if unsafe and message is null
     * @throws IllegalArgumentException if string contains unsafe characters and message is not null
     */
    public static boolean isSafeString(String str, String message) {
        if (str == null || !str.matches("[a-zA-Z0-9_\\-\\s]+")) {
            if (message != null) throw new IllegalArgumentException(message);
            return false;
        }
        return true;
    }

    /**
     * Checks if a value is within the specified range (non-throwing version).
     * 
     * @param value the value to check
     * @param min the minimum value (inclusive)
     * @param max the maximum value (inclusive)
     * @return true if value is in range, false otherwise
     */
    public static boolean isInRange(int value, int min, int max) {
        return value >= min && value <= max;
    }

    /**
     * Checks if a value is within the specified range (non-throwing version).
     * 
     * @param value the value to check
     * @param min the minimum value (inclusive)
     * @param max the maximum value (inclusive)
     * @return true if value is in range, false otherwise
     */
    public static boolean isInRange(double value, double min, double max) {
        return value >= min && value <= max;
    }

    /**
     * Checks if a string length is within the specified range (non-throwing version).
     * 
     * @param str the string to check
     * @param minLength the minimum length (inclusive)
     * @param maxLength the maximum length (inclusive)
     * @return true if length is valid, false otherwise
     */
    public static boolean isValidLength(String str, int minLength, int maxLength) {
        return str != null && str.length() >= minLength && str.length() <= maxLength;
    }

    /**
     * Checks if a string is null or empty.
     * 
     * @param str the string to check
     * @return true if null or empty, false otherwise
     */
    public static boolean isNullOrEmpty(String str) {
        return str == null || str.isEmpty();
    }

    /**
     * Checks if a string is null, empty, or contains only whitespace.
     * 
     * @param str the string to check
     * @return true if null, empty, or blank, false otherwise
     */
    public static boolean isNullOrBlank(String str) {
        return str == null || str.trim().isEmpty();
    }

    /**
     * Checks if a collection is null or empty.
     * 
     * @param collection the collection to check
     * @return true if null or empty, false otherwise
     */
    public static boolean isNullOrEmpty(Collection<?> collection) {
        return collection == null || collection.isEmpty();
    }
}