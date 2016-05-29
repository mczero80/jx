package jx.verifier.wcet;

import jx.verifier.bytecode.*;
import jx.verifier.*;
import java.util.Vector;
import java.util.Enumeration;

//the same as OpenIfelement, only that every instance of an openException has an id
//if branches to an exception are inserted into the graph at two different nodes, the ids
//should also be different.
public class OpenException extends OpenIfListElement {
    private static int nextUniqueId = Integer.MIN_VALUE;

    private int id = 0;

    public int getId() {return id;}
    public OpenException(int bcAddr, int ifBranch, int id) {
	super(bcAddr, ifBranch);
	this.id = id;
    }

    public OpenException(int bcAddr, int ifBranch) {
	this(bcAddr, ifBranch, getUniqueId());
    }
    
    private static int getUniqueId() {
	return nextUniqueId++;
    }

    public boolean sameIf(OpenIfListElement other) {
	if (!(other instanceof OpenException)) return false;
	else return (other.bcAddr == this.bcAddr &&
		     ((OpenException)other).getId() == this.getId());
    }
    public String toString() {
	return (String)("<Exception@" + ((bcAddr >= 0)? Integer.toHexString(bcAddr):
				  "-"+Integer.toHexString(-bcAddr))+"=" + ifBranch +"/"+
			branchCount+", id = " + id +">");
    }
}
