package jx.emulation;

import jx.zero.debug.DebugChannel;
import jx.zero.Debug;
import jx.zero.debug.DebugPrintStream;
import jx.zero.debug.DebugOutputStream;
import jx.zero.debug.DebugInputStream;

import java.lang.reflect.*;
import java.lang.reflect.Method;

public class Init {
    public static NamingImpl naming;
    public static void init() {
	naming = new NamingImpl();
	DebugChannel c = new DebugChannelImpl();
	Debug.out = new DebugPrintStream(new DebugOutputStream(c));
	Debug.err = new DebugPrintStream(new DebugOutputStream(c));
	Debug.in = new DebugInputStream(c);
    }
    public static void main(String[] args) throws Exception {
	if (args.length == 0) throw new Error("Need Classname");
	init();
	Class cl = Class.forName(args[0]);
	Class cx = Class.forName("[Ljava.lang.String;");
	Method method = cl.getMethod("main", new Class[] { cx });
	//System.out.println(" XX"+method);
	String a[] = new String[args.length-1];
	System.arraycopy(args, 1, a, 0, a.length);
	try {
	    method.invoke(null, new Object[] {a});
	} catch(InvocationTargetException ex) {
	    Throwable e = ex.getTargetException();
	    System.out.println("Exception: " + e);
	    e.printStackTrace();
	}
    }
}
