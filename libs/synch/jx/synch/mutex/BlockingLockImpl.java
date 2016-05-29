package jx.synch.mutex;

import jx.zero.*;

public class BlockingLockImpl implements BlockingLock {
    
    class Queue {
	class Node {
	    CPUState value;
	    Node next;
	}
	
	Node head;
	Node tail;
	
	public Queue() {
	    Node node = new Node();
	    head = tail = node;
	}
	
	/* producer: add node to tail side of the queue */
	public void enqueue(CPUState value) {
	    Node node = new Node();
	    node.value = value;
	    
	    tail.next = node;
	    tail = node;
	}
	
	
	/* consumer: remove node from head side of the queue */
	public CPUState dequeue() {
	    CPUState ret;
	    Node t = head;
	    if (t == tail) 
		return null;
	    ret = t.value;
	    head = t.next;
	    return ret;
	}
    } // end Queue

    Queue waiting = new Queue();
    static CPUManager 	cpuManager = (CPUManager) InitialNaming.getInitialNaming().lookup("CPUManager");
    static Scheduler scheduler = (Scheduler) InitialNaming.getInitialNaming().lookup("Scheduler");
    CPUState owner;

    public void lock() {
	CPUState mythread = cpuManager.getCPUState();
	for(;;) {
	    scheduler.disableThreadSwitching();
	    if (owner==null) {
		owner = mythread;
		scheduler.enableThreadSwitching();
		return;
	    } else {
		waiting.enqueue(mythread);	    
		scheduler.blockAndEnableThreadSwitching();
	    }
	}
    }

    public void unlock() {
	CPUState mythread = cpuManager.getCPUState();
	scheduler.disableThreadSwitching();
	CPUState next = waiting.dequeue();
	if (next != null) {
	    owner = next;
	    cpuManager.unblock(next);
	} else {
	    owner = null;
	}
	scheduler.enableThreadSwitching();
    }
    
}
