package jx.synch.blocking;

import jx.zero.*;
import jx.zero.debug.*;

public class Test {
    public static void init (final Naming naming /*String []args*/) {
	Debug.out = new DebugPrintStream(new DebugOutputStream((DebugChannel) naming.lookup("DebugChannel0")));
        CPUManager cpuManager = (CPUManager)naming.lookup("CPUManager");
	
	final Queue q = new Queue(cpuManager.getCPUState());
	
	
	new Thread() {
		public void run() { consume(q); }
	    }.start();


	produce(q);
    }

    public static void produce(Queue q) {
	int current = 0;
	for(;;) {
	    Integer i = new Integer(current);
	    q.enqueue(i);
	    current++;
	}
    }    
    
    public static void consume(Queue q) {
	int current = 0;
	for(;;) {
	    Integer i = (Integer) q.dequeue();
	    if (i.intValue() != current) throw new Error("ERROR IN QUEUE / WRONG ELEMENT");
	    current++;
	    Debug.out.print("*");
	}
    }    
    
}
