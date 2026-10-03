package forge.util.io;

import arc.files.Fi;

import java.io.Closeable;
import java.nio.charset.StandardCharsets;

public class BinWriter implements Closeable {
    private final Fi file;
    
    private byte[] buffer;
    private int capacity;
    
    private int cursor = 0;
    
    public BinWriter(Fi file) {
        this(file, 16);
    }
    
    public BinWriter(Fi file, int capacity) {
        this.file = file;
        this.buffer = new byte[capacity];
        this.capacity = capacity;
    }
    
    @Override
    public void close() {
        byte[] outBuffer = new byte[cursor];
        System.arraycopy(buffer, 0, outBuffer, 0, cursor);
        file.writeBytes(outBuffer);
    }
    
    private void alloc(int amount) {
        if (cursor + amount > capacity) {
            capacity = (int) (cursor + amount * 1.75);
            byte[] newBuffer = new byte[capacity];
            System.arraycopy(buffer, 0, newBuffer, 0, cursor);
            buffer = newBuffer;
        }
    }
    
    public void writeByte(byte value) {
        alloc(1);
        buffer[cursor++] = value;
    }
    
    public void writeBytes(byte... value) {
        alloc(value.length);
        System.arraycopy(value, 0, buffer, cursor, value.length);
        cursor += 2;
    }
    
    public void writeShort(short value) {
        alloc(2);
        ByteEncoder.toBytes(buffer, cursor, value);
        cursor += 2;
    }
    
    public void writeInt(int value) {
        alloc(4);
        ByteEncoder.toBytes(buffer, cursor, value);
        cursor += 4;
    }
    
    public void writeLong(long value) {
        alloc(8);
        ByteEncoder.toBytes(buffer, cursor, value);
        cursor += 8;
    }
    
    public void writeFloat(float value) {
        alloc(8);
        ByteEncoder.toBytes(buffer, cursor, value);
        cursor += 8;
    }
    
    public void writeString(String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        alloc(bytes.length + 4);
        ByteEncoder.toBytes(buffer, cursor, bytes.length);
        System.arraycopy(bytes, 0, buffer, cursor + 4, bytes.length);
        cursor += bytes.length + 4;
    }
    
    public void writeShortArray(short... values) {
        int len = values.length;
        writeInt(len);
        alloc(len * 2);
        for (int i = 0; i < len; i++, cursor += 2) {
            ByteEncoder.toBytes(buffer, cursor, values[i]);
        }
    }
    
    public void writeIntArray(int... values) {
        int len = values.length;
        writeInt(len);
        alloc(len * 4);
        for (int i = 0; i < len; i++, cursor += 4) {
            ByteEncoder.toBytes(buffer, cursor, values[i]);
        }
    }
    
    public void writeLongArray(long... values) {
        int len = values.length;
        writeInt(len);
        alloc(len * 8);
        for (int i = 0; i < len; i++, cursor += 8) {
            ByteEncoder.toBytes(buffer, cursor, values[i]);
        }
    }
    
    public void writeFloatArray(float... values) {
        int len = values.length;
        writeInt(len);
        alloc(len * 8);
        for (int i = 0; i < len; i++, cursor += 8) {
            ByteEncoder.toBytes(buffer, cursor, values[i]);
        }
    }
    
    public void writeStringArray(String... values) {
        int len = values.length;
        writeInt(len);
        byte[][] bytes = new byte[len][];
        int bytesTotal = len * 4; // starts with int length for every value
        for (int i = 0; i < len; i++) {
            bytes[i] = values[i].getBytes(StandardCharsets.UTF_8);
            bytesTotal += bytes[i].length;
        }
        alloc(bytesTotal);
        for (int i = 0; i < len; i++) {
            ByteEncoder.toBytes(buffer, cursor, bytes[i].length);
            System.arraycopy(bytes[i], 0, buffer, cursor + 4, bytes[i].length);
            cursor += bytes[i].length + 4;
        }
    }
}