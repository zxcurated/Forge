package forge.util;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class ReflectUtils {

    @SuppressWarnings("unchecked")
    public static <T> T get(Class<?> clazz, String name) {
        try {
            Field field = clazz.getDeclaredField(name);
            field.setAccessible(true);
            return (T) field.get(null);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            throw new Error(e);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> T get(Object obj, String name) {
        try {
            Field field = obj.getClass().getDeclaredField(name);
            field.setAccessible(true);
            return (T) field.get(obj);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            throw new Error(e);
        }
    }

    public static void invoke(Object obj, Method method, Object... args) {
        try {
            method.setAccessible(true);
            method.invoke(obj, args);
        } catch (InvocationTargetException | IllegalAccessException e) {
            throw new Error(e);
        }
    }

    public static void invoke(Method method, Object... args) {
        try {
            method.setAccessible(true);
            method.invoke(null, args);
        } catch (InvocationTargetException | IllegalAccessException e) {
            throw new Error(e);
        }
    }
}
