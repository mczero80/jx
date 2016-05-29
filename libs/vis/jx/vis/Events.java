package jx.vis;

import java.util.*;
import java.io.*;
import jx.formats.*;

public class Events {

    EventInfo[] all;
    EventType[] types;
    int nevents;

    Events(InputStream input, boolean verbose) throws IOException {
	// read data
	LittleEndianInputStream in = new LittleEndianInputStream(new BufferedInputStream(input));

	int i=0;
	int neventtypes=in.readInt();
	types = new EventType[neventtypes];
	for(i=0;i<neventtypes;i++) {
	    String name = in.readString2ByteAligned();
	    if (verbose) System.out.println("EVENT "+i+": "+name);
	    types[i] = new EventType();
	    types[i].name = name;
	    types[i].number = i+1;
	}
	nevents=in.readInt();
	all = new EventInfo[nevents];
	try {
	    for(i=0;i<nevents;i++) {
		EventInfo e = new EventInfo();
		e.timestamp=in.readLong();
		e.number=in.readInt();
		e.info1=in.readInt();
		e.info2=in.readInt();
		e.type = types[e.number-1];
		all[i] = e;
		if (verbose) {
		    System.out.println(e.timestamp+" "+e.number+ " " + e.info1);
		}
	    }
	} catch(java.io.EOFException ex) {
	    //System.out.println("Warning: Incomplete read. Got "+(i-1)+" from "+nevents);
	    nevents = i-1;
	}
    }    
}
