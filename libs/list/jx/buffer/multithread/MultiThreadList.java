package jx.buffer.multithread;

import jx.zero.*;

/**
   a sigle linked-list implementation
   uses CAS to achieve threadsafety (Hopefully)
   uses an AtomicVariable in ListMode to block (consumer) threads
*/
public class MultiThreadList {
//    static final boolean debug = false;

    boolean verbose = false;
    MultiThreadListElement first;
    MultiThreadListElement last;
    CAS cas_first, cas_last;
    CPUState consumer = null;
    CPUManager cpuManager;

    int size;

    String name = null; // for debugging


    public MultiThreadList() {
	if (verbose) Debug.out.println("MultiThreadList()");
	cpuManager = (CPUManager)LookupHelper.waitUntilPortalAvailable(InitialNaming.getInitialNaming(),"CPUManager");
	first = new MultiThreadListElement(null, cpuManager);
	last = first;
	cas_first = cpuManager.getCAS(this.getClass().getName(), "first");
	cas_last  = cpuManager.getCAS(this.getClass().getName(), "last");
	size = 1;
    }

    /** for debugging */
    public void setListName(String name) {
	this.name = name;
    }

    public void appendElement(Object o) {
	if (verbose) Debug.out.println("appendElement ["+name+"] data="+o+" (size="+size+")");
	MultiThreadListElement elm = new MultiThreadListElement(o, cpuManager);
	elm.next.set(null);
	MultiThreadListElement last_bak;
	do {   // set last to elm and get a valid copy of last 
	    last_bak = last;
	} while (!cas_last.casObject(this, last_bak, elm));
	last_bak.next.atomicUpdateUnblock(elm, consumer);
	size++;
    }
    
    
    /** returns the first Element in the List
     *  if the list is empty this method blocks
     *  the return value may be <code>null</code>!!! if unblocked from another Thread or another Thread steals the only element 
     *  @return    <code>null</code> if unblocked or interferences with other Threads.<BR>
     *  else first Object
     */
    public Object undockFirstElement() {	
	if (verbose) Debug.out.println("undockFirstElement ["+name+"] (size="+size+")");

	/* in liste einhaengen */
	consumer = cpuManager.getCPUState();
	first.next.blockIfEqual(null);
	consumer = null;
	/* aus liste austragen */
	
	MultiThreadListElement first_bak, first_bak_next;
	do {   // set last to elm and get a valid copy of last 
	    first_bak = first;
	    first_bak_next = (MultiThreadListElement)first_bak.next.get();
	    if (first_bak_next == null) return null;
	} while (!cas_first.casObject(this, first_bak, first_bak_next));

	size--;
	return first_bak_next.data;
    }

   /** returns the first Element in the List
     *  if the list is empty this method returns <code>null</code>
     *  @return    <code>null</code> if the list is empty<BR>
     *  else first Object
     */
     public Object nonblockingUndockFirstElement() {	
	if (verbose) Debug.out.println("nonblockingUndockFirstElement ["+name+"] (size="+size+")");


	MultiThreadListElement first_bak, first_bak_next;
	do {   // set last to elm and get a valid copy of last 
	    first_bak = first;
	    first_bak_next = (MultiThreadListElement)first_bak.next.get();
	    if (first_bak_next == null) return null;
	} while (!cas_first.casObject(this, first_bak, first_bak_next));



	size--;
	return first_bak_next.data;
    }

    public Object peekFirstElement() {	
	if (verbose) Debug.out.println("peekFirstElement ["+name+"]");

	first.next.blockIfEqual(null);
	
	if (first.next.get() == null) {
	    Debug.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
	    Debug.out.println("unblocked, but no element in list!!. This  may be an error!");
	    Debug.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
	    return null; // erlaubt einen Unblock von aussen
	}
	return first.data;
    }

    public void dump() {
	MultiThreadListElement b = first;
	cpuManager.dump("Dump of list: ", this);
	while(b!=null) {
	    cpuManager.dump(" Node: ", b);
	    cpuManager.dump(" Data: ", b.data);
	    b = (MultiThreadListElement) b.next.get();
	}
    }

    public int size() { return size; }
    public void setVerbose(boolean v) { verbose = v; }
    // ************
    // helper class
    // ************
    
    class MultiThreadListElement {
	public AtomicVariable next;
	public Object data;
	
	public MultiThreadListElement(Object mem, CPUManager cpuManager) {
	    data = mem;
	    next = cpuManager.getAtomicVariable();
	    next.activateListMode();
	    next.set(null);
	}
    }
    
}
