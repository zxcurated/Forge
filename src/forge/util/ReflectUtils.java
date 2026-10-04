package forge.util;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class ReflectUtils {
    
    @SuppressWarnings("unchecked")
    public static <T> T get(Field field, Object obj) {
        try {
            field.setAccessible(true);
            return (T) field.get(obj);
        } catch (IllegalAccessException e) {
            throw new Error(e);
        }
    }
    
    public static <T> T get(Field field) {
        return get(field, null);
    }
    
    @SuppressWarnings("unchecked")
    public static <T> T get(Class<?> clazz, Object obj, String name) {
        try {
            return get(clazz.getDeclaredField(name), obj);
        } catch (NoSuchFieldException e) {
            throw new Error(e);
        }
    }
    
    @SuppressWarnings("unchecked")
    public static <T> T get(Class<?> clazz, String name) {
        return get(clazz, null, name);
    }
    
    public static void set(Field field, Object value) {
        set(null, field, value);
    }
    
    public static void set(Object obj, Field field, Object value) {
        try {
            field.setAccessible(true);
            field.set(obj, value);
        } catch (IllegalAccessException e) {
            throw new Error(e);
        }
    }
    
    public static Object invoke(Object obj, Method method, Object... args) {
        try {
            method.setAccessible(true);
            return method.invoke(obj, args);
        } catch (InvocationTargetException | IllegalAccessException e) {
            throw new Error(e);
        }
    }
    
    public static Object invoke(Method method, Object... args) {
        return invoke(null, method, args);
    }
}
