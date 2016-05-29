package jx.zero;

import java.io.InputStream;
import java.io.OutputStream;
import jx.zero.debug.*;
import jx.zero.debug.DebugPrintStream;
import jx.zero.debug.DebugOutputStream;

/**
 * Compile with -debug to activate message and assert
 *
 * see jx/compiler/plugin/Debug.java 
 *
 */

public class Debug {
    public static final boolean debug = false;
    public static InputStream in;
    public static DebugPrintStream out;
    public static DebugPrintStream err;
        
    public static final void throwError(String message) {
	message(message);
	throw new Error("ThrowError called :" + message); 
    }    
    
    public static final void throwError() {
	throw new Error("ThrowError called :"); 
    }

    public static final void message(String message) {
	if (debug) {
	    if (Debug.out!=null) {
		Debug.out.println(message);
	    } else {
		Naming dz = InitialNaming.getInitialNaming();
		DebugChannel d = (DebugChannel) dz.lookup("DebugChannel0");
		Debug.out = new DebugPrintStream(new DebugOutputStream(d));
		Debug.out.println(message);
	    }
	}
    }

    public static final void verbose(String message) {
	message(message);
    }

    public static final void check(boolean condition, String message) {
	assert(condition, message);
    }

    public final static void assert(boolean condition) {
	assert(condition,""); 
    }

    public static final void assert(boolean condition, String message) {
	if (debug && !condition) {
	    message("Assertion Failed :"+message);
	    throw new Error("Assertion Failed :"+message);
	}
    }
    //private static native final void printStackTrace();
}
