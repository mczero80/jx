package jx.verifier.wcet;

import jx.verifier.bytecode.*;

abstract public class CFGEdge {
    private CFGNode source;
    private CFGNode target;

    protected SimMarkList marks = new SimMarkList();

    public CFGParentList parents = new CFGParentList();

    public CFGNode getSource() {return source;}
    public CFGNode getTarget() {return target;}
    public void setTarget(CFGNode newTarget) {target = newTarget;}
    public void setSource(CFGNode newSource) {source = newSource;}
    
    public CFGEdge(CFGNode source, CFGNode target) {
	this.source = source;
	this.target = target;
    }
    //should be called, if the basicblock is only executed if the Condition of the if at
    //address ifAddr is "ifCase"
    abstract public void setIfCase(OpenIfListElement openIf);

    //simulate the effect of all BBs in this edge on the state and return resulting state
    //NOTE: 'state' may change (i.e. no copy of the parameter is made before simulation)!;
    // the returned state can be but is not necessarily the same object as the state-par.;
    // the nextBC of SimState is ignored, and execution is startet at the first bytecode
    //of the first BB of this edge.
    abstract public SimState simulate(SimState state) ;

    //call if this edge contains an bytecode that is necessary for the simulation of the if
    //at address ifAddr;
    abstract public void markWithIf(int ifAddr);

    //returns true, if address is contained within this edge
    abstract public boolean contains(int address);
    //returns first bytecode contained in this edge
    abstract public ByteCode getFirst();
}
