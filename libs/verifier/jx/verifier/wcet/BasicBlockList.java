package jx.verifier.wcet;

import jx.verifier.bytecode.*;
import java.util.Vector;
import java.util.Enumeration;

public class BasicBlockList {
    private Vector list = new Vector();
    
    //get BB that starts at bytecode "start"
    public BasicBlock getBBlock(ByteCode start) {
	for (Enumeration e = list.elements(); e.hasMoreElements();) {
	    BasicBlock act = (BasicBlock) e.nextElement();
	    if (act.getBegin() == start) return act;
	}
	return new BasicBlock(start);
    }
}
