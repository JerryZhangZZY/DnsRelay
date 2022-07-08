package myparser;

import org.xbill.DNS.DClass;
import org.xbill.DNS.OPTRecord;
import org.xbill.DNS.Opcode;
import org.xbill.DNS.Section;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class MyMessage implements Cloneable {
    private MyHeader header;
    private List<MyRecord>[] sections;  //0:question, 1:answer, 2:authority, 3:addition
    private int size;

    public MyMessage(byte[] b) throws IOException {
        MyDnsInput in = new MyDnsInput(b);
        MyHeader header = new MyHeader(in);
        sections = new List[4];
        this.header = header;
        boolean isUpdate = header.getOpcode() == 5;
        boolean truncated = header.getFlag(6);
        try {
            for (int i = 0; i < 4; i++) {
                int count = header.getCount(i);
                if (count > 0) {
                    sections[i] = new ArrayList<>(count);
                }
                for (int j = 0; j < count; j++) {
                    int pos = in.current();
                    MyRecord rec = MyRecord.fromWire(in, i, isUpdate);
                    sections[i].add(rec);
                }
            }
        } catch (IOException e) {
            if (!truncated) {
                throw e;
            }
        }
        size = in.current();
    }


    public MyRecord getQuestion() {
        List<MyRecord> l = sections[0];
        if (l == null || l.isEmpty()) {
            return null;
        }
        return l.get(0);
    }

    public MyHeader getHeader() {
        return header;
    }

    public List<MyRecord> getSection(int section) {
        if (sections[section] == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(sections[section]);
    }

    public void addRecord(MyRecord r, int section) {
        if (sections[section] == null) {
            sections[section] = new LinkedList<>();
        }
        header.increaseCount(section);
        sections[section].add(r);
    }

    public byte[] toWire() {
        MyDnsOutput out = new MyDnsOutput();
        toWire(out);
        size = out.current();
        return out.toByteArray();
    }

    void toWire(MyDnsOutput out) {
        header.toWire(out);
        MyCompression c = new MyCompression();
        for (int i = 0; i < sections.length; i++) {
            if (sections[i] == null) {
                continue;
            }
            for (MyRecord rec : sections[i]) {
                rec.toWire(out, i, c);
            }
        }
    }

    @Override
    public MyMessage clone() {
        MyMessage m = null;
        try {
            m = (MyMessage) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
        m.sections = (List<MyRecord>[]) new List[sections.length];
        for (int i = 0; i < sections.length; i++) {
            if (sections[i] != null) {
                m.sections[i] = new LinkedList<>(sections[i]);
            }
        }
        m.header = header.clone();
        return m;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 4; i++) {
            if (header.getOpcode() != Opcode.UPDATE) {
                sb.append(";; ").append(Section.longString(i)).append(":\n");
            } else {
                sb.append(";; ").append(Section.updString(i)).append(":\n");
            }
            sectionToString(sb, i);
            sb.append("\n");
        }
        sb.append(";; Message size: ").append(size).append(" bytes");
        return sb.toString();
    }

    private void sectionToString(StringBuilder sb, int i) {
        if (i > 3) {
            return;
        }
        for (MyRecord rec : getSection(i)) {
            if (i == Section.QUESTION) {
                sb.append(";;\t").append(rec.name);
                sb.append(", type = ").append(MyType.string(rec.type));
                sb.append(", class = IN");
            } else {
                sb.append(rec);
            }
            sb.append("\n");
        }
    }
}
