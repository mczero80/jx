package jx.verifier.wcet;

import jx.verifier.bytecode.*;
import jx.verifier.*;
import java.util.Vector;
import java.util.Enumeration;

public class OpenIfList {
    private Vector list;

    //joins two lists. If one if is contained in the resulting list with both true and false
    //case, it is eliminated ("closed").
    //returns true, if join changed this list.
    private boolean join(OpenIfList other) {
	/*	boolean retval = false;
	if (list == null) {
	    list = new Vector(1); 
	    retval = true; // true because list changed from uninitialized to empty!
	}
	for (Enumeration e = other.getListEnum() ; e.hasMoreElements() ;) {
	    retval |=  this.add((OpenIfListElement)e.nextElement());
         }
	 return retval;*/
	throw new Error("Internal Error: join not implemented");
    }
    
    //returns true, if ifAddr is in list with some branches, but not with all branches.
    //e == null returns true! NOTE: e.ifBranch is ignored!
    private boolean openIf(OpenIfListElement other) {
	if (other == null) return true;
	OpenIfListElement act = null;
	int found = 0;
	int max = 1; //if no branch is found, max > found...
	for (Enumeration e = this.getListEnum() ; e.hasMoreElements() ;) {
	    act = ((OpenIfListElement)e.nextElement());
	    if (act.sameIf(other)) {
		max = act.branchCount;
		found++;
	    }
         }
	if (found > 0 && found < max) return true;
	else return false;

    }


    //add if at address ifAddr and with branch ifBranch; 
    //returns true, if add changed this list, or if the "if" was already in the list.
    private boolean add(int ifAddr, int ifBranch) {
	return add(new OpenIfListElement(ifAddr, ifBranch));
    }
    //add if at address ifAddr and with branch ifBranch and a total number of branches branchCount
    //returns true, if add changed this list, or if the "if" was already in the list.
    private boolean add(int ifAddr, int ifBranch, int branchCount) {
	return add(new OpenIfListElement(ifAddr, ifBranch, branchCount));
    }

    private boolean add(OpenIfListElement newElm) {
	//search for if
	boolean retval = false;
	if (list == null) {list = new Vector(1); retval = true;}
	for (Enumeration e = this.getListEnum() ; e.hasMoreElements() ;) {
	    OpenIfListElement actElm = (OpenIfListElement)e.nextElement();
	    if (actElm.sameIf(newElm)) {
		if (newElm.ifBranch == actElm.ifBranch)
		    return retval; //already in list --> do nothing
		else {
		    //both true and false case --> if is closed --> remove from list
		    //list.removeElement(actElm);
		    //return true;
		    //DONT REMOVE FROM LIST
		    //Just do nothing, so the new element will also be added
		    //then both are contained, which is the same as if none was in the list
		    //however, if either case is added again later, there are still both 
		    //cases in the list and not only one.
		}
	    }
	}
	//If this point is reached, the if was not yet in the list. So add it!
	list.addElement(newElm);
	return true;
    }

    //constructor
    private OpenIfList() {
	//list must be null for new iflist, so that add / join returns true, if it is the first time they are called.
	list = null;
    }
    //copy-constr.
    private OpenIfList(Vector list) {
	this.list = list;
    }

    //returns empty enum, if list is null!
    private Enumeration getListEnum() {
	if (list == null) return (new Vector(0)).elements();
	return list.elements();
    }

    //remove all entries with all cases in the list (true and false)
    private void cleanupList() {
	if (list == null) return;
	for(int i = 0;i<list.size(); i++) {
	    Vector tmpList = new Vector(1); //holds all element with the current ifAddress
	    OpenIfListElement e = (OpenIfListElement)list.elementAt(i);
	    tmpList.addElement(e);
	    for (int j=i; j < list.size(); j++) {
		OpenIfListElement e2 = (OpenIfListElement)list.elementAt(j);
		if (e.sameIf(e2) &&
		    e != e2) {
		    tmpList.addElement(e2);
		}
	    }
	    if (tmpList.size() == e.branchCount) {
		//al branches were in the list so the if is closed --> remove all entries
		for (int j= 0; j < tmpList.size(); j++) {
		    list.removeElement(tmpList.elementAt(j));
		}
		i--; //i-- because element i from 'list' has also been removed, so 
		//the new element i must be checked  again.
	    }
	}
    }

    //Mark all ifs in the list for simulation during pass 3 of WCETA
    // this method is usefull, if a bytecode is marked that is within an open if branch, 
    // as the if has to be marked as well then.
    private void markOpenIfs(VerifierInterface mv) {
	if (list == null) return;
	ByteCode bc = null;
	for (int i = 0; i < list.size(); i++) {
	    //for each if in list...
	    //if the if to be marked is an exception, throw an error, as exceptions cannot (yet)
	    //be simulated
	    //FEHLER einfach einen Error schmeissen ist auch zu wenig, eigentlich müsste
	    //wenn sowas auftritt die simulation abgebrochen werden und statt dessen lzü eingef.
	    //werden.
	    if (((OpenIfListElement)list.elementAt(i)).bcAddr < 0) {
		throw new Error("Internal Error: Cannot simulate Exceptions (Handler @ " + (-1*((OpenIfListElement)list.elementAt(i)).bcAddr) + ")");
	    }
	    //FEHLER der (MethodVerifier) Cast muss da weg!!!
	    bc = ((MethodVerifier)mv).getCode().getBCAt(((OpenIfListElement)list.elementAt(i)).bcAddr);
	    //check if already marked
	    if (((WCETState)bc.beforeState).necessary) {
		//already marked, nothing to do
		continue;
	    } else {
		//mark Bytecode and register for checking
		((WCETState)bc.beforeState).necessary = true;
		mv.checkBC(bc);
	    }
	}
    }

    public String toString() {
	StringBuffer ret = new StringBuffer(16);
	for (Enumeration e = this.getListEnum() ; e.hasMoreElements() ;) {
	    ret.append(e.nextElement());
	}
	return ret.toString();
    }

    //returns a copy of this
    private  OpenIfList copy() {
	if (list != null)
	    return new OpenIfList((Vector)list.clone());
	else 
	    return new OpenIfList(null);
    }
}

