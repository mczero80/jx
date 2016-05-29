package jx.verifier.bytecode;

import java.util.Vector;
import jx.verifier.JVMState;

/** Eintrag in der BC Liste */
public class BCLinkListEntry {
    public boolean isTarget;
    public BCLinkListEntry prev;
    public BCLinkListEntry next;
    private ByteCode BC;
    public ByteCode getBC() {return BC;}
    public JVMState beforeState;
    public int mvCheckCount; //counts how often this is in checkQueue of a methodverifier
    public int svCheckCount; //how often in checkQueue of SubroutineVerifier

    public BCLinkListEntry(BCLinkListEntry prev, ByteCode BC){
	if (BC == null) {
	    throw new Error("Internal Error: BCLinkListEntry created with (null) ByteCode!");
	}
	this.BC = BC;
	if (prev != null) {
	    this.prev = prev;
	    next = prev.next;
	    prev.next = this;
	    if (next != null) {
		next.prev = this;
	    }
	}
	mvCheckCount = 0;
	svCheckCount = 0;
	isTarget=false;
    }
    
    public BCLinkListEntry(ByteCode BC, BCLinkListEntry next){
	if (BC == null) {
	    throw new Error("Internal Error: BCLinkListEntry created with (null) ByteCode!");
	}
	this.BC = BC;
	if (next != null) {
	    this.next = next;
	    prev = next.prev;
	    next.prev = this;
	    if (prev != null) {
		prev.next = this;
	    }
	}
	mvCheckCount = 0;
	svCheckCount = 0;
	isTarget = false;
    }
}

