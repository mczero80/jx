package jx.verifier.wcet;

import jx.verifier.bytecode.*;

//Composite edge, result of an application of rule 1 (a->b->c => a->c)
public class CFGR1Edge extends CFGEdge {
    private CFGEdge inEdge; //a->b
    private CFGEdge outEdge; //b->c
    private CFGNode node; //b

    //changing of iEdges and oEdges in target and source must be done "by Hand"!!!
    public CFGR1Edge(CFGEdge e1, CFGEdge e2) {
	super(e1.getSource(), e2.getTarget());
	inEdge = e1;
	outEdge = e2;
	node = inEdge.getTarget();
	if (outEdge.getSource() != node) {
	    throw new Error("Internal Error: in.target != out.source!!!");
	}
	e1.parents.addParentEdge(this);
	e2.parents.addParentEdge(this);
    }
    
    public String toString() {
	return "CER1: S" + Integer.toHexString(getSource().getAddress()) +
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
    public SimState simulate(SimState state) {
	if (state.timeExceeded) 
	    return state;
	state = inEdge.simulate(state);
	if (state.informationMissing() || state.timeExceeded)
	    return state;
	return outEdge.simulate(state);
    }

    //call if this edge contains an bytecode that is necessary for the simulation of the if
    //at address ifAddr;
    public void markWithIf(int ifAddr){
	//FEHLER debug
	//System.out.println(this + "MARKED!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
	//if not marked already, propagate to parents!
    	if (marks.addElement(ifAddr)) {
	    if (inEdge.contains(ifAddr) ||
		outEdge.contains(ifAddr)){
		return; //do nothing, no ifs need to be marked
	    } else {
		parents.markWithIf(ifAddr);
	    }
	}
    }
    
    public boolean contains(int address) {
	return inEdge.contains(address) || outEdge.contains(address);
    }

    //returns first bytecode contained in this edge
    public ByteCode getFirst() {
	return inEdge.getFirst();
    }

}
