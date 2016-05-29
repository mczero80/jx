package jx.verifier.npa;

import java.util.Vector;
import jx.verifier.*;
import jx.verifier.bytecode.*;

public class NPAState extends JVMState {
    public NPAStack NPAgetStack() { return (NPAStack) getStack();}
    public NPALocalVarsInterface NPAgetLVars() { return (NPALocalVarsInterface) getlVars();}
    public void srCalled(ByteCode srBegin) throws VerifyException {
	super.srCalled(srBegin);
	NPAgetStack().push(NPAValue.newOTHER(), srBegin.getAddress()); //return value
    }

    //Simulate execution of nextBc on this stack.
    //should return all bytecodes which might be executed after this one
    //throws exception if verification fails.
    protected JVMState[] doExecuteNextBC() throws VerifyException {
	return NPABCEffect.simulateBC(this, getNextBC());
    }
    //copy this state.
    //should return something created with
    //new JVMState(nextBC, stack.copy(), lVars.copy(), mv);
    public JVMState copy() {
	return new NPAState(getNextBC(), getStack().copy(), getlVars().copy(),getMv());
    }
    //transform state as if an Exception of type eName had been thrown,
    //i.e. usually clear stack, push an object of type eName onto stack and change nextBC.
    public void exceptionThrown(String eName, ByteCode handler) 
	throws VerifyException {
	setNextBC(handler);
	setStack(new NPAStack(getStack().getMaxSize()));
	((NPAStack)getStack()).push(NPAValue.newNONNULL(), JVMOPStackElement.ADDR_EXCEPTION);

    }

    //Constructor for copy
    protected NPAState(ByteCode nextBC, JVMOPStack stack, JVMLocalVars lVars, 
		       VerifierInterface mv
		       ) {
	super(nextBC, stack, lVars, mv);
    }
    public NPAState(ByteCode nextBC, 
		    int numVars, 
		    int maxStackSize,
		    MethodVerifier mv) 
	throws VerifyException {
	super(nextBC,
	      new NPAStack(maxStackSize),
	      new NPALocalVars(mv.getClassName(), 
			       mv.getTypeDesc(), 
			       mv.isStaticMethod(),
			       numVars),
	      (VerifierInterface) mv);
    }
    
}

