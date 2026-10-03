package forge.util.io;

import arc.files.Fi;

public class BinReader {
    private final byte[] bytes;
    private int cursor = 0;
    
    public BinReader(Fi file) {
        bytes = file.readBytes();
    }
    
    public byte readByte() {
        return bytes[cursor++];
    }
    
    public short readShort() {
        var value = ByteDecoder.toShort(bytes, cursor);
        cursor += 2;
        return value;
    }
    
    public int readInt() {
        var value = ByteDecoder.toInt(bytes, cursor);
        cursor += 4;
        return value;
    }
    
    public long readLong() {
        var value = ByteDecoder.toLong(bytes, cursor);
        cursor += 8;
        return value;
    }
    
    public float readFloat() {
        var value = ByteDecoder.toFloat(bytes, cursor);
        cursor += 8;
        return value;
    }
    
    public String readString() {
        int len = readInt();
        byte[] stringBytes = new byte[len];
        System.arraycopy(bytes, cursor, stringBytes, 0, len);
        cursor += stringBytes.length;
        return new String(stringBytes);
    }
    
    public short[] readShortArray() {
        int len = readInt();
        short[] array = new short[len];
        for (int i = 0; i < len; i++, cursor += 2) {
            array[i] = ByteDecoder.toShort(bytes, cursor);
        }
        return array;
    }
    
    public int[] readIntArray() {
        int len = readInt();
        int[] array = new int[len];
        for (int i = 0; i < len; i++, cursor += 4) {
            array[i] = ByteDecoder.toInt(bytes, cursor);
        }
        return array;
    }
    
    public long[] readLongArray() {
        int len = readInt();
        long[] array = new long[len];
        for (int i = 0; i < len; i++, cursor += 8) {
            array[i] = ByteDecoder.toLong(bytes, cursor);
        }
        return array;
    }
    
    public float[] readFloatArray() {
        int len = readInt();
        float[] array = new float[len];
        for (int i  = 0; i < len; i++, cursor += 8) {
            array[i] = ByteDecoder.toFloat(bytes, cursor);
        }
        return array;
    }
    
    public String[] readStringArray() {
        int len = readInt();
        String[] array = new String[len];
        for (int i = 0; i < len; i++) {
            array[i] = readString();
        }
        return array;
    }
}