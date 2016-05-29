package jx.bootrc;

import jx.zero.*;
import java.util.*;

public class BootRC2 {
    final static int NEWLINE = 0x0a;
    final static int MAXLINE = 256;
    char data[] = new char[MAXLINE];

    Vector domains = new Vector();
    Hashtable components = new Hashtable();
    ReadOnlyMemory mem;
    int pos;
    int curDomainSpec=0;
    GlobalSpec global;
    static final boolean verbose = false;

    public BootRC2(ReadOnlyMemory mem) {
	this.mem = mem;
	DomainSpec domain=null;
	ComponentSpec component=null;
	Spec spec=null;
	for(;;) {
	    String line = readline();
	    if (line == null) break;
	    line = line.trim();
	    if (verbose) Debug.out.println(line);
	    if (line.length() == 0 || line.charAt(0) == '#') continue;
	    if (line.charAt(0) == '[') {
		domain = null;
		component = null;
		if (line.equals("[Global]")) {
		    global = new GlobalSpec();
		    spec = global;
		} else if (line.startsWith("[Domain")) {
		    domain = new DomainSpec();
		    spec = domain;
		    domains.addElement(domain);
		} else if (line.startsWith("[Component ")) {
		    String name = (line.substring(10, line.length()-1)).trim();
		    if (verbose) Debug.out.println("Component: \""+name+"\"");
		    component = new ComponentSpec(name);
		    spec = component;
		    components.put(name, component);
		} else {
		    throw new Error("Syntax error");
		}
		continue;
	    }
	    String[] pair = splitByChar(line, '=');
	    Pair p = new Pair();
	    p.name = pair[0].trim();
	    if (verbose) Debug.out.println("Entry: \""+p.name+"\"");
	    p.value = pair[1].trim();
	    if (spec != null) spec.pairs.addElement(p);
	}
	// link component specs to domains 
	for(int i=0; i<domains.size(); i++) {
	    DomainSpec d = (DomainSpec)domains.elementAt(i);
	    try {
		String[] c = d.getStringArray("Components");
		ComponentSpec comps[] = new ComponentSpec[c.length];
		for(int j=0; j<c.length; j++) {
		    comps[j] = (ComponentSpec) components.get(c[j]);
		    if (comps[j] == null) {
			throw new Error("ComponentSpec \""+c[j]+"\" not found.");
		    }
		}
		d.setComponents(comps);
	    } catch(NameNotFoundException e) {
		Debug.out.println("No components specified for domain");
	    }
	} 
    }

    public GlobalSpec getGlobalSpec() {
	return global;
    }

    public DomainSpec nextDomainSpec() {
	if (curDomainSpec>=domains.size()) return null;
	return (DomainSpec) domains.elementAt(curDomainSpec++);
    }

    static String[] splitByChar(String stringToParse, char separator) {
	boolean exit = false;
	Vector v = new Vector();
	while(! exit){
	    int c3 = stringToParse.indexOf(separator);
	    String s;
	    if (c3==-1) { 
		exit = true;
		s = stringToParse;
	    } else {
		s = stringToParse.substring(0, c3);
		stringToParse = stringToParse.substring(c3+1).trim();
	    }
	    v.addElement(s);
	}
	String ret[] = new String[v.size()];
	v.copyInto(ret);
	return ret;
    }

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
    

