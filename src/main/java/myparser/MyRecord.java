package myparser;


import java.io.IOException;
import java.util.function.Supplier;

public abstract class MyRecord {
    protected MyName name;
    protected int type;
    protected int dclass;
    protected long ttl;

    public MyName getName() {
        return name;
    }

    public int getType() {
        return type;
    }

    public int getDClass() {
        return dclass;
    }

    protected MyRecord() {
    }

    protected MyRecord(MyName name, int type, int dclass, long ttl) {
        this.name = name;
        this.type = type;
        this.dclass = dclass;
        this.ttl = ttl;
    }

    static MyRecord fromWire(MyDnsInput in, int section, boolean isUpdate) throws IOException {
        int type;
        int dclass;
        long ttl;
        int length;
        MyName name;
        MyRecord rec;

        name = new MyName(in);
        type = in.readU16();
        dclass = in.readU16();

        if (section == 0) {
            return newRecord(name, type, dclass);
        }

        ttl = in.readU32();
        length = in.readU16();
        if (length == 0 && isUpdate && (section == 1 || section == 2)) {
            return newRecord(name, type, dclass, ttl);
        }
        rec = newRecord(name, type, dclass, ttl, length, in);
        return rec;
    }

    public static MyRecord newRecord(MyName name, int type, int dclass) {
        return newRecord(name, type, dclass, 0);
    }

    public static MyRecord newRecord(MyName name, int type, int dclass, long ttl) {
        return getEmptyRecord(name, type, dclass, ttl, false);
    }

    private static MyRecord newRecord(
            MyName name, int type, int dclass, long ttl, int length, MyDnsInput in) throws IOException {
        MyRecord rec;
        rec = getEmptyRecord(name, type, dclass, ttl, in != null);
        if (in != null) {
            in.remaining();
            rec.rrFromWire(in);
            in.remaining();
        }
        return rec;
    }

    private static MyRecord getEmptyRecord(MyName name, int type, int dclass, long ttl, boolean hasData) {
        MyRecord rec;
        if (hasData) {
            Supplier<MyRecord> factory = MyType.getFactory(type);
            if (factory != null)
                rec = factory.get();
            else
                rec = new MyEmptyRecord();
        } else {
            rec = new MyEmptyRecord();
        }
        rec.name = name;
        rec.type = type;
        rec.dclass = dclass;
        rec.ttl = ttl;
        return rec;
    }

    protected abstract void rrFromWire(MyDnsInput in) throws IOException;

    protected abstract void rrToWire(MyDnsOutput out);

    void toWire(MyDnsOutput out, int section, MyCompression c) {
        name.toWire(out, c);
        out.writeU16(type);
        out.writeU16(dclass);
        if (section == 0) {
            return;
        }
        out.writeU32(ttl);
        int lengthPosition = out.current();
        out.writeU16(0); /* until we know better */
        rrToWire(out);
        int rrlength = out.current() - lengthPosition - 2;
        out.writeU16At(rrlength, lengthPosition);
    }
}
