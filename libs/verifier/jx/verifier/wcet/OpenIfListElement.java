package jx.verifier.wcet;

import jx.verifier.bytecode.*;
import jx.verifier.*;
import java.util.Vector;
import java.util.Enumeration;


public class OpenIfListElement {
    
    int bcAddr;
    int ifBranch; //the number of the actual if-Branch. 0<=ifBranch<branchCount!
    int branchCount; //how many branches exist

    public OpenIfListElement(int bcAddr, int ifBranch) {
	this(bcAddr, ifBranch, 2); //default case: true and false branches
	//FEHLER debug
	if (bcAddr == 0) throw new Error("asdfasdf");
    }
    public OpenIfListElement(int bcAddr, int ifBranch, int branchCount) {
	if (ifBranch >= branchCount) {
	    throw new Error("Internal Error: ifBranch >= branchCount!");
	}
	if (ifBranch < 0) {
	    throw new Error("Internal Error: ifBranch < 0°");
	}
	this.bcAddr = bcAddr;
	this.ifBranch = ifBranch;
	this.branchCount = branchCount;
    }
    
    public boolean sameIf(OpenIfListElement other) {
	if (other instanceof OpenException) return false;
	else return (other.bcAddr == this.bcAddr);
    }

    public String toString() {
	return (String)("<if@" + ((bcAddr >= 0)? Integer.toHexString(bcAddr):
				  "-"+Integer.toHexString(-bcAddr))+"=" + ifBranch +"/"+
			branchCount+">");
    }
}
