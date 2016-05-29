package jx.synch.mutex;

import jx.zero.*;
import jx.zero.debug.*;
import java.util.*;


public class Test implements Runnable {
    public static void init (final Naming naming /*String []args*/) {
	Debug.out = new DebugPrintStream(new DebugOutputStream((DebugChannel) naming.lookup("DebugChannel0")));
	new Test();
    }

    int a;
    BlockingLock mutex = new BlockingLockImpl();

    Test(){
	for(int i=0; i<10; i++) {
	    new Thread(this).start();
	}
    }
    
    public void run() { 
	mutex.lock();
	for(int i=0; i<1000000; i++) a++;
	mutex.unlock();
	Debug.out.println("a="+a);
    }

}
