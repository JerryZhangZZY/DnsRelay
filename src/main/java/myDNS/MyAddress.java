package myDNS;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;

public class MyAddress {

    public static final int IPv4 = 1;
    public static final int IPv6 = 2;

    private MyAddress() {}

    /**
     * Returns the family of an InetAddress.
     *
     * @param address The supplied address.
     * @return The family, either IPv4 or IPv6.
     */
    public static int familyOf(InetAddress address) {
        if (address instanceof Inet4Address) {
            return IPv4;
        }
        if (address instanceof Inet6Address) {
            return IPv6;
        }
        throw new IllegalArgumentException("unknown address family");
    }

}
