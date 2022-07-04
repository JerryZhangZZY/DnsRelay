package myparser;

public class MyCompression {
    private static class Entry {
        MyName name;
        int pos;
        Entry next;
    }

    private static final int TABLE_SIZE = 17;
    private static final int MAX_POINTER = 0x3FFF;
    private final Entry[] table;

    public MyCompression() {
        table = new Entry[TABLE_SIZE];
    }

    public void add(int pos, MyName name) {
        if (pos > MAX_POINTER) {
            return;
        }
        int row = (name.hashCode() & 0x7FFFFFFF) % TABLE_SIZE;
        Entry entry = new Entry();
        entry.name = name;
        entry.pos = pos;
        entry.next = table[row];
        table[row] = entry;
    }

    public int get(MyName name) {
        int row = (name.hashCode() & 0x7FFFFFFF) % TABLE_SIZE;
        int pos = -1;
        for (Entry entry = table[row]; entry != null; entry = entry.next) {
            if (entry.name.equals(name)) {
                pos = entry.pos;
            }
        }

        return pos;
    }
}
