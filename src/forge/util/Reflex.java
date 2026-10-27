package forge.util;

import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

@SuppressWarnings("unused")
public class Reflex {
    private static final Unsafe unsafe = Reflex.getPrivateStatic(Unsafe.class, "theUnsafe");
    
    public static Field field(Class<?> clazz, String field) {
        try {
            return clazz.getDeclaredField(field);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }
    
    public static Method method(Class<?> clazz, String method, Class<?>... parameters) {
        try {
            return clazz.getDeclaredMethod(method, parameters);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }
    
    // region set
    
    public static void setPrivateStatic(Class<?> clazz, String field, Object value) {
        setPrivateStatic(field(clazz, field), value);
    }
    
    public static void setPrivateStatic(Field field, Object value) {
        setPrivate(field, null, value);
    }
    
    public static void setPrivate(Class<?> clazz, String field, Object obj, Object value) {
        set(field(clazz, field), obj, value);
    }
    
    public static void setPrivate(Field field, Object obj, Object value) {
        field.setAccessible(true);
        set(field, obj, value);
    }
    
    public static void setStatic(Class<?> clazz, String field, Object value) {
        setStatic(field(clazz, field), value);
    }
    
    public static void setStatic(Field field, Object value) {
        set(field, null, value);
    }
    
    public static void set(Class<?> clazz, String field, Object obj, Object value) {
        set(field(clazz, field), obj, value);
    }
    
    public static void set(Field field, Object obj, Object value) {
        try {
            field.set(obj, value);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
    
    // endregion set
    // region setfinal
    
    public static void setPrivateStaticFinal(Class<?> clazz, String field, Object value) {
        setPrivateStaticFinal(field(clazz, field), value);
    }
    
    public static void setPrivateStaticFinal(Field field, Object value) {
        field.setAccessible(true);
        setStaticFinal(field, value);
    }
    
    public static void setPrivateFinal(Class<?> clazz, String field, Object obj, Object value) {
        setFinal(field(clazz, field), obj, value);
    }
    
    public static void setPrivateFinal(Field field, Object obj, Object value) {
        field.setAccessible(true);
        setFinal(field, obj, value);
    }
    
    public static void setStaticFinal(Class<?> clazz, String field, Object value) {
        setStaticFinal(field(clazz, field), value);
    }
    
    @SuppressWarnings("deprecation")
    public static void setStaticFinal(Field field, Object value) {
        unsafe.putObject(unsafe.staticFieldBase(field), unsafe.staticFieldOffset(field), value);
    }
    
    public static void setFinal(Class<?> clazz, String field, Object obj, Object value) {
        setFinal(field(clazz, field), obj, value);
    }
    
    @SuppressWarnings("deprecation")
    public static void setFinal(Field field, Object obj, Object value) {
        unsafe.putObject(obj, unsafe.objectFieldOffset(field), value);
    }
    
    // endregion setfinal
    // region get
    
    public static <T> T getPrivateStatic(Class<?> clazz, String field) {
        return getPrivateStatic(field(clazz, field));
    }
    
    public static <T> T getPrivateStatic(Field field) {
        return getPrivate(field, null);
    }
    
    public static <T> T getPrivate(Class<?> clazz, String field, Object obj) {
        return getPrivate(field(clazz, field), obj);
    }
    
    public static <T> T getPrivate(Field field, Object obj) {
        field.setAccessible(true);
        return get(field, obj);
    }
    
    public static <T> T getStatic(Class<?> clazz, String field) {
        return getStatic(field(clazz, field));
    }
    
    public static <T> T getStatic(Field field) {
        return get(field, null);
    }
    
    public static <T> T get(Class<?> clazz, String field, Object obj) {
        return get(field(clazz, field), obj);
    }
    
    @SuppressWarnings("unchecked")
    public static <T> T get(Field field, Object obj) {
        try {
            return (T) field.get(obj);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
    
    // endregion get
    // region invoke
    
    public static Object invokePrivateStatic(Class<?> clazz, String method, Object... args) {
        return invokePrivateStatic(method(clazz, method), args);
    }
    
    public static Object invokePrivateStatic(Method method, Object... args) {
        return invokePrivate(method, null, args);
    }
    
    public static Object invokePrivate(Class<?> clazz, String method, Object obj, Object... args) {
        return invokePrivate(method(clazz, method), obj, args);
    }
    
    public static Object invokePrivate(Method method, Object obj, Object... args) {
        method.setAccessible(true);
        return invoke(method, obj, args);
    }
    
    public static Object invokeStatic(Class<?> clazz, String method, Object... args) {
        return invokeStatic(method(clazz, method), args);
    }
    
    public static Object invokeStatic(Method method, Object... args) {
        return invoke(method, null, args);
    }
    
    public static Object invoke(Class<?> clazz, String method, Object obj, Object... args) {
        return invoke(method(clazz, method), obj, args);
    }
    
    public static Object invoke(Method method, Object obj, Object... args) {
        try {
            return method.invoke(obj, args);
        } catch (InvocationTargetException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
    
    // endregion invoke
}
