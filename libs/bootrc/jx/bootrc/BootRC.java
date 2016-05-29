package jx.bootrc;

import jx.zero.*;

public class BootRC {
    final static int MAXLINE = 256;
    final static int NEWLINE = 0x0a;
    final static int QUOTATION = 0x22;
    final static int COMMA = 0x2c;
    final static int SPACE = 0x20;
    final static int KILO  = (int)'k';
    final static int MEGA  = (int)'M';
    final static int SEPARATOR = COMMA;
    final static int NUM_ZERO = 0x30;
    final static int NUM_NINE = 0x39;
    final static int COMMENT = 0x23;
    final static int LPAREN = 0x5b;
    final static int RPAREN = 0x5d;
    char data[] = new char[MAXLINE];
    ReadOnlyMemory mem;
    int pos;
    int linenumber=1;

    static final boolean debug = false;

    // ****

    String startConfig;
    Section firstSection=null;

    public BootRC(ReadOnlyMemory mem) {
	this.mem = mem;
	parse();
    }
    String nextLine() {
	byte b;
	int i=0;
	while((b = mem.get8(pos)) != NEWLINE) {
	    data[i] = (char)b;
	    pos++;
	    i++;
	} 
	pos++;
	linenumber++;
	String s = new String(data, 0, i);
	return s;
    }

    String readInParen() {
	byte b = mem.get8(pos);
	if (b != LPAREN) throw new Error("readInParen (b != LPAREN)"); 
	int i=0;
	while((b = mem.get8(pos)) != RPAREN) {
	    data[i] = (char)b;
	    pos++;
	    i++;
	} 
	pos++;
	return new String(data, 1, i-1);
    }
    Section nextSection() {
	if (! gotoNextSection()) return null;

	    
	Section s = new Section();
	s.name = readInParen();

	//Debug.out.println("NAME:"+s.name);
	if (! haveMoreTokens()) return null;
	upToNextToken();
	for(;;) {
	    Record r = nextRecord();
	    if (r == null) return s;
	    s.add(r);
	}
    }

    Record nextRecord() {
	if (! gotoFirstTokenInLine()) return null;
	if (! haveMoreTokens()) return null;
	try {
	    Record r = new Record();
	    r.domainName = nextStringToken();
	    upToNextToken();
	    r.mainLib = nextStringToken();
	    upToNextToken();
	    r.startClass = nextStringToken();
	    upToNextToken();
	    try {
		while((mem.get8(pos)) == SPACE) pos++;
		if (mem.get8(pos) == QUOTATION) {
		    r.schedulerClass = nextStringToken();
		    upToNextToken();
		} else {
		    r.schedulerClass = null; 
		}
	    }catch(StringTokenException e) { r.schedulerClass = null; }
	    r.heapSize = nextIntToken();
	    String[] args = new String[100];
	    int argc=0;
	    while (upToNextToken()) {
		args[argc] = nextStringOrNullToken();
		argc++;
	    }
	    r.argv = new String[argc];
	    for(int i=0; i<argc;i++) {
		r.argv[i] = args[i];
	    }
	    return r;
	} catch(Exception e) {
	    Debug.out.println("Exception:"+e);
	    e.printStackTrace();
	    throw new Error(); 
	}

    }

    String nextStringOrNullToken() throws StringTokenException {
	byte b;
	int i=0;

	while((b = mem.get8(pos)) == SPACE) pos++;

	if ((b = mem.get8(pos)) != QUOTATION) {
	    if (b == 'n') {
		pos++;
		if ((b = mem.get8(pos++)) != 'u') throw new Error("no u");
		if ((b = mem.get8(pos++)) != 'l') throw new Error("no l");
		if ((b = mem.get8(pos++)) != 'l') throw new Error("no l");
		return null;
	    }
	    Debug.out.println("Error parsing line "+ linenumber);
	    throw new StringTokenException();
	}
	pos++;
	
	while((b = mem.get8(pos)) != QUOTATION) {
	    data[i] = (char)b;
	    pos++;
	    i++;
	} 
	pos++;

	String s = new String(data, 0, i);


	//Debug.out.println("TOK:"+s);
	return s;
    }

