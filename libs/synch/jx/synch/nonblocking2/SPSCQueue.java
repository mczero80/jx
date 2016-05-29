package jx.synch.nonblocking2;

import jx.zero.*;


/** Single Producer, Single Consumer */
public class SPSCQueue implements Queue {
    class Node {
	Object value;
	Node next;
    }

    Node head;
    Node tail;

    public SPSCQueue() {
	Node node = new Node();
	head = tail = node;
    }

    /* producer: add node to tail side of the queue */
    public void enqueue(Object value) {
	Node node = new Node();
	node.value = value;
	
	tail.next = node;
	tail = node;
    }

    
    /* consumer: remove node from head side of the queue */
    public Object dequeue() throws QueueEmptyException {
	Object ret;
	Node t = head;
	if (t == tail) 
	    throw new QueueEmptyException();
	ret = t.value;
	head = t.next;
	return ret;
    }
}
