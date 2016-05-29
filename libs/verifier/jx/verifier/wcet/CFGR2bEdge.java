package jx.verifier.wcet;

import jx.verifier.bytecode.*;

//Composite edge, result of an application of rule 2b (loop elimination)
public class CFGR2bEdge extends CFGEdge {
    private CFGEdge loopEdge; //the edge that points back to the node.
    private CFGEdge exitEdge; //the edge that leaves the loop.

    //changing of iEdges and oEdges in target and source must be done "by Hand"!!!
    public CFGR2bEdge(CFGEdge loop, CFGEdge exit) {
	super(exit.getSource(), exit.getTarget());
	loopEdge = loop;
	exitEdge = exit;
	if (exit.getSource() != loop.getSource() ||
	    loop.getSource() != loop.getTarget()){
	    throw new Error("Internal Error: different source or target!");
	}
	loop.parents.addParentEdge(this);
	exit.parents.addParentEdge(this);
	if (loop.getSource().type == CFGNode.EXCEPTION) {
	    throw new Error("Internal Error: cannot loop over an Exception edge");
	}
    }
    
    public String toString() {
	return "CER2b: S" + Integer.toHexString(getSource().getAddress()) +
	    ", T" + Integer.toHexString(getTarget().getAddress()) + " ";
	
    }
    //should be called, if the basicblock is only executed if the Condition of the if at
    //address ifAddr is "ifCase"
    public void setIfCase(OpenIfListElement openIf) {
	//FEHLER
    }
    //simulate the effect of all BBs in this edge on the state and return resulting state
    //NOTE: 'state' may change (i.e. no copy of the parameter is made before simulation)!;
    // the returned state can be but is not necessarily the same object as the state-par.;
    // the nextBC of SimState is ignored, and execution is startet at the first bytecode
    //of the first BB of this edge.
    public SimState simulate(SimState state)  {
	if (state.timeExceeded)
	    return state;
	state.nextBC = this.getFirst();

	if (loopEdge.getSource() instanceof CFGR3Node) {
	    state = ((CFGR3Node)loopEdge.getSource()).simulate(state);
	} else {
	    //execute the if, to find out which branch should be taken
	    state.executeBC();
	}
	while(state.nextBC == loopEdge.getFirst()) {
	    if (state.informationMissing()||state.timeExceeded)
		return state;
	    state = loopEdge.simulate(state);
	    if (state.informationMissing()||state.timeExceeded)
		return state;
	    state.nextBC = this.getFirst();
	    if (loopEdge.getSource() instanceof CFGR3Node) {
		state = ((CFGR3Node)loopEdge.getSource()).simulate(state);
	    } else {
		//execute the if, to find out which branch should be taken
		state.executeBC();
	    }
	    if (state.informationMissing()||state.timeExceeded)
		return state;
	}
	if (state.informationMissing()||state.timeExceeded)
	    return state;
	
	//FEHLER debug
	//System.out.println("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
	//System.out.println("loopEdge: " + loopEdge + "\nexitEdge" + exitEdge);
	//  System.out.println("state.nextBC: " + state.nextBC);
	//  System.out.println("exitEdge.getFirst(): " + exitEdge.getFirst());
	if (state.nextBC == exitEdge.getFirst()) {
	    //FEHLER debug
	    //System.out.println("&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&&");
	    return exitEdge.simulate(state);
	} else {
	    throw new Error("Internal Error: target of branch not found!\n\tedge: " + this);
	}
    }
    
    //call if this edge contains an bytecode that is necessary for the simulation of the if
    //at address ifAddr;
    public void markWithIf(int ifAddr){
	//FEHLER debug
	//System.out.println(this + "MARKED!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
    	if (marks.addElement(ifAddr)) {
	    if (!(this.contains(ifAddr)))
		parents.markWithIf(ifAddr);
	}
    }

    public boolean contains(int address) {
	return loopEdge.contains(address) || exitEdge.contains(address);
    }

    //returns first bytecode contained in this edge
    public ByteCode getFirst() {
	return loopEdge.getSource().getBCode();
    }

}
