package myDNS;

public class MyEmptyRecord extends MyRecord{
        MyEmptyRecord() {}

        @Override
        protected void rrFromWire(MyDnsInput in) {}

        @Override
        protected void rrToWire(MyDnsOutput out, MyCompression c, boolean canonical) {}

}
