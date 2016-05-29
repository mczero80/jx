package jx.bootrc;

import jx.zero.*;

public class Section {
    public String name;
    public Record records;
    public Record cur;
    public Record last;
    public Section next;
    public Record nextRecord() {
	if (cur==null) return null;
	Record ret = cur;
	cur = cur.next;
	return ret;
    }
    public void reset() { cur = records; }
    public void add(Record r) {
	if (records == null) {
	    records = r;
	    cur = records;
	    last = records;
	    return;
	} 
	last.next = r;
	last = r;
    }
}

