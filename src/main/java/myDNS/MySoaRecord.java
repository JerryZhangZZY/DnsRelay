package myDNS;

import java.io.IOException;

public class MySoaRecord extends MyRecord{
    private MyName host, admin;
    private long serial, refresh, retry, expire, minimum;

    MySoaRecord() {}

    /**
     * Creates an SOA Record from the given data
     *
     * @param host The primary name server for the zone
     * @param admin The zone administrator's address
     * @param serial The zone's serial number
     * @param refresh The amount of time until a secondary checks for a new serial number
     * @param retry The amount of time between a secondary's checks for a new serial number
     * @param expire The amount of time until a secondary expires a zone
     * @param minimum The minimum TTL for records in the zone
     */
    public MySoaRecord(
            MyName name,
            int dclass,
            long ttl,
            MyName host,
            MyName admin,
            long serial,
            long refresh,
            long retry,
            long expire,
            long minimum) {
        super(name, MyType.SOA, dclass, ttl);
//        this.host = checkName("host", host);
//        this.admin = checkName("admin", admin);
//        this.serial = checkU32("serial", serial);
//        this.refresh = checkU32("refresh", refresh);
//        this.retry = checkU32("retry", retry);
//        this.expire = checkU32("expire", expire);
//        this.minimum = checkU32("minimum", minimum);
    }

    @Override
    protected void rrFromWire(MyDnsInput in) throws IOException {
        host = new MyName(in);
        admin = new MyName(in);
        serial = in.readU32();
        refresh = in.readU32();
        retry = in.readU32();
        expire = in.readU32();
        minimum = in.readU32();
    }

    @Override
    protected void rrToWire(MyDnsOutput out, MyCompression c, boolean canonical) {
        host.toWire(out, c, canonical);
        admin.toWire(out, c, canonical);
        out.writeU32(serial);
        out.writeU32(refresh);
        out.writeU32(retry);
        out.writeU32(expire);
        out.writeU32(minimum);
    }
}
