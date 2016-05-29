package jx.zero.env;

import jx.zero.*;

public class Monitor {
    static final public void enter(Object obj) {
	throw new Error("monitorenter called");
    }
    static final public void exit(Object obj) {
	throw new Error("monitorexit called");
    }
}
