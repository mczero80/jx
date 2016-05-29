package jx.scheduler;

import jx.zero.*;
import jx.zero.debug.*;

public class LinkedList {
    final private CPUManager cpuManager = (CPUManager)InitialNaming.getInitialNaming().lookup("CPUManager");
    private ObjectContainer first=null;
    private ObjectContainer last =null;
    private ObjectContainer enumerator =null;
    private int anzElements = 0;
    
    private boolean debug=false;   //test
    
    public LinkedList(){
	/* init Debug.out */
//	DebugChannel d = (DebugChannel) domainZero.lookup("DebugChannel0");
//	Debug.out = new DebugPrintStream(new DebugOutputStream(d));
    }
    
    public int size()  {
	return anzElements;
    }
    
    public boolean add(Object newObj)  {
	ObjectContainer c;
	
	if (newObj == null)
	    return false;
	
	for (c = first; c != null; c=c.next) 
	    if (c.content == newObj) {
		Debug.out.print("LinkedList.add: Object\n   ");
		cpuManager.dump("",newObj);
		Debug.out.println(" already in list.");
		return false;
	    }
	
	if (last == null) {
	    last = first = getNewContainer(newObj);
	} else {
	    last.next = getNewContainer(newObj);
	    last = last.next;
	} 
	anzElements++;
	
	return true; 
    }
    
    public boolean remove(Object obj)  {
	if (first == null) {
	    if (debug)
		Debug.out.println("remove: queue is empty.");
	    return false;
	}
	if (first == obj) {
	    removeFirst();
	    return true;
	}
	ObjectContainer pre=first;
	ObjectContainer c = first.next;
	while (c != null) {
	    if (c.content == obj) {
		pre.next=c.next;
		if (c == last)
		    last = pre;
		releaseContainer(c);
		anzElements--;
		return true;
	    }
	    pre = c;
	    c = c.next;
	}
	if (debug) {
		Debug.out.println("LinkedList.remove: Object\n   ");
		cpuManager.dump("",obj);
		Debug.out.println("    not in list.");
	}
	return false;
    }
    
    public Object removeFirst()  {
	ObjectContainer c;
	Object o;
	
	if (first == null)    /* Q empty*/
	    return null;
	
	c = first;
	first = first.next;
	if (first == null)   /* runQ empty now*/
	    last = null;
	
	o = c.content;
	releaseContainer(c);
	anzElements --;
	return o;
    }

    boolean isEmpty(){
	return (size()==0);
    }

    boolean contains(Object o){
	if (indexOf(o) == -1)
	    return false;
	else 
	    return true;
    }
    
    int indexOf(Object o) {
	int i = 0;
	for ( ObjectContainer c = first; c != null; c=c.next) {
	    if (c.content == o)
		return i;
	    i++;
	}
	return -1;
    }

    Object get(int index){
	int i = 0;
	for ( ObjectContainer c = first; c != null; c=c.next) {
	    if (i++ == index)
		return c.content;
	}
	return null;
    }
 
    Object remove(int index){
	Object backup = get(index);
	remove(backup);
	return backup;
    }
   
    public void dump() {
	if (getFirst() == null)
	    Debug.out.println("       none");
	else {
	    Debug.out.println("       "+size()+" elements");
	    for ( ObjectContainer c = first; c != null; c=c.next) {
		cpuManager.dump("       ",c.content);
	    }
	}
    }

    /************ protected *******************/
    protected Object getFirst()
    {
	enumerator = first;
	
	if (enumerator == null)
	    return null;
	else
	    return enumerator.content;
    }
    protected Object getNext()
    {
	if (enumerator != null)
	    enumerator = enumerator.next;
	
	if (enumerator == null)
	    return null;
	else
	    return enumerator.content;
    }
    
    /********************** Memory-Management ****************************/     
    private ObjectContainer ContainerPool = null;
    private int ContainerPoolSize=0;
    
    private ObjectContainer getNewContainer(Object o)   {
	ObjectContainer c = ContainerPool;
	if (c == null) {
	    ContainerPoolSize++;
	    if (debug)
		Debug.out.println("LinkedList.getNewContainer: ContainerPool empty, new size: "+ContainerPoolSize);
	    return new ObjectContainer(o);
	}
	ContainerPool=ContainerPool.next;
	
	c.content = o;
	c.next = null;
	return c;
    }
    
    private void releaseContainer(ObjectContainer c)	  {
	c.next = ContainerPool;
	ContainerPool = c;
    }
    
    /******************  Helper Class *****************/
    class ObjectContainer {
	Object content;
	ObjectContainer next;
	
	public ObjectContainer(Object content)  {
	    this.content=content;
	    next = null;
	}
    } 
}



