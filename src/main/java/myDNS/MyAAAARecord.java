package myDNS;


import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class MyAAAARecord extends MyRecord{
    private byte[] address;

    MyAAAARecord() {}

    /**
     * Creates an AAAA Record from the given data
     *
     * @param address The address suffix
     */
    public MyAAAARecord(MyName name, int dclass, long ttl, InetAddress address) {
        super(name, MyType.AAAA, dclass, ttl);
        if (MyAddress.familyOf(address) != MyAddress.IPv4 && MyAddress.familyOf(address) != MyAddress.IPv6) {
            throw new IllegalArgumentException("invalid IPv4/IPv6 address");
        }
        this.address = address.getAddress();
    }

    @Override
    protected void rrFromWire(MyDnsInput in) throws IOException {
        address = in.readByteArray(16);
    }

    @Override
    protected void rrToWire(MyDnsOutput out, MyCompression c, boolean canonical) {
        out.writeByteArray(address);
    }

    /** Returns the address */
    public InetAddress getAddress() {
        try {
            if (name == null) {
                return InetAddress.getByAddress(address);
            } else {
                return InetAddress.getByAddress(name.toString(), address);
            }
        } catch (UnknownHostException e) {
            return null;
        }
    }

}
