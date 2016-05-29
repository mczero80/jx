package jx.verifier.wcet;

import java.util.Vector;
import java.util.Enumeration;
import jx.verifier.bytecode.*;

//list for all nodes
//list is sorted ascending by address (
public class CFGNodeList {
    private CFGNodeListEntry first = null;
    private CFGNodeListEntry last = null;
    private CFGNodeListEntry act = null; //holds a pointer to the last element accessed. Just to save time when accessing nodes sequentially
    private CFGNode retNode = new CFGNode(null); //target node for all returns.
    Vector newNodes = new Vector(0);
    
    public CFGNode getFirst() {return (first != null)? first.node : null;}
    //add new node with address 'address'; if such a node already exists, no new node is added
    //and the existant one is returned.
    public CFGNode addNode(ByteCode bCode) {
	if (first == null) { //list is empty!
	    first = new CFGNodeListEntry();
	    first.node= new CFGNode(bCode);
	    act = first;
	    last = first;
	    newNodes.addElement(act.node);
	    return act.node;
	}
	//find position, where node should be put into
	if (act == null) act = first;
	while (bCode.getAddress() > act.node.getAddress() && act.next != null) {
	    act = act.next;
	}
	while (bCode.getAddress() < act.node.getAddress() && act.prev != null) {
	    act = act.prev;
	}
	//at this point, act.node has either address 'address' or it is has the next lower 
	//address in the list --> if not equal, add after act Element.
	if (bCode.getAddress() == act.node.getAddress()) return act.node;
	else {
	    CFGNodeListEntry tmp = new CFGNodeListEntry();
	    tmp.prev = act;
	    tmp.next = act.next;
	    act.next = tmp;
	    if (tmp.next != null) tmp.next.prev = tmp;
	    tmp.node = new CFGNode(bCode);
	    if (act.next == null) last = tmp;
	    newNodes.addElement(tmp.node);
	    return tmp.node;
	}
    }

    //add new node with address 'address'; if such a node already exists, another entry is 
    //added!!!
    public CFGNode addNode(CFGNode newNode) {
	if (first == null) { //list is empty!
	    first = new CFGNodeListEntry();
	    first.node= newNode;
	    act = first;
	    last = first;
	    newNodes.addElement(act.node);
	    return act.node;
	}
	//find position, where node should be put into
	if (act == null) act = first;
	while (newNode.getAddress() > act.node.getAddress() && act.next != null) {
	    act = act.next;
	}
	while (newNode.getAddress() < act.node.getAddress() && act.prev != null) {
	    act = act.prev;
	}
	
	CFGNodeListEntry tmp = new CFGNodeListEntry();
	tmp.prev = act;
	tmp.next = act.next;
	act.next = tmp;
	if (tmp.next != null) tmp.next.prev = tmp;
	tmp.node = newNode;
	if (act.next == null) last = tmp;
	newNodes.addElement(tmp.node);
	return tmp.node;

    }


    //remove 'rmNode' from list;
    //the node is searched by comparing the references!
    //returns true, if rmNode was found (and removed), false else.
    public boolean  removeNode(CFGNode rmNode) {
	if (first == null || rmNode == null) return false;
	if (act == null) act = first;
	
	while(act != null && act.node.getAddress() > rmNode.getAddress()) 
	    act = act.prev;
	while(act != null && act.node.getAddress() < rmNode.getAddress())
	    act = act.next;
	if (act == null) act = first;
	if (act.node != rmNode) 
	    return false;
	//found it, remove it
	removeAct();
	return true;

    }
    
    //remove Node pointed to by act
    private void removeAct() {
	if (act.prev == null) first = act.next;
	else act.prev.next = act.next;
	if (act.next == null) last = act.prev;
	else act.next.prev = act.prev;
	
    }

    public CFGNode getRetNode() {
	return retNode;
    }


    public Enumeration getElements() {
	return new CFGNodeEnum(first);
    }

    public String toString() {
	Object act = null;
	String ret = "";
	Enumeration e = getElements(); 
	while (e.hasMoreElements()){
	    act = e.nextElement();
	    ret += act.toString() + "\n";
	}
	return ret;
    }
}

class CFGNodeListEntry {
    public CFGNodeListEntry next;
    public CFGNodeListEntry prev;
    public CFGNode node;
}

class CFGNodeEnum implements Enumeration {
    private CFGNodeListEntry act;
    public boolean hasMoreElements() {
	return (act !=null);
    }
    public Object nextElement() {
	Object ret = act.node;
	act = act.next;
	return ret;
    }
    public CFGNodeEnum(CFGNodeListEntry first) {
	act = first;
    }
}
