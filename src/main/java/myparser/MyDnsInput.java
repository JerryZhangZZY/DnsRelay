package myparser;


import java.io.IOException;
import java.nio.ByteBuffer;

public class MyDnsInput {
    public final ByteBuffer byteBuffer;
    private final int offset;
    private final int limit;
    private int savedPos;
    private int savedEnd;
    public MyDnsInput(byte[] input) {
        ByteBuffer byteBuffer = ByteBuffer.wrap(input);
        this.byteBuffer = byteBuffer;
        offset = byteBuffer.position();
        limit = byteBuffer.limit();
        savedPos = -1;
        savedEnd = -1;
    }
    public int current() {
        return byteBuffer.position() - offset;
    }
    public int readU8() {
        return byteBuffer.get() & 0xFF;
    }
    public int readU16() {
        return byteBuffer.getShort() & 0xFFFF;
    }

    public long readU32() {
        return byteBuffer.getInt() & 0xFFFFFFFFL;
    }
    public int remaining() {
        return byteBuffer.remaining();
    }

    public void readByteArray(byte[] b, int off, int len) {
        byteBuffer.get(b, off, len);
    }
    public byte[] readByteArray(int len) {
        byte[] out = new byte[len];
        byteBuffer.get(out, 0, len);
        return out;
    }
    public void save() {
        savedPos = byteBuffer.position();
        savedEnd = byteBuffer.limit();
    }
    public void jump(int index) {
        if (index + offset >= limit) {
            throw new IllegalArgumentException("cannot jump past end of input");
        }
        byteBuffer.position(offset + index);
        byteBuffer.limit(limit);
    }
    public void restore() {
        if (savedPos < 0) {
            throw new IllegalStateException("no previous state");
        }
        byteBuffer.position(savedPos);
        byteBuffer.limit(savedEnd);
        savedPos = -1;
        savedEnd = -1;
    }
}
