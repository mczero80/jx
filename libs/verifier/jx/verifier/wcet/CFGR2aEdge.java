package jx.verifier.wcet;

import jx.verifier.bytecode.*;

//Composite edge, result of an application of rule 2a (two edges with same source and target can be unified)
public class CFGR2aEdge extends CFGEdge {
    private CFGEdge edge1; 
    private CFGEdge edge2; 

    //changing of iEdges and oEdges in target and source must be done "by Hand"!!!
    public CFGR2aEdge(CFGEdge e1, CFGEdge e2) {
	super(e1.getSource(), e1.getTarget());
	edge1 = e1;
	edge2 = e2;
	if (edge1.getSource() != edge2.getSource() ||
	    edge1.getTarget() != edge2.getTarget()) {
	    throw new Error("Internal Error: different source or target!");
	}
	e1.parents.addParentEdge(this);
	e2.parents.addParentEdge(this);
    }
    
    public String toString() {
	return "CER2a: S" + Integer.toHexString(getSource().getAddress()) +
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
    //of the first BB of the edge.
    public SimState simulate(SimState state)  {
	if (state.timeExceeded)
	    return state;
	state.nextBC = this.getFirst();
	if (edge1.getSource() instanceof CFGR3Node ||
	    (state.nextBC.getTargets().length > 1 &&
	    ((WCETState)state.nextBC.beforeState).necessary &&
	    edge1.getSource().type != CFGNode.EXCEPTION)) {

	    if (edge1.getSource() instanceof CFGR3Node) {
		state = ((CFGR3Node)edge1.getSource()).simulate(state);
	    } else {
		//execute the if, to find out which branch should be taken
		state.executeBC();
	    }
	    if (state.informationMissing()||state.timeExceeded)
		return state;

	    if (state.nextBC == edge1.getFirst()) {
		return edge1.simulate(state);
	    } else if (state.nextBC == edge2.getFirst()) {
		return edge2.simulate(state);
	    } else {
		throw new Error("Internal Error: target of branch not found!");
	    }

	} else {
	    //if should not be executed, so simulate both branches and merge the results

	    //pop operands from stack
	    for (int i = 0; i< BCEffectPass0.POP[state.nextBC.getOpCode()]; i++) {
		state.pop();
	    }
	    SimState tmpState = edge1.simulate(state.copy());
	    if (tmpState.informationMissing()||state.timeExceeded)
		return tmpState;
	    return tmpState.merge(edge2.simulate(state));
	}
    }

    //call if this edge contains an bytecode that is necessary for the simulation of the if
    //at address ifAddr;
    public void markWithIf(int ifAddr){
	//FEHLER debug
	//System.out.println(this + "MARKED!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
    	if (marks.addElement(ifAddr)) {
	    if (this.contains(ifAddr)){
		//do nothing, no need to propagate if
	    } else {
		edge1.getSource().markIf();
		parents.markWithIf(ifAddr);
	    }
	}
    }
    public boolean contains(int address) {
	return edge1.contains(address) || edge2.contains(address);
    }

    //returns first bytecode contained in this edge
    public ByteCode getFirst() {
	return edge1.getSource().getBCode();
    }

}
