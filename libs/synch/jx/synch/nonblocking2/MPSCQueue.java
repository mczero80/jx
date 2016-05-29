package jx.synch.nonblocking2;

import jx.zero.*;


/** Multiple Producer, Single Consumer */
public class MPSCQueue extends SPSCQueue {

    CAS cas_next, cas_tail;
    CPUManager 	cpuManager = (CPUManager) InitialNaming.getInitialNaming().lookup("CPUManager");

    MPSCQueue() {
	cas_next = cpuManager.getCAS("jx/synch/nonblocking2/SPSCQueue$Node", "next");
	cas_tail = cpuManager.getCAS("jx/synch/nonblocking2/SPSCQueue", "tail");
    }

    /* producer: add node to tail side of the queue */
    public void enqueue(Object value) {
	Node node = new Node();
	node.value = value;
	
	Node tmp_tail;
	for(;;) {
	    tmp_tail = tail;
	    Node tmp_next = tmp_tail.next;
	    if (tmp_tail == tail) {
		if (tmp_next == null) {
		    if (cas_next.casObject(tmp_tail, tmp_next, node)) {
			break;
		    }
		} else {
		    cas_tail.casObject(this, tmp_tail,  tmp_next);
		}
	    }
	}
	cas_tail.casObject(this, tmp_tail,  node);
    }
}
