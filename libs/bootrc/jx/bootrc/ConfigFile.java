package jx.bootrc;

import jx.zero.*;
import java.util.*;

public class ConfigFile {
    Vector pairs=new Vector();
    Hashtable name2value = new Hashtable();
    ReadOnlyMemory mem;
    public ConfigFile(ReadOnlyMemory mem) {
	this.mem = mem;
    	this.mem = mem;
	for(;;) {
	    String line = readline();
	    if (line == null) break;
	    line = line.trim();
	    if (line.length() == 0 || line.charAt(0) == '#') continue;
	    String[] pair = BootRC2.splitByChar(line, '=');
	    Pair p = new Pair();
	    p.name = pair[0].trim();
	    p.value = pair[1].trim();
	    pairs.addElement(p);
	    name2value.put(p.name, p.value);
	}
    }
    public String get(String key) {
	return (String)name2value.get(key);
    }

    int pos;
    final static int NEWLINE = 0x0a;
    final static int MAXLINE = 256;
    char data[] = new char[MAXLINE];

    private String readline() {
	byte b;
	int i=0;
	while(pos < mem.size() && (b = mem.get8(pos)) != NEWLINE) {
	    data[i] = (char)b;
	    pos++;
	    i++;
	} 
	if (pos >= mem.size() && i==0) return null;
	pos++;
	String s = new String(data, 0, i);
	return s;
    }

}
