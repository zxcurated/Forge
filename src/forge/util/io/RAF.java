package forge.util.io;

import java.io.IOException;
import java.io.RandomAccessFile;

public class RAF {
    
    public static void writeStringArray(RandomAccessFile raf, String[] array) throws IOException {
        raf.writeInt(array.length);
        for (String element : array) {
            raf.writeUTF(element);
        }
    }
    
    public static String[] readStringArray(RandomAccessFile raf) throws IOException {
        int len = raf.readInt();
        String[] array = new String[len];
        for (int i = 0; i < len; i++) {
            array[i] = raf.readUTF();
        }
        return array;
    }
}
