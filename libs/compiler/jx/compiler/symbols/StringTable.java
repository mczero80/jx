package jx.compiler.symbols;

import jx.zero.Debug;

import jx.compiler.*;
import jx.compiler.execenv.*;
import jx.compiler.symbols.*;

import java.util.*;
import java.io.IOException;


public class StringTable {

    Vector strings;

    public StringTable() {
	strings = new Vector();
	//strings.addElement("STRING-TABLE:");
	strings.addElement("java/lang/Object");
    }

    public void register(String str) {
	getIdentifier(str);
    }

    public int getIdentifier(String str) {
	int i;
	for (i=0;i<strings.size();i++) {
	    String e = (String)strings.elementAt(i);
	    if (e.equals(str)) return i;
	}
	strings.addElement(str);
	return i;
    }

    public String getString(int id) {
	return (String)strings.elementAt(id);
    }
    
    public void writeStringTable(ExtendedDataOutputStream out) throws IOException {
	int number = strings.size();
	//out.writeInt(number+1);
	out.writeInt(number);
	for (int i=0;i<number;i++) {
	    String str = (String)strings.elementAt(i);
	    /*
	    if (str.equals("STRING-TABLE:")) {
		out.writeString("STRING-TABLE: size = "+number);
		continue;
	    } else {
	    */
	    out.writeString((String)strings.elementAt(i));
	}
	//out.writeString("END OF STRING-TABLE");
    }

    public void writeStringID(ExtendedDataOutputStream out, String str) throws IOException {
	int number = strings.size();
	for (int i=0;i<number;i++) {
	    String e = (String)strings.elementAt(i);
	    if (e.equals(str)) {
		out.writeInt(i);
		return;
	    }
	}
	throw new Error("invalid String ID");
    }
}
