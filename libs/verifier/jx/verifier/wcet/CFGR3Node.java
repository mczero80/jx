package jx.verifier.wcet;

import jx.verifier.bytecode.*;
import java.util.Vector;
import java.util.Enumeration;

//nodes that are the result of an application of rule 3. 
public class CFGR3Node extends CFGNode {
    public CFGEdge bi1;
    public CFGEdge ti2;
    public CFGEdge bi2;
    public CFGEdge exit;
    public CFGR3Node(ByteCode bCode, CFGEdge bi1, CFGEdge ti2, CFGEdge bi2, CFGEdge exit) {
	super(bCode);
	this.bi1 = bi1;
	this.bi2 = bi2;
	this.ti2 = ti2;
	this.exit = exit;
    }
    //simulate the effect of all BBs in this node on the state and return resulting state
    //NOTE: 'state' may change (i.e. no copy of the parameter is made before simulation)!;
    // the returned state can be but is not necessarily the same object as the state-par.;
    // the nextBC of SimState is ignored, and execution is startet at the first bytecode
    //of the first BB of this edge.
    public SimState simulate(SimState state)   {
	if (state.timeExceeded)
	    return state;
	state.nextBC = bi1.getSource().getBCode();
	state.executeBC();

	if (state.timeExceeded)
	    return state;

	if (state.nextBC == bi1.getFirst()) {
	    return bi1.simulate(state);
	} else if (state.nextBC == ti2.getFirst()) {
	    state = ti2.simulate(state);
	    if (state.informationMissing()||state.timeExceeded)
		return state;
	} else {
	    throw new Error("Internal Error: target of branch not found!");
	}
	state.executeBC();
	if (state.timeExceeded)
	    return state;

	if (state.nextBC == bi2.getFirst()) {
	    return bi2.simulate(state);
	} else {
	    return exit.simulate(state);
	} 
    }

    //mark the if represented by this node for evaluation
    public void markIf() {
	//do nothing, all nodes are marked allready
    }


}
