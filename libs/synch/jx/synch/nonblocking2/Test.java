package jx.synch.nonblocking2;

import jx.zero.*;
import jx.zero.debug.*;
import java.util.*;


public class Test {
    static class ID {
	int id,value;
	ID(int a, int b) {
	    this.id = a; this.value = b;
	}
    }
    public static void init (final Naming naming /*String []args*/) {
	Debug.out = new DebugPrintStream(new DebugOutputStream((DebugChannel) naming.lookup("DebugChannel0")));
        CPUManager cpuManager = (CPUManager)naming.lookup("CPUManager");
	
	final Queue q = new MPSCQueue();
	
	
	new Thread() {
		public void run() { consume(q); }
	    }.start();


	new Thread() {
		public void run() { produce(1, q); }
	    }.start();


	produce(2, q);
    }

    public static void produce(final int id, Queue q) {
	int current = 0;
	for(;;) {
	    ID i = new ID(id, current);
	    q.enqueue(i);
	    current++;
	}
    }    
    
    public static void consume(Queue q) {
	for(;;) {
	    try {
		ID i = (ID) q.dequeue();
		//  if (i.id() != current) throw new Error("ERROR IN QUEUE / WRONG ELEMENT");
		Debug.out.print("*");
	    } catch(QueueEmptyException e) {} // ignore and retry
	}
    }    
    
}
