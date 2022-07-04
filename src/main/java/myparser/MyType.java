package myparser;

import java.util.HashMap;
import java.util.function.Supplier;

public class MyType {
    public static final int A = 1;
    public static final int AAAA = 28;

    private static class MyTypeMnemonic extends MyMnemonic {
        private final HashMap<Integer, Supplier<MyRecord>> factories;

        public MyTypeMnemonic() {
            super("Type", CASE_UPPER);
            setPrefix("TYPE");
            setMaximum(0xFFFF);
            factories = new HashMap<>();
        }

        public void add(int val, String str, Supplier<MyRecord> factory) {
            super.add(val, str);
            factories.put(val, factory);
        }

        public void replace(int val, String str, Supplier<MyRecord> factory) {
            int oldVal = getValue(str);
            if (oldVal != -1) {
                if (oldVal != val) {
                    throw new IllegalArgumentException(
                            "mnemnonic \"" + str + "\" already used by type " + oldVal);
                } else {
                    remove(val);
                    factories.remove(val);
                }
            }
            add(val, str, factory);
        }

        @Override
        public void check(int val) {
            MyType.check();
        }

        public Supplier<MyRecord> getFactory(int val) {
            check(val);
            return factories.get(val);
        }
    }

    private static final MyTypeMnemonic types = new MyTypeMnemonic();

    static {
        types.add(A, "A", MyARecord::new);
        types.add(AAAA, "AAAA", MyAAAARecord::new);
    }

    private MyType() {
    }

    public static void check() {
    }

    public static String string(int val) {
        return types.getText(val);
    }

    public static int value(String s, boolean numberok) {
        int val = types.getValue(s);
        if (val == -1 && numberok) {
            val = types.getValue("TYPE" + s);
        }
        return val;
    }

    public static int value(String s) {
        return value(s, false);
    }

    static Supplier<MyRecord> getFactory(int val) {
        return types.getFactory(val);
    }
}
