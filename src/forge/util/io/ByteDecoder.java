package forge.util.io;

/*
short
int
long
boolean
float
char
string
*/
public class ByteDecoder {
    
    public short toShort(byte b1, byte b2) {
        return (short) (
            b1 << 8 & 0xFF |
            (b1 & 0xFF)
        );
    }

    public int toInt(byte b1, byte b2, byte b3, byte b4) {
        return (
            b1 << 24 & 0xFF |
            (b2 << 16) & 0xFF |
            (b2 << 8) & 0xFF |
            b3 & 0xFF
        );
    }

    public int toLong(byte b1, byte b2, byte b3, byte b4,
                      byte b5, byte b6, byte b7, byte b8) {
    }
}
