// SPDX-License-Identifier: BSD-3-Clause
// Copyright (c) 1999-2004 Brian Wellington (bwelling@xbill.org)

package dns;

import java.io.IOException;
import java.util.*;

/**
 * A DNS Message. A message is the basic unit of communication between the client and server of a
 * DNS operation. A message consists of a Header and 4 message sections.
 *
 * @see Resolver
 * @see Header
 * @see Section
 * @author Brian Wellington
 */
public class Message implements Cloneable {
  /**
   * Returns the first record in the QUESTION section.
   *
   * @see Record
   * @see Section
   */
  public Record getQuestion() {
    List<Record> l = sections[Section.QUESTION];
    if (l == null || l.isEmpty()) {
      return null;
    }
    return l.get(0);
  }


  /** The maximum length of a message in wire format. */
  public static final int MAXLENGTH = 65535;
  private Header header;
  private List<Record>[] sections;
  private int size;

  private Resolver resolver;

  @SuppressWarnings("unchecked")
  private Message(Header header) {
    sections = new List[4];
    this.header = header;
  }

  /** Creates a new Message with the specified Message ID */
  public Message(int id) {
    this(new Header(id));
  }

  /** Creates a new Message with a random Message ID */
  public Message() {
    this(new Header());
  }

  Message(DNSInput in) throws IOException {
    this(new Header(in));
    boolean isUpdate = header.getOpcode() == Opcode.UPDATE;
    boolean truncated = header.getFlag(Flags.TC);
    try {
      for (int i = 0; i < 4; i++) {
        int count = header.getCount(i);
        if (count > 0) {
          sections[i] = new ArrayList<>(count);
        }
        for (int j = 0; j < count; j++) {
          int pos = in.current();
          Record rec = Record.fromWire(in, i, isUpdate);
          sections[i].add(rec);
        }
      }
    } catch (WireParseException e) {
      if (!truncated) {
        throw e;
      }
    }
    size = in.current();
  }

  /**
   * Creates a new Message from its DNS wire format representation
   *
   * @param b A byte array containing the DNS Message.
   */
  public Message(byte[] b) throws IOException {
    this(new DNSInput(b));
  }

  /**
   * Retrieves the Header.
   *
   * @see Header
   */
  public Header getHeader() {
    return header;
  }

  /**
   * Adds a record to a section of the Message, and adjusts the header.
   *
   * @see Record
   * @see Section
   */
  public void addRecord(Record r, int section) {
    if (sections[section] == null) {
      sections[section] = new LinkedList<>();
    }
    header.incCount(section);
    sections[section].add(r);
  }

  /**
   * Returns the OPT record from the ADDITIONAL section, if one is present.
   *
   * @see OPTRecord
   * @see Section
   */



  /**
   * Returns all records in the given section, or an empty list if the section is empty.
   *
   * @see Record
   * @see Section
   */
  public List<Record> getSection(int section) {
    if (sections[section] == null) {
      return Collections.emptyList();
    }
    return Collections.unmodifiableList(sections[section]);
  }

  private static boolean sameSet(Record r1, Record r2) {
    return r1.getRRsetType() == r2.getRRsetType()
        && r1.getDClass() == r2.getDClass()
        && r1.getName().equals(r2.getName());
  }

  void toWire(DNSOutput out) {
    header.toWire(out);
    Compression c = new Compression();
    for (int i = 0; i < sections.length; i++) {
      if (sections[i] == null) {
        continue;
      }
      for (Record rec : sections[i]) {
        rec.toWire(out, i, c);
      }
    }
  }

  /* Returns the number of records not successfully rendered. */
  private int sectionToWire(DNSOutput out, int section, Compression c, int maxLength) {
    int n = sections[section].size();
    int pos = out.current();
    int rendered = 0;
    int count = 0;
    Record lastrec = null;

    for (int i = 0; i < n; i++) {
      Record rec = sections[section].get(i);
      if (section == Section.ADDITIONAL && rec instanceof OPTRecord) {
        continue;
      }

      if (lastrec != null && !sameSet(rec, lastrec)) {
        pos = out.current();
        rendered = count;
      }
      lastrec = rec;
      rec.toWire(out, section, c);
      if (out.current() > maxLength) {
        out.jump(pos);
        return n - rendered;
      }
      count++;
    }
    return n - count;
  }

