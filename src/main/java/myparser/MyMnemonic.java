package myparser;

import java.util.HashMap;

public class MyMnemonic {

    /* Strings are case-sensitive. */
    static final int CASE_SENSITIVE = 1;

    /* Strings will be stored/searched for in uppercase. */
    static final int CASE_UPPER = 2;

    /* Strings will be stored/searched for in lowercase. */
    static final int CASE_LOWER = 3;

    private final HashMap<String, Integer> strings;
    private final HashMap<Integer, String> values;
    private final String description;
    private final int wordcase;
    private String prefix;
    private int max;
    private boolean numericok;

    public MyMnemonic(String description, int wordcase) {
        this.description = description;
        this.wordcase = wordcase;
        strings = new HashMap<>();
        values = new HashMap<>();
        max = Integer.MAX_VALUE;
    }

    public void setMaximum(int max) {
        this.max = max;
    }

    public void setPrefix(String prefix) {
        this.prefix = sanitize(prefix);
    }

    public void check(int val) {
        if (val < 0 || val > max) {
            throw new IllegalArgumentException(description + " " + val + " is out of range");
        }
    }

    /* Converts a String to the correct case. */
    private String sanitize(String str) {
        if (wordcase == CASE_UPPER) {
            return str.toUpperCase();
        } else if (wordcase == CASE_LOWER) {
            return str.toLowerCase();
        }
        return str;
    }

    private int parseNumeric(String s) {
        try {
            int val = Integer.parseInt(s);
            if (val >= 0 && val <= max) {
                return val;
            }
        } catch (NumberFormatException e) {
        }
        return -1;
    }

    public void add(int val, String str) {
        check(val);
        str = sanitize(str);
        strings.put(str, val);
        values.put(val, str);
    }

    public void remove(int val) {
        values.remove(val);
        strings.entrySet().removeIf(entry -> entry.getValue() == val);
    }

    public String getText(int val) {
        check(val);
        String str = values.get(val);
        if (str != null) {
            return str;
        }
        str = Integer.toString(val);
        if (prefix != null) {
            return prefix + str;
        }
        return str;
    }

    public int getValue(String str) {
        str = sanitize(str);
        Integer value = strings.get(str);
        if (value != null) {
            return value;
        }
        if (prefix != null) {
            if (str.startsWith(prefix)) {
                int val = parseNumeric(str.substring(prefix.length()));
                if (val >= 0) {
                    return val;
                }
            }
        }
        if (numericok) {
            return parseNumeric(str);
        }
        return -1;
    }
}
