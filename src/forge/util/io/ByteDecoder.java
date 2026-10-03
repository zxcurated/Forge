package forge.util.io;

public class ByteDecoder {

    public static short toShort(byte b1, byte b2) {
        return (short) (
            ((b1 & 0xFF) << 8) |
            (b2 & 0xFF)
        );
    }

    public static int toInt(byte b1, byte b2, byte b3, byte b4) {
        return (
            ((b1 & 0xFF) << 24) |
            ((b2 & 0xFF) << 16) |
            ((b3 & 0xFF) << 8) |
            (b4 & 0xFF)
        );
    }
    
    public static long toLong(byte b1, byte b2, byte b3, byte b4, byte b5, byte b6, byte b7, byte b8) {
        return (
            ((long) (b1 & 0xFF) << 56) |
            ((long) (b2 & 0xFF) << 48) |
            ((long) (b3 & 0xFF) << 40) |
            ((long) (b4 & 0xFF) << 32) |
            ((long) (b5 & 0xFF) << 24) |
            ((long) (b6 & 0xFF) << 16) |
            ((long) (b7 & 0xFF) << 8) |
            ((long) (b8 & 0xFF))
        );
    }
    
    public static float toFloat(byte b1, byte b2, byte b3, byte b4) {
        return Float.intBitsToFloat(toInt(b1, b2, b3, b4));
    }
    
    public static String toString(byte... bytes){
        return new String(bytes);
    }
    
    public static short toShort(byte[] array, int cursor) {
        return toShort(
            array[cursor],
            array[cursor + 1]
        );
    }
    
    public static int toInt(byte[] array, int cursor) {
        return toInt(
            array[cursor],
            array[cursor + 1],
            array[cursor + 2],
            array[cursor + 3]
        );
    }
    
    public static long toLong(byte[] array, int cursor) {
        return toLong(
            array[cursor],
            array[cursor + 1],
            array[cursor + 2],
            array[cursor + 3],
            array[cursor + 4],
            array[cursor + 5],
            array[cursor + 6],
            array[cursor + 7]
        );
    }
    
    public static float toFloat(byte[] array, int cursor) {
        return toFloat(
            array[cursor],
            array[cursor + 1],
            array[cursor + 2],
            array[cursor + 3]
        );
    }
    
    public static String toString(byte[] array, int cursor) {
        int length = toInt(
            array[cursor],
            array[cursor + 1],
            array[cursor + 2],
            array[cursor + 3]
        );
        byte[] bytes = new byte[length];
        System.arraycopy(array, cursor + 4, bytes, 0, length);
        return new String(bytes);
    }
}