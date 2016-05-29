package jx.compspec;

import java.io.*;
import java.util.Vector;


class NewParser {
    final static int MAXLINE = 256;
    final static int NEWLINE = 0x0a;
    final static int QUOTATION = 0x22;
    final static int COMMA = 0x2c;
    final static int SPACE = 0x20;
    final static int SEPARATOR = COMMA;
    final static int NUM_ZERO = 0x30;
    final static int NUM_NINE = 0x39;
    final static int COMMENT = 0x23;
    final static int LPAREN = 0x5b;
    final static int RPAREN = 0x5d;
    int pos;
    int linenumber=1;
    FileInputStream in ;
    
    NewParser(String filename) throws IOException {
	in = new FileInputStream(filename);
    }
    
    String[][] getComponents() {
	try {
	    Vector v = new Vector();
	    for(int i=0; ; i++) {
		String l = readLine();
		if (l==null) break;
		l = l.trim();
		if (l.length() == 0 || l.charAt(0)=='#') { i--; continue; }
		String[] l1 = splitByChar(l, ':');
		v.addElement(l1);
	    }
	    String[][] s = new String[v.size()][];
	    for(int i=0; i<v.size(); i++) {
		s[i] = (String[])v.elementAt(i);
	    }
	    return s;
	}catch(Exception e){throw new Error(e.toString());}
    }
    
    private String readLine()  throws IOException {
	linenumber++;
	return readLine(in);
    }
    public static String readLine(InputStream in)  throws IOException {
	int bi;
	int i=0;
	char data[] = new char[MAXLINE];
	for(;;) {
	    bi = in.read();
	    if (bi == NEWLINE) 	break; // end of line
	    if (bi == -1) break; // end of file
	    data[i] = (char)bi;
	    i++;
	}
	if (i==0) {
	    if (bi == -1) return null;
	    else return "";
	}
	return new String(data, 0, i);
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

}
