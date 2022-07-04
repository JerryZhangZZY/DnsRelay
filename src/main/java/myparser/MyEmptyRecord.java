package myparser;

public class MyEmptyRecord extends MyRecord{
        MyEmptyRecord() {}

        @Override
        protected void rrFromWire(MyDnsInput in) {}

        @Override
        protected void rrToWire(MyDnsOutput out) {}
}
