package jx.verifier.wcet;

import jx.verifier.bytecode.*;
import java.util.Vector;
import java.util.Enumeration;

public class CFGNode {
    //Types
    public static final int NORMAL = 0;
    public static final int IF = 1;
    public static final int EXCEPTION = -1;

    public int type = 0;

    public OpenIfListElement[] openIf = new OpenIfListElement[2];

    private ByteCode bCode;
    protected Vector oEdges = new Vector(1); //Outgoing edges
    protected Vector iEdges = new Vector(1); //Incoming edges
    
    public ByteCode getBCode() {return bCode;}
    //address should never change after object creation! Else CFGNodeList searching does not work!
    public CFGNode(ByteCode bCode) {
	this.bCode = bCode;
    }

    public int getAddress() {
	if (bCode == null) return -1;
	return bCode.getAddress();
    }

    public String toString() {
	String ret = Integer.toHexString(bCode.getAddress()) + " oEdges: ";
	for (Enumeration e = oEdges.elements(); e.hasMoreElements();) {
	    ret += e.nextElement().toString()
		+ "; ";
	}
	return ret;
    }

    public void registerOEdge(CFGEdge newEdge) {
	oEdges.addElement(newEdge);
    }
    public void registerIEdge(CFGEdge newEdge) {
	iEdges.addElement(newEdge);
    }

    //replace  outgoing edge with new edge
    public void replaceOEdge(CFGEdge oldEdge, CFGEdge newEdge) {
	oEdges.removeElement(oldEdge);
	oEdges.addElement(newEdge);
    }
    //replace outgoing edges with new edge
    public void replaceOEdges(CFGEdge oldEdge1, CFGEdge oldEdge2, CFGEdge newEdge) {
	oEdges.removeElement(oldEdge1);
	oEdges.removeElement(oldEdge2);
	oEdges.addElement(newEdge);
    }
    //replace oldEdge with newEdge; oldEdge MUST be in inEdges
    public void replaceIEdge(CFGEdge oldEdge, CFGEdge newEdge) {
	iEdges.removeElement(oldEdge);
	iEdges.addElement(newEdge);
    }
    //replace oldEdge with newEdge; oldEdge MUST be in inEdges
    public void replaceIEdges(CFGEdge oldEdge1, CFGEdge oldEdge2, CFGEdge newEdge) {
	iEdges.removeElement(oldEdge1);
	iEdges.removeElement(oldEdge2);
	iEdges.addElement(newEdge);
    }
    
    //mark the if represented by this node for evaluation
    public void markIf() {
	//FEHLER debug
	//System.out.println(this + "MARKED!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
	if (bCode.getTargets().length != 2) {
	    throw new Error("Internal Error: markIf called on node that is not an if!\n\t"+this);
	}
	((WCETState)bCode.beforeState).necessary = true;
	//FEHLER debug
	//System.out.println(bCode.toString() + " MARKED!");
    }
}
