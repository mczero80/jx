package jx.compspec;

import jx.zero.Debug;

import java.io.*;
import java.util.Vector;


public class Cleaner {

    private static final boolean doDebug = true;
    
    static MetaInfo metas[] = new MetaInfo[200];
    static int nmetas=0;
    
    public static void main(String[] args) throws Exception {
	/*
	jx.emulation.Init.init();	
	
	System.out.println("JX Cleaner");
	
	if (args.length < 2) {
	    System.out.println("Parameters:");
	    System.out.println("     COMPONENTS file");
	    System.out.println("     components directory");
	    return;
	}
	
	
	
	Parser p = new Parser(args[0]);
	for(int i=0; ; i++) {
	    String l = p.nextComponent();
	    if (l==null) break;
	    MetaInfo s = new MetaInfo(args[1]+"/"+l);
	    //	    s.dump();
	    // compute transitive needlibs
	    String v = s.getVar("CLASSFILES");
	    String[] c = MetaInfo.split(v);
	    if (c != null) {
		for(int j=0;j<c.length;j++) {
		    String lib = c[j];
		    Debug.out.println(lib);
		}
	    }
	}
	
	
	*/	
	
	
    }
}

