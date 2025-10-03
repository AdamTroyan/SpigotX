package dev.adam.utils;

import java.lang.reflect.Method;

public class ReflectionUtils {
    public static Object callMethod(Object obj, String method) {
        try {
            Method m = obj.getClass().getMethod(method);
            return m.invoke(obj);
        } catch (Exception e) {
            return null;
        }
    }
}