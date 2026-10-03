package forge.util.io;

import java.nio.charset.StandardCharsets;

public class ByteEncoder {
    
    public static byte[] toBytes(short value) {
        return new byte[]{
            (byte) (value >> 8),
            (byte) (value & 0xFF)
        };
    }
    
    public static byte[] toBytes(int value) {
        return new byte[]{
            (byte) (value >> 24),
            (byte) (value >> 16),
            (byte) (value >> 8),
            (byte) (value & 0xFF)
        };
    }
    
    public static byte[] toBytes(long value) {
        return new byte[]{
            (byte) (value >> 56),
            (byte) (value >> 48),
            (byte) (value >> 40),
            (byte) (value >> 32),
            (byte) (value >> 24),
            (byte) (value >> 16),
            (byte) (value >> 8),
            (byte) (value & 0xFF)
        };
    }
    
    public static byte[] toBytes(float value) {
        return toBytes(Float.floatToIntBits(value));
    }
    
    public static byte[] toBytes(String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        byte[] allBytes = new byte[bytes.length + 4];
        System.arraycopy(toBytes(value.length()), 0, allBytes, 0, 4);
        System.arraycopy(bytes, 0, allBytes, 4, bytes.length);
        return allBytes;
    }
    
    public static void toBytes(byte[] array, int cursor, short value) {
        System.arraycopy(toBytes(value), 0, array, cursor, 2);
    }
    
    public static void toBytes(byte[] array, int cursor, int value) {
        System.arraycopy(toBytes(value), 0, array, cursor, 4);
    }
    
    public static void toBytes(byte[] array, int cursor, long value) {
        System.arraycopy(toBytes(value), 0, array, cursor, 8);
    }
    
    public static void toBytes(byte[] array, int cursor, float value) {
        System.arraycopy(toBytes(value), 0, array, cursor, 8);
    }
    
    public static void toBytes(byte[] array, int cursor, String value) {
        byte[] bytes = toBytes(value);
        System.arraycopy(bytes, 0, array, cursor, bytes.length);
    }
}
