package jx.verifier.typecheck;

import java.util.Vector;
import jx.verifier.*;
import jx.verifier.bytecode.*;

public class TCState extends JVMState {
    public OPStack TCgetStack() {return (OPStack) super.getStack();}
    public TCLocalVarsInterface TCgetlVars() { return (TCLocalVarsInterface) super.getlVars();}

    //Simulate execution of nextBc on this stack.
    //throws exception if verification fails.
    //returns the resulting states, with nextBC correctly set to the next target
    //if result is null, all in nextBC.getTargets() will be verified with this as beforeState
    protected JVMState[] doExecuteNextBC() throws VerifyException { 
	// Transform State...
	BCStackEffect.simulateBC(this, getNextBC());
	return null;
	
    }

    //transform state as if an Exception of type eName had been thrown,
    //i.e. clear stack, push an object of type eName onto stack and change getNextBC().
    public void exceptionThrown(String eName, ByteCode handler) throws VerifyException{
	setNextBC(handler);
	setStack(new OPStack(getStack().getMaxSize()));
	((OPStack)getStack()).push(new TCObjectTypes(eName), JVMOPStackElement.ADDR_EXCEPTION);
    }

    //copy this state.
    //should return something created with
    //new JVMState(nextBC, stack.copy(), lVars.copy(), mv);
    public JVMState copy() {
	return new TCState(getNextBC(), getStack().copy(), getlVars().copy(), getMv());
    }

    public void srCalled(ByteCode srBegin) throws VerifyException {
	super.srCalled(srBegin);
	//Return Address must be added to stack. The RA Object contains the beginAddress of 
	// the subroutine for identification
	TCRAType returnAddress = new TCRAType(srBegin.getAddress());
	TCgetStack().push(returnAddress, srBegin.getAddress());
    }
    public TCState(ByteCode nextBC, 
		   int numVars, 
		   int maxStackSize,
		   MethodVerifier mv) 
	throws VerifyException {
	super(nextBC,
	      new OPStack(maxStackSize),
	      new TCLocalVars(mv.getClassName(), 
					     mv.getTypeDesc(), 
					     mv.isStaticMethod(),
					     numVars),
	      (VerifierInterface) mv);
    }

    //constructor for copy
    private TCState(ByteCode nextBC, JVMOPStack stack, JVMLocalVars lVars, 
		     VerifierInterface mv) {
	super(nextBC, stack, lVars, mv);
    }

}

