package jx.verifier.wcet;

import jx.verifier.bytecode.*;

public class CFGBBEdge extends CFGEdge {
    private BasicBlock bb;
    public int type; //normal, if or exception type - see CFGNode
    public int eHandler = 0; //address of exception handler to which this edge points

    public CFGBBEdge(CFGNode source, CFGNode target, BasicBlock bb, int type) {
	super(source, target);
	this.bb = bb;
	source.registerOEdge(this);
	target.registerIEdge(this);
	this.type = type;
	if (bb != null)
	    bb.parents.addParentEdge(this);
    }
    
    public static CFGBBEdge createEdge(CFGNode source, ByteCode actBC, 
				       CFGNodeList nodes, int type, BasicBlockList bBlocks) {
	BasicBlock basicBlock = bBlocks.getBBlock(actBC);
	ByteCode[] targets = (basicBlock.getEnd() != null)? 
	    basicBlock.getEnd().getTargets():
	    actBC.getTargets();
	if(targets.length == 0) {
	    //return at end of bb.
	    return new CFGBBEdge(source, nodes.getRetNode(), basicBlock, type);
	} else {
	    if (targets.length <= 1) {
		//end of basic block because target of a branch ...
		return new CFGBBEdge(source, nodes.addNode(targets[0]), 
				     basicBlock, type);
	    } else {
		//...or next bc is an if.
		return new CFGBBEdge(source, nodes.addNode((basicBlock.getEnd() != null)? 
				     basicBlock.getEnd(): actBC), basicBlock, type);
	    }
	}
	    
    }

    public String toString() {
	return "BBE: S" + Integer.toHexString(getSource().getAddress()) +
	    ", T" + Integer.toHexString(getTarget().getAddress()) + 
	    ", BB" + bb + " ";
	
    }
    //should be called, if the basicblock is only executed if the Condition of the if at
    //address ifAddr is "ifCase"
    public void setIfCase(OpenIfListElement openIf) {
	bb.setIfCase(openIf);
	//the source node of the edge may not be in the BB, if they are ifs.
	//--> notify the source as well! (target musn't be notified, as it might already be
	//outside the if)
	//((WCETState)getSource().getBCode().beforeState).getOIfList().add(openIf);
    }
    //simulate the effect of all BBs in this edge on the state and return resulting state
    //NOTE: 'state' may change (i.e. no copy of the parameter is made before simulation)!;
    // the returned state can be but is not necessarily the same object as the state-par.;
    // the nextBC of SimState is ignored, and execution is startet at the first bytecode
    //of the first BB of this edge.
    public SimState simulate(SimState state) {
	if (state.timeExceeded)
	    return state;
	if (bb != null)
	    return bb.simulate(state);
	else 
	    return state;
    }

    //call if this edge contains an bytecode that is necessary for the simulation of the if
    //at address ifAddr;
    public void markWithIf(int ifAddr){
	//FEHLER debug
	//System.out.println(this + "MARKED!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
	//if not marked already, propagate to parents!
    	if (marks.addElement(ifAddr)) {
	    parents.markWithIf(ifAddr);
	}
    }
    public boolean contains(int address) {
	if (bb!= null)
	    return bb.contains(address);
	else return false;
    }

    //returns first bytecode contained in this edge
    public ByteCode getFirst() {
	if (bb!=null)
	    return bb.getBegin();
	else 
	    return getTarget().getBCode();
    }

}
