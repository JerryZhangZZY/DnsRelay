package myDNS;

import java.io.IOException;

public class MyDnsOutput {

    private byte[] array;
    private int pos;
    private int saved_pos;
    public MyDnsOutput(int size) {
        array = new byte[size];
        pos = 0;
        saved_pos = -1;
    }
    public MyDnsOutput() {
        this(32);
    }

    public int current() {
        return pos;
    }
    public byte[] toByteArray() {
        byte[] out = new byte[pos];
        System.arraycopy(array, 0, out, 0, pos);
        return out;
    }
    public void writeU8(int val) {
        check(val, 8);
        need(1);
        array[pos++] = (byte) (val & 0xFF);
    }

    /**
     * Writes an unsigned 16 bit value to the stream.
     *
     * @param val The value to be written
     */
    public void writeU16(int val) {
        check(val, 16);
        need(2);
        array[pos++] = (byte) ((val >>> 8) & 0xFF);
        array[pos++] = (byte) (val & 0xFF);
    }

    /**
     * Writes an unsigned 16 bit value to the specified position in the stream.
     *
     * @param val The value to be written
     * @param where The position to write the value.
     */
    public void writeU16At(int val, int where) {
        check(val, 16);
        if (where > pos - 2) {
            throw new IllegalArgumentException("cannot write past end of data");
        }
        array[where++] = (byte) ((val >>> 8) & 0xFF);
        array[where++] = (byte) (val & 0xFF);
    }

    /**
     * Writes an unsigned 32 bit value to the stream.
     *
     * @param val The value to be written
     */
    public void writeU32(long val) {
        check(val, 32);
        need(4);
        array[pos++] = (byte) ((val >>> 24) & 0xFF);
        array[pos++] = (byte) ((val >>> 16) & 0xFF);
        array[pos++] = (byte) ((val >>> 8) & 0xFF);
        array[pos++] = (byte) (val & 0xFF);
    }
    private void check(long val, int bits) {
        long max = 1;
        max <<= bits;
        if (val < 0 || val > max) {
            try {
                throw new IOException(val + " out of range for " + bits + " bit value");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
    private void need(int n) {
        if (array.length - pos >= n) {
            return;
        }
        int newsize = array.length * 2;
        if (newsize < pos + n) {
            newsize = pos + n;
        }
        byte[] newarray = new byte[newsize];
        System.arraycopy(array, 0, newarray, 0, pos);
        array = newarray;
    }
    public void writeByteArray(byte[] b) {
        writeByteArray(b, 0, b.length);
    }
    public void writeByteArray(byte[] b, int off, int len) {
        System.arraycopy(b, off, array, pos, len);
        pos += len;
    }
}
