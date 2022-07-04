package myparser;


import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class MyARecord extends MyRecord{
    private int addr;

    MyARecord() {}

    private static int fromArray(byte[] array) {
        return ((array[0] & 0xFF) << 24)
                | ((array[1] & 0xFF) << 16)
                | ((array[2] & 0xFF) << 8)
                | (array[3] & 0xFF);
    }

    private static byte[] toArray(int addr) {
        byte[] bytes = new byte[4];
        bytes[0] = (byte) ((addr >>> 24) & 0xFF);
        bytes[1] = (byte) ((addr >>> 16) & 0xFF);
        bytes[2] = (byte) ((addr >>> 8) & 0xFF);
        bytes[3] = (byte) (addr & 0xFF);
        return bytes;
    }

    public MyARecord(MyName name, int dclass, long ttl, InetAddress address) {
        super(name, MyType.A, dclass, ttl);
        if (MyAddress.familyOf(address) != MyAddress.IPv4) {
            throw new IllegalArgumentException("invalid IPv4 address");
        }
        addr = fromArray(address.getAddress());
    }

    @Override
    protected void rrFromWire(MyDnsInput in) throws IOException {
        addr = fromArray(in.readByteArray(4));
    }

    @Override
    protected void rrToWire(MyDnsOutput out) {
        out.writeU32((long) addr & 0xFFFFFFFFL);
    }

    public InetAddress getAddress() {
        try {
            if (name == null) {
                return InetAddress.getByAddress(toArray(addr));
            } else {
                return InetAddress.getByAddress(name.toString(), toArray(addr));
            }
        } catch (UnknownHostException e) {
            return null;
        }
    }

}
