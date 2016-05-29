package jx.verifier.wcet;

import java.util.Vector;
import java.util.Enumeration;

//holds a List of all Parent edges of an edge
public class CFGParentList {
    private Vector list = new Vector();
    public boolean addParentEdge(CFGEdge newEdge) {
	for (Enumeration e = list.elements(); e.hasMoreElements();) {
	    if (e.nextElement() == newEdge) 
		return false;
	}
	list.addElement(newEdge);
	return true;
    }
    public void markWithIf(int ifAddr) {
	for (Enumeration e = list.elements(); e.hasMoreElements();) {
	    ((CFGEdge)e.nextElement()).markWithIf(ifAddr);
	}
    }
}
