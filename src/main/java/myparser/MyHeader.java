package myparser;


import java.io.IOException;

public class MyHeader implements Cloneable{
    private int id;
    private int flags;
    private int[] counts;

    MyHeader(MyDnsInput in) throws IOException {
        this.id = in.readU16();
        counts = new int[4];
        flags = 0;
        flags = in.readU16();
        for (int i = 0; i < counts.length; i++) {
            counts[i] = in.readU16();
        }
    }
    void increaseCount(int field) {
        if (counts[field] == 0xFFFF) {
            throw new IllegalStateException("DNS section count cannot be incremented");
        }
        counts[field]++;
    }
    public void setRcode(int value) {
        if (value < 0 || value > 0xF) {
            throw new IllegalArgumentException("DNS Rcode " + value + " is out of range");
        }
        flags &= ~0xF;
        flags |= value;
    }
    public boolean getFlag(int bit) {
        // bits are indexed from left to right
        return (flags & (1 << (15 - bit))) != 0;
    }
    public int getOpcode() {
        return (flags >> 11) & 0xF;
    }
    public int getCount(int field) {
        return counts[field];
    }
    void toWire(MyDnsOutput out) {
        out.writeU16(getID());
        out.writeU16(flags);
        for (int count : counts) {
            out.writeU16(count);
        }
    }
    public int getID() {
        return id;
    }
    @Override
    public MyHeader clone() {
        MyHeader h = null;
        try {
            h = (MyHeader) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
        h.id = id;
        h.flags = flags;
        h.counts = new int[h.counts.length];
        System.arraycopy(counts, 0, h.counts, 0, counts.length);
        return h;
    }
}
