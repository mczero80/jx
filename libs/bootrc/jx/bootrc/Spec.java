package jx.bootrc;

import jx.zero.*;
import java.util.*;

public class Spec {
    Vector pairs=new Vector();

    public String getString(String name) throws NameNotFoundException {
	String v = find(name);
	return v;
    }

    public int getInt(String name) throws NameNotFoundException {
	String v = find(name);
	return Integer.parseInt(v);
    }

    public String[] getStringArray(String name) throws NameNotFoundException {
	String v = find(name);
	String[] list = BootRC2.splitByChar(v, ',');
	return list;
    }

    protected String find(String name) throws NameNotFoundException {
	for(int i=0; i<pairs.size(); i++) {
	    Pair p = (Pair) pairs.elementAt(i);
	    if (p.name.equals(name)) return p.value;
	}
	throw new NameNotFoundException("Entry not found: \""+name+"\"");
    }

}
