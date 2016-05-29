package jx.compspec;

import jx.zero.Debug;
import jx.zero.*;

import java.util.Vector;

final public class MetaInfo {

    final static int MAXLINE = 5000;
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
    char data[] = new char[MAXLINE];
    int pos;
    int linenumber=1;
    String filename;

    String [] vars = new String[20];
    String [] vals = new String[20];
    int nvars;

    String [] neededLibs;

    //ReadOnlyMemory meta;
    byte[] meta;
    int metapos=0;

    class Line {
	String var;
	int op;
	String val;
    }

    //public MetaInfo(String filename, ReadOnlyMemory meta) {
    public MetaInfo(String filename, byte[] meta) {
	this.filename = filename;
	this.meta = meta;
	//System.out.println("META: "+filename);
	lineloop:
	for(int j=0; ; j++) {
	    Line line = nextLine();
	    if (line == null) return;
	    if (line.op==1) {
		for(int i=0; i<nvars; i++) {
		    if (vars[i].equals(line.var)) {
			vals[i] += " "+line.val;
			//Debug.out.println("    ADDED VAR="+vars[i]+", VAL="+vals[i]);
			continue lineloop;
		    }
		}
		throw new Error("Var "+line.var+" not found (cannot perform += operation)");
	    }		
	    vars[nvars] = line.var;
	    vals[nvars] = line.val;
	    nvars++;
	    //Debug.out.println("    VAR="+line.var+", VAL="+line.val);
	}
    }

    private Line nextLine() {
	byte b;
	int bi;
	int i=0;
	Line line = new Line();
	while((bi = nextchar()) != '=') {
	    if (bi==-1) return null;
	    b = (byte)bi;
	    if (i==0 && b == SPACE) continue; // skip leading spaces
	    if (i==0 && b == COMMENT) {
		while((b = (byte)nextchar()) != NEWLINE);
		continue; // skip comment line 
	    }
	    if (b == NEWLINE) { i = 0; continue;} // skip this line

	    data[i++] = (char)b;
	}
	if (data[i-1] == '+') {
	    line.op = 1;
	    i--;
	}

	// remove trailing spaces
	while (i>0 && (data[i-1] == ' ' || data[i-1] == '\t')) {
	    i--;
	}

	line.var = new String(data, 0, i);

	while((b = (byte)nextchar()) == ' ');
		
	i=0;
	if (b!=NEWLINE && b!=-1) {
	    do {
		data[i] = (char)b;
		pos++;
		i++;
	    } while((b = (byte)nextchar()) != NEWLINE && b != -1 && b != COMMENT);
	}
	if (b==COMMENT) {
	    while((b = (byte)nextchar()) != NEWLINE && b!=-1);
	}
	pos++;
	linenumber++;

	// remove trailing spaces
	while (i>0 && (data[i-1] == ' ' || data[i-1] == '\t')) {
	    i--;
	}
	line.val = new String(data, 0, i);
	return line;
    }

    public void dump() {
	for(int i=0; i<nvars;i++) {
	    Debug.out.println("    VAR="+vars[i]+", VAL="+vals[i]);
	}
    }

    public String getVar(String varname) {
	for(int i=0; i<nvars;i++) {
	    if (varname.equals(vars[i])) return vals[i];
	}
	//throw new Error("Var "+varname+" not found in META file "+filename);
	return null;
    }

    public String[] getVars() {
	String [] ret = new String[nvars];
	for(int i=0; i<nvars;i++) {
	    ret[i] = vars[i];
	}
	return ret;
    }

    public void setNeededLibs(Vector v) { 
	neededLibs = new String[v.size()];
	for(int i=0; i<neededLibs.length; i++) {
	    neededLibs[i] = (String) v.elementAt(i); 
	}
    }
	
    public String[] getNeededLibs() {
	if (neededLibs==null) return null;
	return (String[])neededLibs.clone();
    }

    public String getComponentName() {
	return getVar("LIBNAME");
    }

