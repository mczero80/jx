package jx.synch.blocking;

import jx.zero.*;

/**
 * see also
 *  Massalin, H. and Pu, C. Threads and Input/Output in the Synthesis Kernel. In Proceedings of the 12th ACM Symposium
 *  on Operating Systems Principles, pp. 191--201, December 1989. 
 */

class Node {
    static CPUManager 	cpuManager = (CPUManager) InitialNaming.getInitialNaming().lookup("CPUManager");
    Object value;
    AtomicVariable next = cpuManager.getAtomicVariable();
}

public class Queue {
    Node head;
    Node tail;

    CPUManager 	cpuManager = (CPUManager) InitialNaming.getInitialNaming().lookup("CPUManager");
    CPUState consumer;

    public Queue(CPUState consumer) {
	this.consumer = consumer;
	Node node = new Node();
	head = tail = node;
    }

    
    public void enqueue(Object value) {
	Node node = new Node();
	node.value = value;
	node.next.set(null);
	tail.next.atomicUpdateUnblock(node, consumer);
	tail = node;
    }

    public Object dequeue() {
	Node ret = head;
	head.next.blockIfEqual(null);
	head = (Node)head.next.get();
	return head.value;
    }
}

