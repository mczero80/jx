package jx.verifier.wcet;

import jx.verifier.bytecode.*;

//contains pointers to begin and end of basic blocks, where all bytecodes between
//begin and end are neither targets of a contitional branch (i.e. more than one source)
//nor conditional branches(i.e. more than one target). However, they may contain
//unconditional jumps (gotos).
//note that a BB can be empty (begin and end == null)
public class BasicBlock {
    private ByteCode begin = null;
    private ByteCode end = null;
    private boolean containsGoto = false; //set to true if there is a goto within this bb

    public CFGParentList parents = new CFGParentList();
    private SimMarkList marks = new SimMarkList();
    
    public ByteCode getBegin() {return begin;}
    public ByteCode getEnd() {return end;}

    //all bytecodes that have one source and one target, beginning at 'start' will be
    //contained in the new basic block. 
    //FALSCH: However, if start itself has more than one target
    //the result will be an empty basic block 
    //RICHTIG: note that start may have more than one source, and the final bytecode of the bb may have more than one target.!
    //Besides, the basic block will end, if code protected by an exception handler starts or
    //protection ends
    public BasicBlock(ByteCode start) {

	/*	if (start.getTargets().length > 1) {
	    System.out.println("0" + this.toString());
	    return;
	    }*/
	begin = start;
	end= start;
	ByteCode prev = null;
	while(true) {
	    //FEHLER eigentlich gehört auch das erst nach den exception handler hin
	    //check for return or athrow
	    if (end.getTargets().length == 0) {
		registerWithBCodes();
		return;
	    }
	    ByteCode next = end.getTargets()[0];

	    //check for change in exception handlers
	    boolean ehEnds = false; //true, if protection by an eh ends with this bc.
	    if (end.eHandlers != null) {
		if (next.eHandlers == null) ehEnds = true;
		else if (end.eHandlers.length < next.eHandlers.length) ehEnds =true;
		else for (int i = 0; i < end.eHandlers.length; i++) {
		    if (!end.eHandlers[i].equals(next.eHandlers[i])) ehEnds = true;
		}
		if (ehEnds) {
		    //if protection by an exception handler ends here, set end of 
		    //basic block to the previous bytecode, so it ends just before
		    //the exception handler
		    end = prev;
		    if (end == null) begin = null;
		    registerWithBCodes();
		    return;
		}
		//check if a new eh starts protection at 'next'
		if (next.eHandlers.length > end.eHandlers.length) {
		    registerWithBCodes();
		    return;
		}
		
	    } else {
		//end.eHandlers == null!
		if (end.eHandlers != next.eHandlers) {
		    registerWithBCodes();
		    return;
		}
	    }
	    
	    //check for ifs or targets of branches
	    if (next.getTargets().length > 1 || next.getSources().length > 1 ||
		next.startsEH != null ) {
		//ifs still belong to this basic block
		if (next.getTargets().length > 1) {
		    end = next;
		}
		registerWithBCodes();
		return;
	    } 
	    //FEHLER
	    //evtl. auch bei jsr o.ä.?
	    if (end.getOpCode() == ByteCode.GOTO ||
		end.getOpCode() == ByteCode.GOTO_W)
		containsGoto = true;
	    prev = end;
	    end = next;
	}
    }
    
    //Create empty basic block
    public BasicBlock() {
    }

    //set state.bBlock of all bytecodes contained in basicBlock to 'this'
    private void registerWithBCodes() {
	if (begin == null) return;
	for (ByteCode act = begin; act != end; act = act.getTargets()[0]) {
	    ((WCETState)act.beforeState).bBlock = this;
	}
	((WCETState)end.beforeState).bBlock = this;

    }


    //should be called, if the basicblock is only executed if the Condition of the if at
    //address ifAddr is "ifBranch"
    public void setIfCase(OpenIfListElement openIf) {
	ByteCode act = begin;
	for (; act != end; act = act.getTargets()[0]) {
	    //((WCETState)act.beforeState).getOIfList().add(openIf);
	}
	//set if for "end"
	if (act != null) {
	    //((WCETState)act.beforeState).getOIfList().add(openIf);
	}
    }
    //simulate the effect of all Bcs in this BB on the state and return resulting state
    //NOTE: 'state' may change (i.e. no copy of the parameter is made before simulation)!;
    // the returned state can be but is not necessarily the same object as the state-par.;
    // the nextBC of SimState is ignored, and execution is startet at the first bytecode
    // this BB
    // if this bytecode is the beginning of an Exception handler, the stack is cleared and
    // an exception-object is pushed onto it.
    public SimState simulate(SimState state) {
	//FEHLER debug
	//System.out.println("Simulating " + this);
	if (state.timeExceeded)
	    return state;
	if (begin == null) return state;
	state.nextBC = begin;
	if (state.nextBC.startsEH != null) {
	    //this bytecode is the beginning of an Exception handler, so the stack is cleared and
	    // an exception-object is pushed onto it.
	    state.clearStack();
	    state.push(new SimData(-state.nextBC.getAddress()));
	}

	boolean endLoop = false;
	while(true) { //loop through the bb and execute all bytecodes
	    
	    //ifs are not executed within the BB, but in the correspoinding CFGR2a/bedge
	    if (state.nextBC.getTargets().length > 1) {
		if (state.nextBC == end) //everything ok, return
		    return state;
		else //an if within an Basic-Block?!
		    throw new Error("Internal Error: If found within Basic-Block!");
	    }
	    //simulate BC normally

	    //FEHLER debug
	    //System.out.print("Executing "+ state.nextBC + " - ");

	    //finished simulating state.nextBC, proceed to next bytecode
	    if (state.nextBC == end) //the last bytecode has been simulated
		endLoop = true;

	    //simulate this bytecode
	    if (((WCETState)state.nextBC.beforeState).necessary) {
		state.executeBC();
		if (state.informationMissing())
		    return state;
		
	    } else {
		state.simulateBC();
		if (state.informationMissing())
		    return state;
	    }

	    if (state.nextBC == null || endLoop)
		return state;
	} //while(true) { //loop through the bb and execute all bytecodes
    }

    public void markWithIf(int ifAddr, ByteCode markedBC) {
	//FEHLER debug
	//System.out.println(this + "MARKED by "+markedBC+"!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
	//FEHLER warum müssen die ifs überhaupt noch teil des BBs sein?
	//if the last bytecode in the bb is an if and is the one that marked the bb, dont do anythin
	if (markedBC == end &&
	    markedBC.getTargets().length > 1) {
	    //nothing 
	} else if (marks.addElement(ifAddr)) {
	    parents.markWithIf(ifAddr);
	}
    }

    public boolean contains(int address) {
	if (begin == null) return false;
	if (containsGoto) {
	    //search through all bytecodes
	    for (ByteCode act = begin; act != end; act = act.getTargets()[0]) {
		if (act.getAddress() == address)
		    return true;
	    }
	    if (end.getAddress() == address) 
		return true;
	    
	    return false;
	} else {
	    //just check if address between begin and end
	    return (address >= begin.getAddress() &&
		    address <= end.getAddress());
	}
    }

    public String toString() {
	return "BB:" + ((begin == null || end == null)? "(empty)" :
			Integer.toHexString(begin.getAddress()) + 
			" - " + Integer.toHexString(end.getAddress()));
    }

}
