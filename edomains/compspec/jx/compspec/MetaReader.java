package jx.compspec;

import jx.zero.Debug;
import jx.zero.*;

import java.io.*;
import java.util.Vector;


class MetaReader {
    String[] compdirs;

    MetaReader(String[] compdirs) {
	this.compdirs = new String[compdirs.length];
	for(int i=0; i<compdirs.length; i++) {
	    this.compdirs[i] = compdirs[i].trim();
	}
	
    }

    void addMeta(Vector v, String l) throws IOException {
	for(int i=0; i<compdirs.length; i++) {	
	    String filename = compdirs[i]+"/"+l.trim();
	    RandomAccessFile file;
	    try {
		file = new RandomAccessFile(filename+"/META", "r");
            } catch (Exception ex) {
		continue;
	    } 
	    byte [] data = new byte[(int)file.length()];
	    file.readFully(data);
	    MetaInfo s = new MetaInfo(filename, data);
	    v.addElement(s);
	    return;
	}
    }
}