    private void addVars(StringBuffer buf) { 
	if (neededLibs==null) {
	    neededLibs = split(getVar("NEEDLIBS"));
	}
	for(int i=0; i<nvars;i++) {
	    if (vars[i].equals("NEEDLIBS")) {
		buf.append("NEEDLIBS=\n");
		for(int j=0; j<neededLibs.length; j++) {
		    buf.append("NEEDLIBS+="+neededLibs[j]+"\n");
		}
		buf.append("JCLIBS=");
		for(int j=0; j<neededLibs.length; j++) {
		    buf.append(neededLibs[j]);
		    if (j<neededLibs.length-1) buf.append(":");
		}
		buf.append("\n");
	    } else {
		buf.append(vars[i]+"="+vals[i]+"\n");
	    }
	}
    }

    public byte [] serialize() {
	StringBuffer buf = new StringBuffer();
	addVars(buf);
	return buf.toString().getBytes();
    }

    String createMakefile() {
	StringBuffer buf = new StringBuffer();
	buf.append("# Automatically generated file.\n");
	buf.append("# DO NOT CHANGE THIS FILE.\n");
	buf.append("# Make your changes in the META file.\n");
	buf.append("# Top-level Makefile for component "+getComponentName()+"\n");
	addVars(buf);
	buf.append("include ../../GNUmakerules.lib\n");
	return buf.toString();
    }

    String writeSubDirMakefile(String dirname) {
	StringBuffer buf = new StringBuffer();
	if (dirname.equals("")) throw new Error();
	String path = "..";
	char[]d = dirname.toCharArray();
	for(int i=0;i<d.length;i++) {
	    if (d[i]=='/')
		path += "/..";
	}

	buf.append("# Automatically generated file.\n");
	buf.append("# DO NOT MODIFY!\n");
	buf.append("# Make your changes in the META file.\n");
	buf.append("# Subdir Makefile for component "+getComponentName()+", dir "+dirname+"\n");

	buf.append("# LIBRARY BASE IS AT "+path+"\n");

	buf.append("default:"+"\n");
	buf.append("\tcd "+path+"; $(MAKE)"+"\n");

	buf.append("\n");
	    
	buf.append("compile: decomp"+"\n");
	buf.append("\t@echo \"Environment:\""+"\n");
	buf.append("\t@echo \"CLASSPATH =\" $(CLASSPATH)"+"\n");
	buf.append("\t@echo \"  JAVAC_FLAGS=${JAVAC_FLAGS}\""+"\n");
	buf.append("\t@echo \"Files to compile: \""+"\n");
	buf.append("\t@if $(PERL) $(LISTNEW) *.java; then \\"+"\n");
	buf.append("\t$(JAVAC) $(JAVAC_FLAGS) `$(PERL) $(LISTNEW) *.java`; fi;  "+"\n");
	    
	buf.append("complete allzip nat:"+"\n");
	buf.append("\tcd "+path+"; $(MAKE) $@"+"\n");
	    
	buf.append("decomp: "+"\n");
	buf.append("\tsh -c 'for i in *.java.classes; do $(PERL) $(XDECOMP) $$i; done'"+"\n");
	    
	buf.append("clean: "+"\n");
	buf.append("\t-rm -f *.class *.imcode"+"\n");
	buf.append("\t-rm -f *~ "+"\n");
	    
	buf.append("rpcstubs: $(RPC_INTERFACES)"+"\n");
	buf.append("\t@$(RPCGEN) $(ZIPS) $<"+"\n");
	    
	buf.append("docs:"+"\n");
	buf.append("\tcd "+path+"; $(MAKE) docs"+"\n");

	buf.append("javadoc:"+"\n");
	buf.append("\tjavadoc -author -version -d $(JXROOT)/docs/$(LIBNAME) *.java"+"\n");

	return buf.toString();
    }

    public static String[] split(String s) {
	return split(s, ' ');
    }
    public static String[] split(String s, char splitchar) {

	char[] c = s.toCharArray();
	char[] w = new char[c.length];

	int j;
	Vector v = new Vector();
	for(int i=0; i<c.length; i++) {
	    while (i<c.length && c[i]==' ') i++; // remove  whitespace
	    if (i==c.length)break;
	    j=0;
	    while (i<c.length && c[i]!=splitchar) {
		w[j] = c[i];
		i++; 
		j++;
	    }
	    v.addElement(new String(w, 0, j));
	}

	String ret[] = new String[v.size()];
	v.copyInto(ret);

	return ret;
    }

    int nextchar() {
	if (metapos == meta.length) return -1;
	return meta[metapos++];
    }

    public String getFilename() {
	return filename;
    }
}