  /* Returns true if the message could be rendered. */
  private void toWire(DNSOutput out, int maxLength) {
    if (maxLength < Header.LENGTH) {
      return;
    }

    int tempMaxLength = maxLength;


    int startpos = out.current();
    header.toWire(out);
    Compression c = new Compression();
    int flags = header.getFlagsByte();
    int additionalCount = 0;
    for (int i = 0; i < 4; i++) {
      int skipped;
      if (sections[i] == null) {
        continue;
      }
      skipped = sectionToWire(out, i, c, tempMaxLength);
      if (skipped != 0 && i != Section.ADDITIONAL) {
        flags = Header.setFlag(flags, Flags.TC, true);
        out.writeU16At(header.getCount(i) - skipped, startpos + 4 + 2 * i);
        for (int j = i + 1; j < Section.ADDITIONAL; j++) {
          out.writeU16At(0, startpos + 4 + 2 * j);
        }
        break;
      }
      if (i == Section.ADDITIONAL) {
        additionalCount = header.getCount(i) - skipped;
      }
    }


    if (flags != header.getFlagsByte()) {
      out.writeU16At(flags, startpos + 2);
    }

    if (additionalCount != header.getCount(Section.ADDITIONAL)) {
      out.writeU16At(additionalCount, startpos + 10);
    }
  }

  /**
   * Returns an array containing the wire format representation of the {@link Message}, but does not
   * do any additional processing (e.g. OPT/TSIG records, truncation).
   *
   * <p>Do NOT use this to actually transmit a message, use {@link #toWire(int)} instead.
   */
  public byte[] toWire() {
    DNSOutput out = new DNSOutput();
    toWire(out);
    size = out.current();
    return out.toByteArray();
  }

  public byte[] toWire(int maxLength) {
    DNSOutput out = new DNSOutput();
    toWire(out, maxLength);
    size = out.current();
    return out.toByteArray();
  }


  /**
   * Returns the size of the message. Only valid if the message has been converted to or from wire
   * format.
   */
  public int numBytes() {
    return size;
  }

  private void sectionToString(StringBuilder sb, int i) {
    if (i > 3) {
      return;
    }

    for (Record rec : getSection(i)) {
      if (i == Section.QUESTION) {
        sb.append(";;\t").append(rec.name);
        sb.append(", type = ").append(Type.string(rec.type));
        sb.append(", class = ").append(DClass.string(rec.dclass));
      } else {
        if (!(rec instanceof OPTRecord)) {
          sb.append(rec);
        }
      }
      sb.append("\n");
    }
  }

  /** Converts the Message to a String. */
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
    sb.append(";; Message size: ").append(numBytes()).append(" bytes");
    return sb.toString();
  }

  /**
   * Creates a copy of this Message. This is done by the Resolver before adding TSIG and OPT
   * records, for example.
   *
   * @see Resolver
   * @see TSIGRecord
   * @see OPTRecord
   */
  @Override
  @SuppressWarnings("unchecked")
  public Message clone() {
    Message m = null;
    try {
      m = (Message) super.clone();
    } catch (CloneNotSupportedException e) {
      throw new RuntimeException(e);
    }
    m.sections = (List<Record>[]) new List[sections.length];
    for (int i = 0; i < sections.length; i++) {
      if (sections[i] != null) {
        m.sections[i] = new LinkedList<>(sections[i]);
      }
    }
    m.header = header.clone();
    return m;
  }

  /** Sets the resolver that originally received this Message from a server. */
  public void setResolver(Resolver resolver) {
    this.resolver = resolver;
  }

}