    String nextStringToken() throws StringTokenException {
	byte b;
	int i=0;

	while((b = mem.get8(pos)) == SPACE) pos++;

	if ((b = mem.get8(pos)) != QUOTATION) {
	    Debug.out.println("Error parsing line "+ linenumber);
	    throw new StringTokenException();
	}
	pos++;
	
	while((b = mem.get8(pos)) != QUOTATION) {
	    data[i] = (char)b;
	    pos++;
	    i++;
	} 
	pos++;

	String s = new String(data, 0, i);


	//Debug.out.println("TOK:"+s);
	return s;
    }

    int nextIntToken() throws NumberFormatException {
	byte b;
	int i=0;

	while((b = mem.get8(pos)) == SPACE) pos++;
	
        for(;;) {
           b = mem.get8(pos);
           if (b==SPACE) {pos++;continue;}
           if (b==MEGA) {
              pos++;
              data[i++]='0';
              data[i++]='0';
              data[i++]='0';
              data[i++]='0';
              data[i++]='0';
              data[i++]='0';
              continue;
            }
            if (b==KILO)  {
              pos++;
              data[i++]='0';
              data[i++]='0';
              data[i++]='0';
              continue;
            }
	    if (b < NUM_ZERO || b > NUM_NINE) break;
	    data[i] = (char)b;
	    pos++;
	    i++;
	} 

	String s = new String(data, 0, i);
	return Integer.parseInt(s, 10);
    }

    String selectedConfig() {
	if (! gotoNextSection()) return null;
	return readInParen();
    }

    boolean haveMoreTokens() {
	if (pos >= mem.size()) return false;
	byte b = mem.get8(pos);
	if (b == LPAREN) return false;
	return true;
    }
    /** goto first token; call to skip garbage between section name and contents */
    void gotoFirstToken() {
	byte b;
	while(pos < mem.size()) {
	    b = mem.get8(pos);
	    if (b == LPAREN) throw new Error("empty section not allowed");
	    if (b != COMMENT && b != NEWLINE) return;
	    while(pos < mem.size()) {
		b = mem.get8(pos);
		if (b == NEWLINE) {
		    linenumber++;
		    break;
		}
		pos++;
	    }
	    pos++;
	}

    }

    boolean gotoFirstTokenInLine() {
	byte b;
	while(pos < mem.size()) {
	    b = mem.get8(pos);
	    if (b == LPAREN) return false;
	    if (b != COMMENT && b != NEWLINE) return true;
	    while(pos < mem.size()) {
		b = mem.get8(pos);
		if (b == NEWLINE) {
		    linenumber++;
		    break;
		}
		pos++;
	    }
	    pos++;
	}
	return false;

    }

    boolean gotoNextSection() {
	byte b;
	while(pos < mem.size()) {
	    b = mem.get8(pos);
	    if (b == LPAREN) return true;
	    while(pos < mem.size()) {
		b = mem.get8(pos);
		if (b == NEWLINE) {
		    linenumber++;
		    break;
		}
		pos++;
	    }
	    pos++;
	}
	return false;

    }

    /** @return true=more tokens in this line; false=no more tokens */
    boolean upToNextToken() {
	byte b;
	while(pos < mem.size()) {
	    b = mem.get8(pos);
	    if (b == SEPARATOR) { pos++; return true;}
	    if (b == NEWLINE) { pos++;linenumber++; return false;}
	    pos++;
	}
	return false;
    }

    // ***********************************************

    private void parse() {
	Section s;
	Record r;
	Section lastSection=null;

	startConfig = selectedConfig();

	if (debug) Debug.out.println("     configuration: "+startConfig);
	while ((s = nextSection()) != null) {
	    if (firstSection == null) {
		firstSection = s;
		lastSection = s;
	    } else {
		lastSection.next = s;
		lastSection = s;
	    }
	    if (debug) {
		 Debug.out.println("**** Section: "+s.name);
		 while ((r = s.nextRecord()) != null) {
		      Debug.out.println("-----");
		      Debug.out.println("DomainName: "+r.domainName);
		      Debug.out.println("MainLib: "+r.mainLib);
		      Debug.out.println("StartClass: "+r.startClass);
		      Debug.out.println("SchedulerClass: "+r.schedulerClass);
		      Debug.out.println("HeapSize: "+r.heapSize);
		 }
	    }
	}


    }

    // ***********************************************

    public String getSelectedName() {
	return startConfig;
    }

    public Section getSelectedSection() {
	Section s;
	for(s=firstSection; s != null; s=s.next) {
	    if (s.name.equals(startConfig)) {
		return s;
	    }
	}
	throw new Error("selected config not found");
    }
}
    

